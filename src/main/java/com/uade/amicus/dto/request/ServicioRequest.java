package com.uade.amicus.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Alta o modificacion de una publicacion.
 *
 * Recibe ids de categoria y zonas, no objetos completos: el cliente no tiene por
 * que enviar una categoria entera para decir a cual pertenece.
 */
public record ServicioRequest(

        @NotBlank(message = "El titulo es obligatorio")
        @Size(max = 120)
        String titulo,

        @NotBlank(message = "La descripcion es obligatoria")
        @Size(max = 1000)
        String descripcion,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor a cero")
        BigDecimal precio,

        @NotNull(message = "Los cupos disponibles son obligatorios")
        @PositiveOrZero(message = "Los cupos no pueden ser negativos")
        Integer cuposDisponibles,

        @NotNull(message = "La categoria es obligatoria")
        Long categoriaId,

        @NotNull(message = "El profesional que publica es obligatorio")
        Long profesionalId,

        Set<Long> zonaIds,

        List<String> imagenes
) {}
