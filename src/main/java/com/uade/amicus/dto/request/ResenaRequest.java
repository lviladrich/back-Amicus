package com.uade.amicus.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Alta de una reseña.
 *
 * El servicio y el autor no viajan en el cuerpo: el servicio sale de la ruta y
 * el autor del parametro usuarioId. El cliente no elige a nombre de quien
 * escribe.
 */
public record ResenaRequest(

        @NotNull(message = "El puntaje es obligatorio")
        @Min(value = 1, message = "El puntaje minimo es 1")
        @Max(value = 5, message = "El puntaje maximo es 5")
        Integer puntaje,

        @Size(max = 1000, message = "El comentario no puede superar los 1000 caracteres")
        String comentario
) {}
