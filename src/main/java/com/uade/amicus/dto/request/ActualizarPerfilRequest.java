package com.uade.amicus.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Edicion de perfil. No incluye username ni password a proposito: cambiar el
 * nombre de usuario o la contrasena son operaciones mas sensibles, con sus
 * propias validaciones, y quedan fuera del alcance de este endpoint.
 */
public record ActualizarPerfilRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80)
        String nombre,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 80)
        String apellido,

        @NotBlank(message = "El mail es obligatorio")
        @Email(message = "El mail no tiene un formato valido")
        @Size(max = 120)
        String email
) {}
