package com.uade.amicus.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ImagenRequest(

        @NotBlank(message = "La url de la imagen es obligatoria")
        @Size(max = 500)
        String url,

        Integer ordenVisualizacion
) {}
