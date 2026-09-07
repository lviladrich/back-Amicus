package com.uade.amicus.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Modificacion de una publicacion.
 *
 * Solo incluye los campos editables.
 * El profesional propietario no puede cambiarse y las imagenes
 * se administran mediante endpoints especificos.
 */
public record ActualizarServicioRequest(

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

        Set<Long> zonaIds

) {}