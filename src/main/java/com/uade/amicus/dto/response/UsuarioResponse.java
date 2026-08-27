package com.uade.amicus.dto.response;

import com.uade.amicus.model.Usuario;

/**
 * Datos publicos de un usuario.
 *
 * Notar que NO incluye la contrasena. Ese es el motivo principal por el que
 * existen los DTOs: si el controller devolviera la entidad Usuario, el hash de
 * la contrasena viajaria en cada respuesta JSON.
 */
public record UsuarioResponse(
        Long id,
        String username,
        String email,
        String nombre,
        String apellido
) {
    public static UsuarioResponse desde(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getEmail(),
                usuario.getNombre(),
                usuario.getApellido()
        );
    }
}
