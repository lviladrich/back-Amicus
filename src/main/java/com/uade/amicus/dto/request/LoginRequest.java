package com.uade.amicus.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** La consigna especifica que el login pide mail y contrasena. */
public record LoginRequest(

        @NotBlank(message = "El mail es obligatorio")
        @Email(message = "El mail no tiene un formato valido")
        String email,

        @NotBlank(message = "La contrasena es obligatoria")
        String password
) {}
