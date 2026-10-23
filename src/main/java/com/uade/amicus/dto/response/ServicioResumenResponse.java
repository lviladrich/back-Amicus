package com.uade.amicus.dto.response;

import com.uade.amicus.model.Servicio;

import java.math.BigDecimal;
import java.util.List;

/**
 * Version liviana para los listados del catalogo.
 *
 * Existe separada del detalle porque la home muestra decenas de servicios y no
 * necesita la descripcion completa ni todas las imagenes. Enviar solo lo que la
 * pantalla usa reduce el tamano de la respuesta.
 *
 * La tarjeta de la maqueta muestra quien presta el servicio y donde, por eso van
 * el nombre del profesional y los nombres de las zonas (solo texto: no hace falta
 * el usuario completo, y asi no se expone su email en el catalogo publico).
 */
public record ServicioResumenResponse(
        Long id,
        String titulo,
        BigDecimal precio,
        Integer cuposDisponibles,
        boolean disponible,
        String categoria,
        String imagenPortada,
        String profesional,
        List<String> zonas
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
                portada,
                servicio.getProfesional().getNombre() + " " + servicio.getProfesional().getApellido(),
                servicio.getZonas().stream()
                        .map(zona -> zona.getNombre())
                        .sorted()
                        .toList()
        );
    }
}
