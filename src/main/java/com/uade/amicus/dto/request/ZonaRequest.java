package com.uade.amicus.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ZonaRequest(

        @NotBlank(message = "El nombre de la zona es obligatorio")
        @Size(max = 80)
        String nombre
) {}
