package com.uade.amicus.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** La consigna pide que quien publica pueda manejar el stock de su publicacion. */
public record ActualizarCuposRequest(

        @NotNull(message = "Los cupos son obligatorios")
        @PositiveOrZero(message = "Los cupos no pueden ser negativos")
        Integer cuposDisponibles
) {}
