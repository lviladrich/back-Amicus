package com.uade.amicus.controller;

import com.uade.amicus.dto.response.OrdenResponse;
import com.uade.amicus.dto.response.PaginaResponse;
import com.uade.amicus.service.CheckoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            description = "Ordenes del usuario, de la mas reciente a la mas antigua, paginado.")
    @GetMapping
    public ResponseEntity<PaginaResponse<OrdenResponse>> listarPorUsuario(
            @RequestParam Long usuarioId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(checkoutService.listarPorUsuario(usuarioId, page, size));
    }

    @Operation(summary = "Detalle de una orden",
            description = "Muestra el titulo y el precio CONGELADOS al momento de la compra, no los actuales del servicio. Un comprobante no se reescribe.")
    @GetMapping("/{id}")
    public ResponseEntity<OrdenResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(checkoutService.buscarPorId(id));
    }

    /**
     * PATCH y no DELETE: cancelar no borra la orden, le cambia el estado. La
     * orden sigue existiendo en el historial.
     */
    @Operation(summary = "Cancelar una orden",
            description = "Pasa la orden a estado CANCELADA y devuelve los cupos a cada servicio, que es la operacion inversa del checkout. La orden no se borra ni cambia su total: sigue en el historial. Devuelve 403 si el usuario no es quien hizo la compra y 400 si ya estaba cancelada.")
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<OrdenResponse> cancelar(@PathVariable Long id,
                                                  @RequestParam Long usuarioId) {
        return ResponseEntity.ok(checkoutService.cancelar(id, usuarioId));
    }
}
