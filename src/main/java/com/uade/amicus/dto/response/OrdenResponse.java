package com.uade.amicus.dto.response;

import com.uade.amicus.model.Orden;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrdenResponse(
        Long id,
        Long usuarioId,
        LocalDateTime fecha,
        String estado,
        BigDecimal total,
        List<OrdenItemResponse> items
) {
    public static OrdenResponse desde(Orden orden) {
        return new OrdenResponse(
                orden.getId(),
                orden.getUsuario().getId(),
                orden.getFecha(),
                orden.getEstado().name(),
                orden.getTotal(),
                orden.getItems().stream()
                        .map(OrdenItemResponse::desde)
                        .toList()
        );
    }
}
