package com.uade.amicus.controller;

import com.uade.amicus.dto.request.ResenaRequest;
import com.uade.amicus.dto.response.PaginaResponse;
import com.uade.amicus.dto.response.ResenaResponse;
import com.uade.amicus.service.ResenaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Calificaciones de los servicios.
 *
 * Las reseñas de un servicio cuelgan de su ruta (/servicios/{id}/resenas)
 * porque no existen por su cuenta: siempre son de algo. El borrado, en cambio,
 * apunta a la reseña directo, que es el recurso que se esta tocando.
 */
@Validated
@Tag(name = "7. Resenas", description = "Calificaciones de los servicios")
@RestController
@RequestMapping("/api")
public class ResenaController {

    private final ResenaService resenaService;

    public ResenaController(ResenaService resenaService) {
        this.resenaService = resenaService;
    }

    @Operation(summary = "Reseñas de un servicio",
            description = "Listado paginado de las mas nuevas a las mas viejas. El promedio y la cantidad vienen en el detalle del servicio.")
    @GetMapping("/servicios/{servicioId}/resenas")
    public ResponseEntity<PaginaResponse<ResenaResponse>> listar(
            @PathVariable
            @Positive(message = "El id del servicio debe ser mayor que 0")
            Long servicioId,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "La pagina no puede ser negativa")
            int page,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "El tamaño de pagina debe ser al menos 1")
            @Max(value = 100, message = "El tamaño de pagina no puede superar 100")
            int size) {
        return ResponseEntity.ok(resenaService.listarPorServicio(servicioId, page, size));
    }

    @Operation(summary = "Reseñar un servicio",
            description = "Puntaje de 1 a 5 y comentario opcional. Solo puede reseñar quien tenga una orden CONFIRMADA que incluya el servicio: devuelve 403 si no la tiene, 400 si intenta reseñar su propia publicacion y 409 si ya lo habia reseñado.")
    @PostMapping("/servicios/{servicioId}/resenas")
    public ResponseEntity<ResenaResponse> crear(
            @PathVariable
            @Positive(message = "El id del servicio debe ser mayor que 0")
            Long servicioId,
            @RequestParam
            @Positive(message = "El id de usuario debe ser mayor que 0")
            Long usuarioId,
            @Valid @RequestBody ResenaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(resenaService.crear(servicioId, usuarioId, request));
    }

    @Operation(summary = "Borrar una reseña",
            description = "Solo el autor puede borrar la suya. Devuelve 403 en cualquier otro caso.")
    @DeleteMapping("/resenas/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable
            @Positive(message = "El id de la reseña debe ser mayor que 0")
            Long id,
            @RequestParam
            @Positive(message = "El id de usuario debe ser mayor que 0")
            Long usuarioId) {
        resenaService.eliminar(id, usuarioId);
        return ResponseEntity.noContent().build();
    }
}
