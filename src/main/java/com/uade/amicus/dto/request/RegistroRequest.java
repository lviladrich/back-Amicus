package com.uade.amicus.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos del registro. La consigna pide exactamente estos cinco campos.
 *
 * Las anotaciones de validacion se verifican automaticamente cuando el
 * controller marca el parametro con @Valid. Si algo falla, el manejador global
 * devuelve un 400 con el detalle campo por campo.
 */
public record RegistroRequest(

        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Size(min = 3, max = 50, message = "El nombre de usuario debe tener entre 3 y 50 caracteres")
        String username,

        @NotBlank(message = "El mail es obligatorio")
        @Email(message = "El mail no tiene un formato valido")
        @Size(max = 120)
        String email,

        @NotBlank(message = "La contrasena es obligatoria")
        @Size(min = 6, max = 100, message = "La contrasena debe tener al menos 6 caracteres")
        String password,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80)
        String nombre,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 80)
        String apellido
) {}
