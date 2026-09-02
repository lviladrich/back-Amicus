package com.uade.amicus.dto.request;

import com.uade.amicus.model.Frecuencia;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Cambio de una linea del carrito ya existente.
 *
 * frecuencia es opcional y significa "no la toques": mandar solo la cantidad
 * cambia las visitas y conserva la recurrencia que la linea ya tenia.
 */
public record ActualizarCantidadRequest(

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        Integer cantidad,

        Frecuencia frecuencia
) {}
