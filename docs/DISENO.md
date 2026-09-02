# Amicus - Diseño del Backend

**Materia:** Aplicaciones Interactivas (UADE) - 2C 2026
**Entrega:** 1ra Actividad Obligatoria (solo backend)
**Repositorio:** `back-Amicus`

Amicus es un marketplace de servicios del hogar (electricidad, plomería, gas,
pintura, limpieza). Un usuario publica servicios con cupos disponibles y otro
usuario los contrata a través de un carrito y un checkout.

Referencia de negocio: TaskRabbit / Timbrit. Sin inteligencia artificial.

---

## 1. Decisiones de diseño

### 1.1 El cupo hace de stock

La consigna está escrita para productos con stock. Un servicio no tiene stock,
tiene disponibilidad. La traducción que adoptamos: cada publicación declara
`cuposDisponibles`, que es la cantidad de veces que el profesional puede tomar
ese trabajo. Al hacer checkout se descuenta un cupo, exactamente como se
descontaría stock.

Esto cumple la consigna literal sin romper la metáfora del negocio.

### 1.2 La orden congela el precio

`OrdenItem` guarda `precioUnitario` y `tituloServicio` como copia, no como
referencia. Si el profesional sube el precio después, las órdenes viejas siguen
mostrando lo que el cliente realmente pagó.

### 1.3 Baja lógica, no física

`Servicio.activo = false` en vez de borrar la fila. Un servicio borrado que
aparece en órdenes históricas no puede desaparecer de la base sin romperlas.

### 1.4 Zona resuelve el ManyToMany

La consigna pide explícitamente `@ManyToMany`. Las zonas de cobertura lo
justifican de verdad: un electricista trabaja en varios barrios y en cada barrio
trabajan varios profesionales.

### 1.5 La recurrencia reusa la cantidad

Un servicio no se compra por unidad, se contrata por visita. `CarritoItem` ya
multiplicaba el precio por `cantidad` y ya descontaba esa cantidad de cupos, asi
que contratar ocho visitas era mecánicamente idéntico a `cantidad = 8`. Lo que
faltaba no era la lógica sino el significado: `Frecuencia` (`UNICA`, `SEMANAL`,
`QUINCENAL`, `MENSUAL`) dice cada cuánto se repiten.

La decisión que había que tomar era si ocho semanas consumen ocho cupos o uno.
Consumen ocho: un cupo es una visita que el profesional se compromete a tomar, y
así no hubo que separar precio de disponibilidad ni tocar el checkout.

La frecuencia no entra en el cálculo del total. Ocho visitas cuestan lo mismo
sean semanales o mensuales; lo que cambia es cuándo se prestan.

Una línea tiene una sola frecuencia, porque la restricción única sobre
`(carrito_id, servicio_id)` impide que el mismo servicio aparezca dos veces. Si
se agrega de nuevo con otra frecuencia, la nueva redefine la línea.

### 1.6 Solo reseña quien contrató

En un marketplace de oficios la reputación es el producto: nadie deja entrar a
un desconocido a su casa sin ver antes qué opinó el resto. La regla que le da
valor al promedio es que solo pueda escribir quien tenga una orden `CONFIRMADA`
que incluya el servicio. Una orden cancelada no habilita, porque el trabajo no
se prestó.

El promedio lo calcula la base con `avg()`, no Java: traer todas las reseñas
para devolver un número sería mover cientos de filas por la red. Devuelve `null`
cuando no hay ninguna, y no se reemplaza por `0`: un servicio nuevo no vale cero
estrellas, no tiene calificación, y son dos cosas distintas.

La calificación aparece en el detalle del servicio y no en el resumen del
catálogo a propósito: es una consulta agregada por servicio, y ponerla en el
listado significaría una consulta extra por cada fila devuelta.

### 1.7 Base de datos

MySQL 8.4 como base principal, levantado con `docker compose up -d` para que los
cinco integrantes tengan la misma versión y las mismas credenciales sin instalar
nada a mano.

Se conserva un perfil `h2` de respaldo, que permite correr el proyecto sin
Docker. Sirve para el integrante que no logre levantarlo y para garantizar que
quien corrija el `.zip` siempre pueda ejecutar la aplicación.

Cambiar de un motor al otro no requiere tocar una línea de código Java, solo la
configuración. Eso demuestra que la capa de persistencia está bien desacoplada.

---

## 2. Modelo de datos

