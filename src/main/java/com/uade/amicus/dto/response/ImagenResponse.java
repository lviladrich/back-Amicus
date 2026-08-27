package com.uade.amicus.dto.response;

import com.uade.amicus.model.ServicioImagen;

public record ImagenResponse(
        Long id,
        String url,
        Integer ordenVisualizacion
) {
    public static ImagenResponse desde(ServicioImagen imagen) {
        return new ImagenResponse(
                imagen.getId(),
                imagen.getUrl(),
                imagen.getOrdenVisualizacion()
        );
    }
}
