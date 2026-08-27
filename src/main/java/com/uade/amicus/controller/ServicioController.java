package com.uade.amicus.controller;

import com.uade.amicus.dto.request.ActualizarCuposRequest;
import com.uade.amicus.dto.request.ImagenRequest;
import com.uade.amicus.dto.request.ServicioRequest;
import com.uade.amicus.dto.response.ServicioDetalleResponse;
import com.uade.amicus.dto.response.ServicioResumenResponse;
import com.uade.amicus.service.ServicioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Catalogo y ABM de publicaciones.
 *
 * Cubre dos requisitos de la consigna: el listado ordenado alfabeticamente de la
 * home con su detalle, y la gestion de publicaciones por parte del profesional.
 */
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
    @GetMapping("/{id}")
    public ResponseEntity<ServicioDetalleResponse> buscarDetalle(@PathVariable Long id) {
        return ResponseEntity.ok(servicioService.buscarDetalle(id));
    }

    /** Publicaciones de un profesional, para su panel. */
    @GetMapping("/profesional/{profesionalId}")
    public ResponseEntity<List<ServicioResumenResponse>> listarPorProfesional(
            @PathVariable Long profesionalId) {
        return ResponseEntity.ok(servicioService.listarPorProfesional(profesionalId));
    }

    @PostMapping
    public ResponseEntity<ServicioDetalleResponse> crear(@Valid @RequestBody ServicioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicioService.crear(request));
    }

    /** PUT reemplaza el recurso completo. */
    @PutMapping("/{id}")
    public ResponseEntity<ServicioDetalleResponse> actualizar(@PathVariable Long id,
                                                              @Valid @RequestBody ServicioRequest request) {
        return ResponseEntity.ok(servicioService.actualizar(id, request));
    }

    /**
     * PATCH y no PUT: modifica un solo campo, no reemplaza el recurso entero.
     * Esa es la diferencia entre los dos verbos.
     */
    @PatchMapping("/{id}/cupos")
    public ResponseEntity<ServicioDetalleResponse> actualizarCupos(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarCuposRequest request) {
        return ResponseEntity.ok(servicioService.actualizarCupos(id, request));
    }

    /** 204 NO CONTENT: se hizo, no hay nada que devolver. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        servicioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/imagenes")
    public ResponseEntity<ServicioDetalleResponse> agregarImagen(@PathVariable Long id,
                                                                 @Valid @RequestBody ImagenRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicioService.agregarImagen(id, request));
    }

    @DeleteMapping("/{id}/imagenes/{imagenId}")
    public ResponseEntity<Void> eliminarImagen(@PathVariable Long id, @PathVariable Long imagenId) {
        servicioService.eliminarImagen(id, imagenId);
        return ResponseEntity.noContent().build();
    }
}