### usuarios
| Campo | Tipo | Restricciones |
|---|---|---|
| id | BIGINT | PK, autoincremental |
| username | VARCHAR(50) | NOT NULL, UNIQUE |
| email | VARCHAR(120) | NOT NULL, UNIQUE |
| password | VARCHAR(255) | NOT NULL (hash BCrypt, nunca texto plano) |
| nombre | VARCHAR(80) | NOT NULL |
| apellido | VARCHAR(80) | NOT NULL |
| fechaAlta | TIMESTAMP | NOT NULL |
| activo | BOOLEAN | default true |

Un mismo usuario puede publicar servicios y contratarlos. No hay tabla de roles.

### categorias
| Campo | Tipo | Restricciones |
|---|---|---|
| id | BIGINT | PK |
| nombre | VARCHAR(60) | NOT NULL, UNIQUE |
| descripcion | VARCHAR(200) | |

Datos iniciales: Electricidad, Plomería, Gas, Pintura, Carpintería, Limpieza,
Aire acondicionado, Cerrajería, Jardinería, Mudanzas.

### zonas
| Campo | Tipo | Restricciones |
|---|---|---|
| id | BIGINT | PK |
| nombre | VARCHAR(80) | NOT NULL, UNIQUE |

Datos iniciales: barrios de CABA y partidos del GBA.

### servicios
| Campo | Tipo | Restricciones |
|---|---|---|
| id | BIGINT | PK |
| titulo | VARCHAR(120) | NOT NULL |
| descripcion | VARCHAR(1000) | NOT NULL |
| precio | DECIMAL(12,2) | NOT NULL, mayor a 0 |
| cuposDisponibles | INT | NOT NULL, mayor o igual a 0 |
| activo | BOOLEAN | default true |
| fechaPublicacion | TIMESTAMP | NOT NULL |
| categoria_id | BIGINT | FK categorias, NOT NULL, `@ManyToOne` |
| profesional_id | BIGINT | FK usuarios, NOT NULL, `@ManyToOne` |

### servicio_imagenes
| Campo | Tipo | Restricciones |
|---|---|---|
| id | BIGINT | PK |
| url | VARCHAR(500) | NOT NULL |
| orden | INT | para saber cuál es la portada |
| servicio_id | BIGINT | FK servicios, NOT NULL, `@ManyToOne` |

Resuelve el requisito de "una o más fotos". Guardamos URLs, no archivos
binarios.

### servicio_zonas
Tabla intermedia del `@ManyToMany`. PK compuesta (servicio_id, zona_id).

### carritos
| Campo | Tipo | Restricciones |
|---|---|---|
| id | BIGINT | PK |
| usuario_id | BIGINT | FK usuarios, NOT NULL, UNIQUE, `@OneToOne` |
| fechaCreacion | TIMESTAMP | NOT NULL |

Un carrito abierto por usuario. Se crea al registrarse.

### carrito_items
| Campo | Tipo | Restricciones |
|---|---|---|
| id | BIGINT | PK |
| carrito_id | BIGINT | FK carritos, NOT NULL, `@ManyToOne` |
| servicio_id | BIGINT | FK servicios, NOT NULL, `@ManyToOne` |
| cantidad | INT | NOT NULL, mayor a 0. Son visitas |
| frecuencia | VARCHAR(20) | NOT NULL, UNICA / SEMANAL / QUINCENAL / MENSUAL |

UNIQUE (carrito_id, servicio_id): agregar dos veces el mismo servicio suma
cantidad, no crea una fila nueva.

### ordenes
| Campo | Tipo | Restricciones |
|---|---|---|
| id | BIGINT | PK |
| usuario_id | BIGINT | FK usuarios, NOT NULL, `@ManyToOne` |
| fecha | TIMESTAMP | NOT NULL |
| total | DECIMAL(12,2) | NOT NULL |
| estado | VARCHAR(20) | CONFIRMADA / CANCELADA |

### orden_items
| Campo | Tipo | Restricciones |
|---|---|---|
| id | BIGINT | PK |
| orden_id | BIGINT | FK ordenes, NOT NULL, `@ManyToOne` |
| servicio_id | BIGINT | FK servicios, NOT NULL |
| tituloServicio | VARCHAR(120) | copia congelada |
| cantidad | INT | NOT NULL. Son visitas |
| frecuencia | VARCHAR(20) | NOT NULL, copia congelada |
| precioUnitario | DECIMAL(12,2) | copia congelada |
| subtotal | DECIMAL(12,2) | cantidad por precioUnitario |

