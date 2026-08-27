package com.uade.amicus.dto.response;

import com.uade.amicus.model.Zona;

public record ZonaResponse(
        Long id,
        String nombre
) {
    public static ZonaResponse desde(Zona zona) {
        return new ZonaResponse(zona.getId(), zona.getNombre());
    }
}
