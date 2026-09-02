package com.uade.amicus.dto.request;

import com.uade.amicus.model.Frecuencia;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Alta de una linea del carrito.
 *
 * cantidad son las visitas a contratar y frecuencia dice cada cuanto se
 * repiten. frecuencia es opcional: si no viene, se asume UNICA, asi los clientes
 * que ya usaban este endpoint siguen funcionando sin cambiar nada.
 */
public record AgregarItemRequest(

        @NotNull(message = "El servicio es obligatorio")
        Long servicioId,

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        Integer cantidad,

        Frecuencia frecuencia
) {
    /** UNICA es el valor por defecto: contratar una vez es el caso mas comun. */
    public Frecuencia frecuenciaOUnica() {
        return frecuencia == null ? Frecuencia.UNICA : frecuencia;
    }
}
