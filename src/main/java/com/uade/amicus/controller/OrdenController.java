package com.uade.amicus.controller;

import com.uade.amicus.dto.response.OrdenResponse;
import com.uade.amicus.service.CheckoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Historial de compras. */
@Tag(name = "6. Ordenes", description = "Historial de compras")
@RestController
@RequestMapping("/api/ordenes")
public class OrdenController {

    private final CheckoutService checkoutService;

    public OrdenController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @Operation(summary = "Historial de compras",
            description = "Ordenes del usuario, de la mas reciente a la mas antigua.")
    @GetMapping
    public ResponseEntity<List<OrdenResponse>> listarPorUsuario(@RequestParam Long usuarioId) {
        return ResponseEntity.ok(checkoutService.listarPorUsuario(usuarioId));
    }

    @Operation(summary = "Detalle de una orden",
            description = "Muestra el titulo y el precio CONGELADOS al momento de la compra, no los actuales del servicio. Un comprobante no se reescribe.")
    @GetMapping("/{id}")
    public ResponseEntity<OrdenResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(checkoutService.buscarPorId(id));
    }
}
