package com.uade.amicus.controller;

import com.uade.amicus.dto.response.OrdenResponse;
import com.uade.amicus.service.CheckoutService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Historial de compras. */
@RestController
@RequestMapping("/api/ordenes")
public class OrdenController {

    private final CheckoutService checkoutService;

    public OrdenController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @GetMapping
    public ResponseEntity<List<OrdenResponse>> listarPorUsuario(@RequestParam Long usuarioId) {
        return ResponseEntity.ok(checkoutService.listarPorUsuario(usuarioId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrdenResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(checkoutService.buscarPorId(id));
    }
}
