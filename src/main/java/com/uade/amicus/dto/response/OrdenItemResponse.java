package com.uade.amicus.dto.response;

import com.uade.amicus.model.OrdenItem;

import java.math.BigDecimal;

/**
 * Linea de una orden.
 *
 * Devuelve el titulo y el precio CONGELADOS, no los actuales del servicio. Es
 * la razon de ser de OrdenItem: el comprobante muestra lo que se pago.
 */
public record OrdenItemResponse(
        Long id,
        Long servicioId,
        String titulo,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal
) {
    public static OrdenItemResponse desde(OrdenItem item) {
        return new OrdenItemResponse(
                item.getId(),
                item.getServicio().getId(),
                item.getTituloServicio(),
                item.getCantidad(),
                item.getPrecioUnitario(),
                item.getSubtotal()
        );
    }
}
