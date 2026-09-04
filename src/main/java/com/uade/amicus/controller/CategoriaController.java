package com.uade.amicus.controller;

import com.uade.amicus.dto.request.CategoriaRequest;
import com.uade.amicus.dto.response.CategoriaResponse;
import com.uade.amicus.service.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * La consigna pide que la home muestre el listado de categorias.
 */
@Tag(name = "2. Categorias", description = "Rubros de los servicios. La home los lista")
@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @Operation(summary = "Listar categorias",
            description = "Ordenadas alfabeticamente. La consigna pide que la home muestre los tipos de servicio que ofrece el sitio.")
    @GetMapping
    public ResponseEntity<List<CategoriaResponse>> listar() {
        return ResponseEntity.ok(categoriaService.listar());
    }

    @Operation(summary = "Buscar por id",
            description = "Devuelve el recurso o 404 si no existe.")
    @GetMapping("/{id}")
    public ResponseEntity<CategoriaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(categoriaService.buscarPorId(id));
    }

    @Operation(summary = "Crear una categoria")
    @PostMapping
    public ResponseEntity<CategoriaResponse> crear(@Valid @RequestBody CategoriaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaService.crear(request));
    }

    @Operation(summary = "Modificar",
            description = "PUT reemplaza el recurso completo: hay que enviar todos los campos.")
    @PutMapping("/{id}")
    public ResponseEntity<CategoriaResponse> actualizar(@PathVariable Long id,
                                                        @Valid @RequestBody CategoriaRequest request) {
        return ResponseEntity.ok(categoriaService.actualizar(id, request));
    }

    @Operation(summary = "Eliminar una categoria",
            description = "Devuelve 409 si hay servicios asociados a la categoria.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        categoriaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
