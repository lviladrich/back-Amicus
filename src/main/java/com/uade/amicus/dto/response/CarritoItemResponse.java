package com.uade.amicus.dto.response;

import com.uade.amicus.model.CarritoItem;

import java.math.BigDecimal;

public record CarritoItemResponse(
        Long id,
        Long servicioId,
        String titulo,
        BigDecimal precioUnitario,
        Integer cantidad,
        String frecuencia,
        BigDecimal subtotal,
        Integer cuposDisponibles,
        boolean disponible,
        String profesional,
        String categoria
) {
    public static CarritoItemResponse desde(CarritoItem item) {
        return new CarritoItemResponse(
                item.getId(),
                item.getServicio().getId(),
                item.getServicio().getTitulo(),
                item.getServicio().getPrecio(),
                item.getCantidad(),
                item.getFrecuencia().name(),
                item.calcularSubtotal(),
                item.getServicio().getCuposDisponibles(),
                item.getServicio().tieneCuposPara(item.getCantidad()),
                item.getServicio().getProfesional().getNombre() + " "
                        + item.getServicio().getProfesional().getApellido(),
                item.getServicio().getCategoria().getNombre()
        );
    }
}
