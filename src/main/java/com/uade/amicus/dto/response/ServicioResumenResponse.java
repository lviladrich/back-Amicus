package com.uade.amicus.dto.response;

import com.uade.amicus.model.Servicio;

import java.math.BigDecimal;

/**
 * Version liviana para los listados del catalogo.
 *
 * Existe separada del detalle porque la home muestra decenas de servicios y no
 * necesita la descripcion completa, todas las imagenes ni las zonas. Enviar solo
 * lo que la pantalla usa reduce el tamano de la respuesta y evita consultas
 * innecesarias.
 */
public record ServicioResumenResponse(
        Long id,
        String titulo,
        BigDecimal precio,
        Integer cuposDisponibles,
        boolean disponible,
        String categoria,
        String imagenPortada
) {
    public static ServicioResumenResponse desde(Servicio servicio) {
        String portada = servicio.getImagenes().stream()
                .min((a, b) -> Integer.compare(
                        a.getOrdenVisualizacion() == null ? Integer.MAX_VALUE : a.getOrdenVisualizacion(),
                        b.getOrdenVisualizacion() == null ? Integer.MAX_VALUE : b.getOrdenVisualizacion()))
                .map(imagen -> imagen.getUrl())
                .orElse(null);

        return new ServicioResumenResponse(
                servicio.getId(),
                servicio.getTitulo(),
                servicio.getPrecio(),
                servicio.getCuposDisponibles(),
                servicio.tieneCupos(),
                servicio.getCategoria().getNombre(),
                portada
        );
    }
}
