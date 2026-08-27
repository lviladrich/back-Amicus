package com.uade.amicus.dto.response;

import com.uade.amicus.model.Carrito;

import java.math.BigDecimal;
import java.util.List;

/**
 * Contenido del carrito con el total calculado.
 *
 * La consigna pide que el checkout calcule "el costo total de los productos".
 * Devolverlo ya calculado en cada consulta evita que el cliente lo sume por su
 * cuenta y llegue a un numero distinto.
 */
public record CarritoResponse(
        Long id,
        Long usuarioId,
        List<CarritoItemResponse> items,
        BigDecimal total,
        int cantidadDeItems
) {
    public static CarritoResponse desde(Carrito carrito) {
        List<CarritoItemResponse> items = carrito.getItems().stream()
                .map(CarritoItemResponse::desde)
                .toList();

        return new CarritoResponse(
                carrito.getId(),
                carrito.getUsuario().getId(),
                items,
                carrito.calcularTotal(),
                items.size()
        );
    }
}
