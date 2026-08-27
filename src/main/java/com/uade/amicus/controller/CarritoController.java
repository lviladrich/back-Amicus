package com.uade.amicus.controller;

import com.uade.amicus.dto.request.ActualizarCantidadRequest;
import com.uade.amicus.dto.request.AgregarItemRequest;
import com.uade.amicus.dto.response.CarritoResponse;
import com.uade.amicus.dto.response.OrdenResponse;
import com.uade.amicus.service.CarritoService;
import com.uade.amicus.service.CheckoutService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Carrito de compras y checkout.
 *
 * Sobre el parametro usuarioId: la consigna no pide tokens ni sesiones, asi que
 * el usuario se identifica con un parametro explicito. En un sistema real esto
 * saldria de un JWT y jamas de un parametro que el cliente puede cambiar a mano.
 * Es una simplificacion consciente, no un descuido.
 */
@Tag(name = "5. Carrito", description = "Carrito de compras y checkout")
@RestController
@RequestMapping("/api/carrito")
public class CarritoController {

    private final CarritoService carritoService;
    private final CheckoutService checkoutService;

    public CarritoController(CarritoService carritoService, CheckoutService checkoutService) {
        this.carritoService = carritoService;
        this.checkoutService = checkoutService;
    }

    /** Contenido del carrito con el total ya calculado. */
    @Operation(summary = "Ver el carrito",
            description = "Devuelve los items con el total ya calculado a partir de los precios actuales.")
    @GetMapping
    public ResponseEntity<CarritoResponse> ver(@RequestParam Long usuarioId) {
        return ResponseEntity.ok(carritoService.ver(usuarioId));
    }

    /** 409 si el servicio no tiene cupos, como pide la consigna. */
    @Operation(summary = "Agregar al carrito",
            description = "Devuelve 409 si el servicio no tiene cupos, 400 si se pide mas cantidad que la disponible o si el usuario intenta contratar su propio servicio. Si el servicio ya estaba, suma cantidad en vez de duplicar la linea.")
    @PostMapping("/items")
    public ResponseEntity<CarritoResponse> agregarItem(@RequestParam Long usuarioId,
                                                       @Valid @RequestBody AgregarItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(carritoService.agregarItem(usuarioId, request));
    }

    @Operation(summary = "Cambiar la cantidad de un item")
    @PutMapping("/items/{itemId}")
    public ResponseEntity<CarritoResponse> actualizarCantidad(
            @RequestParam Long usuarioId,
            @PathVariable Long itemId,
            @Valid @RequestBody ActualizarCantidadRequest request) {
        return ResponseEntity.ok(carritoService.actualizarCantidad(usuarioId, itemId, request));
    }

    /** Elimina un item del carrito. */
    @Operation(summary = "Eliminar un item del carrito")
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CarritoResponse> eliminarItem(@RequestParam Long usuarioId,
                                                        @PathVariable Long itemId) {
        return ResponseEntity.ok(carritoService.eliminarItem(usuarioId, itemId));
    }

    /** Vacia el carrito. */
    @Operation(summary = "Vaciar el carrito")
    @DeleteMapping
    public ResponseEntity<CarritoResponse> vaciar(@RequestParam Long usuarioId) {
        return ResponseEntity.ok(carritoService.vaciar(usuarioId));
    }

    /**
     * Checkout: calcula el total, descuenta los cupos y genera la orden.
     * Sin procesamiento de pago, como aclara la consigna.
     */
    @Operation(summary = "Confirmar la compra",
            description = "Valida los cupos de todos los items antes de modificar nada, los descuenta, crea la orden congelando titulo y precio de cada linea, y vacia el carrito. Todo en una sola transaccion. Sin procesamiento de pago, como aclara la consigna.")
    @PostMapping("/checkout")
    public ResponseEntity<OrdenResponse> checkout(@RequestParam Long usuarioId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(checkoutService.confirmar(usuarioId));
    }
}
