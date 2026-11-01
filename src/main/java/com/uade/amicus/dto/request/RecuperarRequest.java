package com.uade.amicus.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Mail de la cuenta a la que se le quiere cambiar la contrasena. */
public record RecuperarRequest(

        @NotBlank(message = "El mail es obligatorio")
        @Email(message = "El mail no tiene un formato valido")
        String email
) {}
