package com.uade.amicus.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaRequest(

        @NotBlank(message = "El nombre de la categoria es obligatorio")
        @Size(max = 60)
        String nombre,

        @Size(max = 200)
        String descripcion
) {}
