# Amicus

Marketplace de servicios del hogar (electricidad, plomería, gas, pintura,
limpieza). Un usuario publica servicios con cupos disponibles y otro los
contrata mediante un carrito y un checkout que descuenta los cupos. Backend
desarrollado con Spring Boot y una API REST que expone el catálogo, la gestión
de publicaciones, el carrito y las órdenes. Trabajo Práctico Obligatorio de
Aplicaciones Interactivas, UADE, segundo cuatrimestre 2026.

---

## Stack

Java 21 · Spring Boot 3.5.3 · Spring Data JPA (Hibernate) · Lombok · Maven · MySQL 8.4 · Swagger

## Cómo ejecutarlo

Requiere tener **Docker Desktop** corriendo.

```bash
git clone https://github.com/<usuario>/back-Amicus.git
cd back-Amicus
docker compose up -d      # levanta MySQL 8.4 ya configurado
./mvnw spring-boot:run
```

La aplicación queda en `http://localhost:8080`. La base `amicus` se crea sola y
las tablas también, a partir de las entidades.

Para inspeccionar las tablas:

```bash
docker exec -it amicus-mysql mysql -u amicus -pamicus amicus
```

### Respaldo sin Docker

Si no podés levantar Docker, el proyecto corre con H2 sin instalar nada:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
```

Consola de la base en `http://localhost:8080/h2-console`
(JDBC URL `jdbc:h2:file:./data/amicus`, usuario `sa`, contraseña vacía).

## Arquitectura

```
controller/   @RestController, recibe HTTP y devuelve DTOs
service/      @Service + @Transactional, lógica de negocio
repository/   @Repository extends JpaRepository, acceso a datos
model/        @Entity, mapeo a tablas con relaciones JPA
dto/          objetos de entrada y salida, desacoplan las entidades del HTTP
exception/    excepciones propias y manejador global de errores
config/       configuración y carga de datos inicial
```

## Endpoints

| Método | Ruta | Qué hace |
|---|---|---|
| POST | `/api/auth/registro` | registra un usuario y le crea el carrito |
| POST | `/api/auth/login` | valida mail y contraseña |
| GET | `/api/categorias` | listado de categorías para la home |
| GET | `/api/zonas` | listado de barrios y partidos |
| GET | `/api/servicios` | catálogo alfabético. Filtros: `?categoriaId=` `?zonaId=` `?q=` `?conCupo=true` |
| GET | `/api/servicios/{id}` | detalle con imágenes, categoría, profesional y zonas |
| POST | `/api/servicios` | publica un servicio |
| PUT | `/api/servicios/{id}?usuarioId=` | modifica una publicación. 403 si no sos el dueño |
| PATCH | `/api/servicios/{id}/cupos?usuarioId=` | ajusta los cupos. 403 si no sos el dueño |
| DELETE | `/api/servicios/{id}?usuarioId=` | baja lógica. 403 si no sos el dueño |
| POST | `/api/servicios/{id}/imagenes?usuarioId=` | agrega una foto |
| DELETE | `/api/servicios/{id}/imagenes/{imagenId}?usuarioId=` | quita una foto |
| GET | `/api/carrito?usuarioId=` | contenido con el total calculado |
| POST | `/api/carrito/items?usuarioId=` | agrega un servicio con sus visitas y frecuencia. 409 si no tiene cupos |
| PUT | `/api/carrito/items/{id}?usuarioId=` | cambia la cantidad de visitas y la frecuencia |
| DELETE | `/api/carrito/items/{id}?usuarioId=` | elimina un ítem |
| DELETE | `/api/carrito?usuarioId=` | vacía el carrito |
| POST | `/api/carrito/checkout?usuarioId=` | confirma, descuenta cupos y genera la orden |
| GET | `/api/ordenes?usuarioId=` | historial de compras |
| GET | `/api/ordenes/{id}` | detalle de una orden |
| PATCH | `/api/ordenes/{id}/cancelar?usuarioId=` | cancela y devuelve los cupos. 403 si no sos el comprador |
| GET | `/api/servicios/{id}/resenas` | reseñas del servicio, de la más nueva a la más vieja |
| POST | `/api/servicios/{id}/resenas?usuarioId=` | reseña. 403 si no lo contrataste, 409 si ya lo reseñaste |
| DELETE | `/api/resenas/{id}?usuarioId=` | borra tu reseña. 403 si no sos el autor |

## Contratación recurrente

Un servicio no se compra por unidad, se contrata por visita. Cada línea del
carrito lleva la cantidad de visitas y una frecuencia: `UNICA` (por defecto),
`SEMANAL`, `QUINCENAL` o `MENSUAL`. Ocho visitas semanales son dos meses de
limpieza y consumen ocho cupos, porque un cupo es una visita que el profesional
se compromete a tomar. La frecuencia no cambia el precio, solo cuándo se presta,
y la orden la congela junto al título y al precio.

## Reseñas

Solo puede reseñar quien tenga una orden `CONFIRMADA` que incluya el servicio, y
una sola vez. Nadie reseña su propia publicación. El detalle del servicio expone
`promedioPuntaje` y `cantidadResenas`; el promedio es `null` y no `0` cuando
todavía no hay reseñas, porque un servicio nuevo no vale cero estrellas.






