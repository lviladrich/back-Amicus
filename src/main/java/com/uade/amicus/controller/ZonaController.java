package com.uade.amicus.controller;

import com.uade.amicus.dto.request.ZonaRequest;
import com.uade.amicus.dto.response.ZonaResponse;
import com.uade.amicus.service.ZonaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "3. Zonas", description = "Barrios y partidos donde se presta servicio")
@RestController
@RequestMapping("/api/zonas")
public class ZonaController {

    private final ZonaService zonaService;

    public ZonaController(ZonaService zonaService) {
        this.zonaService = zonaService;
    }

    @Operation(summary = "Listar zonas",
            description = "Barrios de CABA y partidos del GBA, para filtrar el catalogo por cobertura.")
    @GetMapping
    public ResponseEntity<List<ZonaResponse>> listar() {
        return ResponseEntity.ok(zonaService.listar());
    }

    @Operation(summary = "Crear una zona")
    @PostMapping
    public ResponseEntity<ZonaResponse> crear(@Valid @RequestBody ZonaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(zonaService.crear(request));
    }

    @Operation(summary = "Buscar por id",
            description = "Devuelve el recurso o 404 si no existe.")
    @GetMapping("/{id}")
    public ResponseEntity<ZonaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(zonaService.buscarPorId(id));
    }

    @Operation(summary = "Modificar",
            description = "PUT reemplaza el recurso completo: hay que enviar todos los campos.")
    @PutMapping("/{id}")
    public ResponseEntity<ZonaResponse> actualizar(@PathVariable Long id,
                                                    @Valid @RequestBody ZonaRequest request) {
        return ResponseEntity.ok(zonaService.actualizar(id, request));
    }

    @Operation(summary = "Eliminar una zona",
            description = "Devuelve 409 si hay servicios asociados a la zona.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        zonaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
