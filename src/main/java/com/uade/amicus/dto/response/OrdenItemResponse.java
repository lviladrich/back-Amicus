package com.uade.amicus.dto.response;

import com.uade.amicus.model.OrdenItem;

import java.math.BigDecimal;

/**
 * Linea de una orden.
 *
 * Devuelve el titulo, el precio y la frecuencia CONGELADOS, no los actuales del
 * servicio. Es la razon de ser de OrdenItem: el comprobante muestra lo que se
 * contrato y lo que se pago.
 */
public record OrdenItemResponse(
        Long id,
        Long servicioId,
        String titulo,
        Integer cantidad,
        String frecuencia,
        BigDecimal precioUnitario,
        BigDecimal subtotal
) {
    public static OrdenItemResponse desde(OrdenItem item) {
        return new OrdenItemResponse(
                item.getId(),
                item.getServicio().getId(),
                item.getTituloServicio(),
                item.getCantidad(),
                item.getFrecuencia().name(),
                item.getPrecioUnitario(),
                item.getSubtotal()
        );
    }
}
