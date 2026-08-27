# Amicus

Marketplace de servicios del hogar (electricidad, plomería, gas, pintura,
limpieza). Un usuario publica servicios con cupos disponibles y otro los
contrata mediante un carrito y un checkout que descuenta los cupos. Backend
desarrollado con Spring Boot y una API REST que expone el catálogo, la gestión
de publicaciones, el carrito y las órdenes. Trabajo Práctico Obligatorio de
Aplicaciones Interactivas, UADE, segundo cuatrimestre 2026.

---

## Stack

Java 21 · Spring Boot 3.5.3 · Spring Data JPA (Hibernate) · Lombok · Maven · MySQL 8.4

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

## Documentación

- `docs/DISENO.md` — modelo de datos, endpoints, reglas de negocio y reparto de tareas
- `docs/teoria/` — un documento por archivo: qué hace, por qué existe y qué alternativas se descartaron

## Equipo

| Integrante | Responsable de |
|---|---|
| | Setup, configuración, Usuario y autenticación |
| | Categorías, Zonas y carga inicial de datos |
| | Servicio: alta, modificación y baja |
| | Imágenes y catálogo (listado, filtros, detalle) |
| | Carrito, checkout y órdenes |
