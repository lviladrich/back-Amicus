package com.uade.amicus.controller;

import com.uade.amicus.dto.response.ServicioResumenResponse;
import com.uade.amicus.security.UsuarioPrincipal;
import com.uade.amicus.service.FavoritoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Servicios guardados como favoritos. Igual que el carrito, el dueno se toma
 * del usuario autenticado y no de un parametro de la URL.
 */
@Tag(name = "8. Favoritos", description = "Servicios guardados por el usuario")
@RestController
@RequestMapping("/api/favoritos")
@Validated
public class FavoritoController {

    private final FavoritoService favoritoService;

    public FavoritoController(FavoritoService favoritoService) {
        this.favoritoService = favoritoService;
    }

    @Operation(summary = "Ver mis favoritos",
            description = "Devuelve los servicios guardados, del mas reciente al mas antiguo.")
    @GetMapping
    public ResponseEntity<List<ServicioResumenResponse>> listar(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(favoritoService.listar(principal.getUsuario().getId()));
    }

    @Operation(summary = "Agregar a favoritos",
            description = "Si el servicio ya estaba guardado no hace nada. Devuelve la lista actualizada, o 404 si el servicio no existe.")
    @PostMapping("/{servicioId}")
    public ResponseEntity<List<ServicioResumenResponse>> agregar(@AuthenticationPrincipal UsuarioPrincipal principal,
                                                                 @PathVariable @Positive Long servicioId) {
        return ResponseEntity.ok(favoritoService.agregar(principal.getUsuario().getId(), servicioId));
    }

    @Operation(summary = "Quitar de favoritos",
            description = "Si el servicio no estaba guardado no hace nada. Devuelve la lista actualizada.")
    @DeleteMapping("/{servicioId}")
    public ResponseEntity<List<ServicioResumenResponse>> quitar(@AuthenticationPrincipal UsuarioPrincipal principal,
                                                                @PathVariable @Positive Long servicioId) {
        return ResponseEntity.ok(favoritoService.quitar(principal.getUsuario().getId(), servicioId));
    }
}
