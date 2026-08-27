package com.uade.amicus.dto.response;

import com.uade.amicus.model.Categoria;

public record CategoriaResponse(
        Long id,
        String nombre,
        String descripcion
) {
    public static CategoriaResponse desde(Categoria categoria) {
        return new CategoriaResponse(
                categoria.getId(),
                categoria.getNombre(),
                categoria.getDescripcion()
        );
    }
}