### resenas
| Campo | Tipo | Restricciones |
|---|---|---|
| id | BIGINT | PK |
| servicio_id | BIGINT | FK servicios, NOT NULL, `@ManyToOne` |
| autor_id | BIGINT | FK usuarios, NOT NULL, `@ManyToOne` |
| puntaje | INT | NOT NULL, de 1 a 5 |
| comentario | VARCHAR(1000) | opcional: se puede calificar sin escribir |
| fecha | TIMESTAMP | NOT NULL |

UNIQUE (servicio_id, autor_id): una sola reseña por persona y por servicio, para
que nadie pueda repetir su opinión y correr el promedio.

---

## 3. Reglas de negocio

1. No se puede agregar al carrito un servicio con `cuposDisponibles = 0`. La
   consigna pide justamente que el usuario vea esa situación y no pueda
   agregarlo. Respuesta: 409 Conflict.
2. No se puede pedir más cantidad que cupos disponibles. Respuesta: 400.
3. El checkout es transaccional: valida los cupos de todos los ítems, descuenta,
   crea la orden, vacía el carrito. Si algo falla, no se descuenta nada.
4. Un usuario no puede contratar su propio servicio. Respuesta: 400.
5. Borrar un servicio es baja lógica. No aparece en listados pero sobrevive en
   las órdenes.
6. Las contraseñas se guardan hasheadas con BCrypt, nunca en texto plano.
7. Solo el profesional que publicó un servicio puede modificarlo, ajustarle los
   cupos, agregarle fotos o darlo de baja. La consigna lo pide de forma
   implícita: dice que "el usuario **que crea** dicho producto podrá manejar el
   stock del mismo". Responde 403 FORBIDDEN.
8. Una frecuencia recurrente necesita al menos 2 visitas. "1 visita SEMANAL" no
   quiere decir nada: no hay nada que repetir. Respuesta: 400.
9. Solo puede reseñar un servicio quien tenga una orden `CONFIRMADA` que lo
   incluya (403), nadie puede reseñar su propia publicación (400), y se admite
   una sola reseña por persona y servicio (409). Solo el autor puede borrar la
   suya (403).

---

## 4. API REST

Base: `/api`

### Autenticación
| Método | Ruta | Descripción | Éxito |
|---|---|---|---|
| POST | `/auth/registro` | username, email, password, nombre, apellido | 201 |
| POST | `/auth/login` | email + password | 200 |

### Categorías y zonas
| Método | Ruta | Descripción | Éxito |
|---|---|---|---|
| GET | `/categorias` | listado para la home | 200 |
| GET | `/zonas` | listado de barrios | 200 |

### Servicios (catálogo y ABM)
| Método | Ruta | Descripción | Éxito |
|---|---|---|---|
| GET | `/servicios` | listado alfabético por título. Filtros: `?categoriaId=`, `?zonaId=`, `?q=`, `?conCupo=true` | 200 |
| GET | `/servicios/{id}` | detalle con imágenes, categoría, zonas y profesional | 200 / 404 |
| POST | `/servicios` | alta de publicación | 201 |
| PUT | `/servicios/{id}?usuarioId=` | modificación | 200 / 403 / 404 |
| DELETE | `/servicios/{id}?usuarioId=` | baja lógica | 204 / 403 / 404 |
| PATCH | `/servicios/{id}/cupos?usuarioId=` | el profesional ajusta sus cupos | 200 / 403 |
| POST | `/servicios/{id}/imagenes?usuarioId=` | agrega una foto | 201 / 403 |
| DELETE | `/servicios/{id}/imagenes/{imagenId}?usuarioId=` | quita una foto | 204 / 403 |

### Carrito
| Método | Ruta | Descripción | Éxito |
|---|---|---|---|
| GET | `/carrito` | contenido y total calculado | 200 |
| POST | `/carrito/items` | agrega servicio con visitas y frecuencia | 201 / 409 sin cupo |
| PUT | `/carrito/items/{id}` | cambia visitas y frecuencia | 200 |
| DELETE | `/carrito/items/{id}` | elimina un ítem | 204 |
| DELETE | `/carrito` | vacía el carrito | 204 |
| POST | `/carrito/checkout` | confirma, descuenta cupos, genera orden | 201 |

`frecuencia` es opcional en los dos primeros: al agregar se asume `UNICA`, y al
modificar significa "dejala como estaba".

