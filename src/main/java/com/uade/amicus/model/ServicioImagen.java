package com.uade.amicus.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Foto de una publicacion.
 *
 * La consigna pide poder adjuntar "una o mas fotos", asi que no alcanza con un
 * campo de texto en Servicio: hace falta una tabla aparte con relacion
 * uno a muchos.
 *
 * Se guarda la URL y no el archivo binario. Meter imagenes dentro de la base
 * la infla, hace lentas las consultas y complica el backup.
 */
@Entity
@Table(name = "servicio_imagenes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServicioImagen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String url;

    /** Define cual es la portada: la de menor orden. */
    @Column(name = "orden_visualizacion")
    private Integer ordenVisualizacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servicio_id", nullable = false)
    private Servicio servicio;
}
