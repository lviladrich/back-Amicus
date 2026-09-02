package com.uade.amicus.dto.response;

import com.uade.amicus.model.Servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Version completa, para la pantalla de detalle.
 *
 * La consigna pide que al seleccionar un servicio se vea "una imagen mas
 * ampliada junto a su descripcion", y que se visualice si no hay disponibilidad.
 * El campo disponible resuelve eso ultimo sin que el cliente tenga que deducirlo.
 *
 * La calificacion aparece aca y no en el resumen del catalogo a proposito: el
 * promedio sale de una consulta agregada por servicio, y ponerlo en el listado
 * significaria una consulta extra por cada fila devuelta. El detalle se pide de
 * a uno, asi que ahi el costo es una sola consulta.
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
        List<ZonaResponse> zonas,
        Double promedioPuntaje,
        long cantidadResenas
) {
    /**
     * @param promedio null si el servicio no tiene reseñas todavia. No se
     *                 reemplaza por 0: un servicio nuevo no vale cero estrellas,
     *                 no tiene calificacion, y son dos cosas distintas.
     */
    public static ServicioDetalleResponse desde(Servicio servicio, Double promedio,
                                                long cantidadResenas) {
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
                        .toList(),
                redondear(promedio),
                cantidadResenas
        );
    }

    /** 4.333333 estrellas no le dice nada a nadie: se muestra 4.3. */
    private static Double redondear(Double promedio) {
        return promedio == null
                ? null
                : BigDecimal.valueOf(promedio).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
