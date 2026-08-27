package com.uade.amicus.controller;

import com.uade.amicus.dto.request.ActualizarCuposRequest;
import com.uade.amicus.dto.request.ImagenRequest;
import com.uade.amicus.dto.request.ServicioRequest;
import com.uade.amicus.dto.response.ServicioDetalleResponse;
import com.uade.amicus.dto.response.ServicioResumenResponse;
import com.uade.amicus.service.ServicioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Catalogo y ABM de publicaciones.
 *
 * Cubre dos requisitos de la consigna: el listado ordenado alfabeticamente de la
 * home con su detalle, y la gestion de publicaciones por parte del profesional.
 */
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
            description = "Listado ordenado alfabeticamente, solo servicios activos. Todos los filtros son opcionales: sin ninguno devuelve el catalogo completo. La consigna pide poder acceder a la informacion completa o filtrada, y este endpoint resuelve las dos formas.")
    @GetMapping
    public ResponseEntity<List<ServicioResumenResponse>> listar(
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long zonaId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "false") boolean conCupo) {

        boolean sinFiltros = categoriaId == null && zonaId == null
                && (q == null || q.isBlank()) && !conCupo;

        return ResponseEntity.ok(sinFiltros
                ? servicioService.listar()
                : servicioService.buscar(categoriaId, zonaId, q, conCupo));
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
            description = "PUT reemplaza el recurso completo: hay que enviar todos los campos.")
    @PutMapping("/{id}")
    public ResponseEntity<ServicioDetalleResponse> actualizar(@PathVariable Long id,
                                                              @Valid @RequestBody ServicioRequest request) {
        return ResponseEntity.ok(servicioService.actualizar(id, request));
    }

    /**
     * PATCH y no PUT: modifica un solo campo, no reemplaza el recurso entero.
     * Esa es la diferencia entre los dos verbos.
     */
    @Operation(summary = "Ajustar cupos",
            description = "PATCH modifica solo los cupos, sin reenviar el resto de la publicacion.")
    @PatchMapping("/{id}/cupos")
    public ResponseEntity<ServicioDetalleResponse> actualizarCupos(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarCuposRequest request) {
        return ResponseEntity.ok(servicioService.actualizarCupos(id, request));
    }

    /** 204 NO CONTENT: se hizo, no hay nada que devolver. */
    @Operation(summary = "Dar de baja",
            description = "Baja logica: el servicio deja de listarse pero la fila sobrevive, porque puede estar referenciada en ordenes historicas.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        servicioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Agregar una foto",
            description = "La consigna pide poder adjuntar una o mas fotos por publicacion.")
    @PostMapping("/{id}/imagenes")
    public ResponseEntity<ServicioDetalleResponse> agregarImagen(@PathVariable Long id,
                                                                 @Valid @RequestBody ImagenRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicioService.agregarImagen(id, request));
    }

    @Operation(summary = "Quitar una foto")
    @DeleteMapping("/{id}/imagenes/{imagenId}")
    public ResponseEntity<Void> eliminarImagen(@PathVariable Long id, @PathVariable Long imagenId) {
        servicioService.eliminarImagen(id, imagenId);
        return ResponseEntity.noContent().build();
    }
}
