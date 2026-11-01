package com.uade.amicus.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Token que llego por mail y contrasena nueva. La contrasena tiene las mismas
 * reglas que en el registro.
 */
public record RestablecerRequest(

        @NotBlank(message = "El token es obligatorio")
        String token,

        @NotBlank(message = "La contrasena es obligatoria")
        @Size(min = 6, max = 100, message = "La contrasena debe tener al menos 6 caracteres")
        String password
) {}
