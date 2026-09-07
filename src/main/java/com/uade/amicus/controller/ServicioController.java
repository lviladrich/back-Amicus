package com.uade.amicus.controller;

import com.uade.amicus.dto.request.ActualizarCuposRequest;
import com.uade.amicus.dto.request.ImagenRequest;
import com.uade.amicus.dto.request.ServicioRequest;
import com.uade.amicus.dto.response.PaginaResponse;
import com.uade.amicus.dto.response.ServicioDetalleResponse;
import com.uade.amicus.dto.response.ServicioResumenResponse;
import com.uade.amicus.service.ServicioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Catalogo y ABM de publicaciones.
 *
 * Cubre dos requisitos de la consigna: el listado ordenado alfabeticamente de la
 * home con su detalle, y la gestion de publicaciones por parte del profesional.
 */
@Validated
@Tag(name = "4. Servicios", description = "Catalogo y gestion de publicaciones")
@RestController
@RequestMapping("/api/servicios")
public class ServicioController {

    private final ServicioService servicioService;

    public ServicioController(ServicioService servicioService) {
        this.servicioService = servicioService;
    }

    /**
     * Listado del catalogo, ordenado alfabeticamente.
     *
     * Todos los filtros son opcionales (required = false): sin ninguno devuelve
     * el catalogo completo. La consigna pide que la API permita acceder a la
     * informacion "completa o filtrada", y esto resuelve las dos formas con un
     * solo endpoint en vez de multiplicar rutas.
     */
    @Operation(summary = "Catalogo de servicios",
            description = "Listado ordenado alfabeticamente, solo servicios activos, paginado. Todos los filtros son opcionales: sin ninguno devuelve el catalogo completo. La consigna pide poder acceder a la informacion completa o filtrada, y este endpoint resuelve las dos formas.")
    @GetMapping
    public ResponseEntity<PaginaResponse<ServicioResumenResponse>> listar(
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long zonaId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "false") boolean conCupo,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "La pagina no puede ser negativa")
            int page,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "El tamaño de pagina debe ser al menos 1")
            @Max(value = 100, message = "El tamaño de pagina no puede superar 100")
            int size) {

        boolean sinFiltros = categoriaId == null && zonaId == null
                && (q == null || q.isBlank()) && !conCupo;

        return ResponseEntity.ok(sinFiltros
                ? servicioService.listar(page, size)
                : servicioService.buscar(categoriaId, zonaId, q, conCupo, page, size));
    }

    /** Detalle: imagenes, descripcion completa, categoria, profesional y zonas. */
    @Operation(summary = "Detalle de un servicio",
            description = "Incluye descripcion completa, imagenes, categoria, profesional y zonas de cobertura. El campo disponible indica si tiene cupos.")
    @GetMapping("/{id}")
    public ResponseEntity<ServicioDetalleResponse> buscarDetalle(@PathVariable Long id) {
        return ResponseEntity.ok(servicioService.buscarDetalle(id));
    }

    /** Publicaciones de un profesional, para su panel. */
    @Operation(summary = "Publicaciones de un profesional",
            description = "Servicios activos publicados por ese usuario.")
    @GetMapping("/profesional/{profesionalId}")
    public ResponseEntity<List<ServicioResumenResponse>> listarPorProfesional(
            @PathVariable Long profesionalId) {
        return ResponseEntity.ok(servicioService.listarPorProfesional(profesionalId));
    }

    @Operation(summary = "Publicar un servicio",
            description = "Alta de una publicacion con su categoria, sus zonas de cobertura y una o mas fotos. El campo cuposDisponibles hace las veces de stock.")
    @PostMapping
    public ResponseEntity<ServicioDetalleResponse> crear(@Valid @RequestBody ServicioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicioService.crear(request));
    }

    /** PUT reemplaza el recurso completo. */
    @Operation(summary = "Modificar una publicacion",
            description = "PUT reemplaza el recurso completo: hay que enviar todos los campos. Solo el profesional que publico el servicio puede modificarlo: si usuarioId no coincide, devuelve 403.")
    @PutMapping("/{id}")
    public ResponseEntity<ServicioDetalleResponse> actualizar(@PathVariable Long id,
                                                              @RequestParam Long usuarioId,
                                                              @Valid @RequestBody ServicioRequest request) {
        return ResponseEntity.ok(servicioService.actualizar(id, usuarioId, request));
    }

    /**
     * PATCH y no PUT: modifica un solo campo, no reemplaza el recurso entero.
     * Esa es la diferencia entre los dos verbos.
     */
    @Operation(summary = "Ajustar cupos",
            description = "PATCH modifica solo los cupos, sin reenviar el resto de la publicacion. Solo el profesional que publico el servicio puede hacerlo: la consigna dice que el usuario que crea el producto es quien maneja su stock.")
    @PatchMapping("/{id}/cupos")
    public ResponseEntity<ServicioDetalleResponse> actualizarCupos(
            @PathVariable Long id,
            @RequestParam Long usuarioId,
            @Valid @RequestBody ActualizarCuposRequest request) {
        return ResponseEntity.ok(servicioService.actualizarCupos(id, usuarioId, request));
    }

    /** 204 NO CONTENT: se hizo, no hay nada que devolver. */
    @Operation(summary = "Dar de baja",
            description = "Baja logica: el servicio deja de listarse pero la fila sobrevive, porque puede estar referenciada en ordenes historicas. Solo el profesional que lo publico puede darlo de baja.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id,
                                         @RequestParam Long usuarioId) {
        servicioService.eliminar(id, usuarioId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Agregar una foto",
            description = "La consigna pide poder adjuntar una o mas fotos por publicacion.")
    @PostMapping("/{id}/imagenes")
    public ResponseEntity<ServicioDetalleResponse> agregarImagen(@PathVariable Long id,
                                                                 @RequestParam Long usuarioId,
                                                                 @Valid @RequestBody ImagenRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicioService.agregarImagen(id, usuarioId, request));
    }

    @Operation(summary = "Quitar una foto")
    @DeleteMapping("/{id}/imagenes/{imagenId}")
    public ResponseEntity<Void> eliminarImagen(@PathVariable Long id,
                                               @RequestParam Long usuarioId,
                                               @PathVariable Long imagenId) {
        servicioService.eliminarImagen(id, usuarioId, imagenId);
        return ResponseEntity.noContent().build();
    }
}
