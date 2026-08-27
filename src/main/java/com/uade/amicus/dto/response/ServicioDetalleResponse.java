package com.uade.amicus.dto.response;

import com.uade.amicus.model.Servicio;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Version completa, para la pantalla de detalle.
 *
 * La consigna pide que al seleccionar un servicio se vea "una imagen mas
 * ampliada junto a su descripcion", y que se visualice si no hay disponibilidad.
 * El campo disponible resuelve eso ultimo sin que el cliente tenga que deducirlo.
 */
public record ServicioDetalleResponse(
        Long id,
        String titulo,
        String descripcion,
        BigDecimal precio,
        Integer cuposDisponibles,
        boolean disponible,
        LocalDateTime fechaPublicacion,
        CategoriaResponse categoria,
        UsuarioResponse profesional,
        List<ImagenResponse> imagenes,
        List<ZonaResponse> zonas
) {
    public static ServicioDetalleResponse desde(Servicio servicio) {
        return new ServicioDetalleResponse(
                servicio.getId(),
                servicio.getTitulo(),
                servicio.getDescripcion(),
                servicio.getPrecio(),
                servicio.getCuposDisponibles(),
                servicio.tieneCupos(),
                servicio.getFechaPublicacion(),
                CategoriaResponse.desde(servicio.getCategoria()),
                UsuarioResponse.desde(servicio.getProfesional()),
                servicio.getImagenes().stream()
                        .sorted(Comparator.comparing(
                                imagen -> imagen.getOrdenVisualizacion() == null
                                        ? Integer.MAX_VALUE : imagen.getOrdenVisualizacion()))
                        .map(ImagenResponse::desde)
                        .toList(),
                servicio.getZonas().stream()
                        .map(ZonaResponse::desde)
                        .sorted(Comparator.comparing(ZonaResponse::nombre))
                        .toList()
        );
    }
}
