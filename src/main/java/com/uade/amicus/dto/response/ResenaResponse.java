package com.uade.amicus.dto.response;

import com.uade.amicus.model.Resena;

import java.time.LocalDateTime;

/**
 * Una reseña como se muestra en la pantalla de detalle.
 *
 * Del autor se expone el username y el nombre, no el mail: quien lee opiniones
 * no necesita la direccion de correo de quien las escribio.
 */
public record ResenaResponse(
        Long id,
        Integer puntaje,
        String comentario,
        LocalDateTime fecha,
        Long autorId,
        String autor
) {
    public static ResenaResponse desde(Resena resena) {
        return new ResenaResponse(
                resena.getId(),
                resena.getPuntaje(),
                resena.getComentario(),
                resena.getFecha(),
                resena.getAutor().getId(),
                resena.getAutor().getUsername()
        );
    }
}