### Órdenes
| Método | Ruta | Descripción | Éxito |
|---|---|---|---|
| GET | `/ordenes` | historial del usuario | 200 |
| GET | `/ordenes/{id}` | detalle de una orden | 200 / 404 |

### Reseñas
| Método | Ruta | Descripción | Éxito |
|---|---|---|---|
| GET | `/servicios/{id}/resenas` | reseñas del servicio, la más nueva primero | 200 / 404 |
| POST | `/servicios/{id}/resenas` | califica de 1 a 5 con comentario opcional | 201 / 400 / 403 / 409 |
| DELETE | `/resenas/{id}` | el autor borra la suya | 204 / 403 / 404 |

El promedio y la cantidad de reseñas se devuelven en el detalle del servicio,
como `promedioPuntaje` y `cantidadResenas`.

---

## 5. Estructura del proyecto

```
src/main/java/com/uade/amicus/
├── AmicusApplication.java
├── config/          CORS, seed de datos inicial
├── controller/      @RestController
├── service/         @Service + @Transactional
├── repository/      @Repository extends JpaRepository
├── model/           @Entity
├── dto/
│   ├── request/     lo que entra
│   └── response/    lo que sale
└── exception/       excepciones propias + @RestControllerAdvice
```

Los controllers nunca devuelven entidades, siempre DTOs. Los services nunca
reciben ni devuelven objetos HTTP.

---

## 6. Reparto entre los cinco integrantes

El profesor evalúa commits individuales (cantidad, calidad y continuidad). Cada
uno trabaja en su rama y abre Pull Request.

| # | Responsable de | Archivos principales |
|---|---|---|
| 1 | Setup, configuración, Usuario y autenticación, README | `AmicusApplication`, `application.properties`, `docker-compose.yml`, `Usuario`, `AuthController`, `GlobalExceptionHandler` |
| 2 | Categorías, Zonas y carga de datos inicial | `Categoria`, `Zona`, sus repos/services/controllers, `DataSeeder` |
| 3 | Servicio: alta, modificación, baja lógica | `Servicio`, `ServicioService`, `ServicioController`, DTOs |
| 4 | Imágenes y catálogo (listado alfabético, filtros, detalle) | `ServicioImagen`, queries de filtrado, endpoints de búsqueda |
| 5 | Carrito, checkout y órdenes | `Carrito`, `CarritoItem`, `Orden`, `OrdenItem`, `CheckoutService` |

Hay dependencias entre tareas (4 necesita 3, 5 necesita 4). Por eso el **día 1
se crean todas las entidades juntas en una sola sesión grupal**: así cada uno
arranca su capa sin esperar al otro.

---

## 7. Cronograma sugerido (vence 7/9/2026)

| Días | Qué |
|---|---|
| 27-28 ago | Repo, esqueleto Spring Boot, todas las entidades en sesión grupal |
| 29-31 ago | Cada uno su repository + service + controller |
| 1-3 sep | Carrito, checkout, integración entre módulos |
| 4-5 sep | Pruebas con Postman, seed de datos, corrección de errores |
| 6-7 sep | README, colección Postman exportada, zip y entrega en BSP |

**Atención:** el enunciado dice 07/10/2026 pero BSP dice que vence el 7 de
septiembre de 2026 y que restringe el acceso después. Confirmar con el profesor.
Mientras tanto, planificamos para el 7 de septiembre.

---

## 8. Requisitos de la consigna y dónde se cumplen

| Requisito | Dónde |
|---|---|
| Registro con username, mail, contraseña, nombre y apellido | `POST /auth/registro` |
| Login con mail y contraseña | `POST /auth/login` |
| Home con listado alfabético | `GET /servicios` ordenado por título |
| Home con listado de categorías | `GET /categorias` |
| Detalle con imagen ampliada y descripción | `GET /servicios/{id}` |
| Sin stock, no se puede agregar | 409 en `POST /carrito/items` |
| Agregar, vaciar y eliminar ítem del carrito | endpoints de `/carrito` |
| Checkout con cálculo de total | `POST /carrito/checkout` |
| Descuento de stock validado | `CheckoutService`, transaccional |
| Alta de publicación con fotos, descripción y categoría | `POST /servicios` + `/imagenes` |
| Manejo de stock por el publicador | `PATCH /servicios/{id}/cupos` |
| Eliminación de publicación | `DELETE /servicios/{id}` |
| Capa de persistencia | JPA/Hibernate sobre H2 o MySQL |
| API REST completa o filtrada | filtros por categoría, zona y texto |
