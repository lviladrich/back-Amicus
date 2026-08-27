package com.uade.amicus.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Publicacion de un servicio del hogar. Es el equivalente al "producto" de la
 * consigna.
 *
 * Un servicio no tiene stock en el sentido literal, tiene disponibilidad. La
 * traduccion que adopta el proyecto: cuposDisponibles es la cantidad de veces
 * que el profesional puede tomar ese trabajo. El checkout descuenta un cupo,
 * exactamente como se descontaria stock.
 */
@Entity
@Table(name = "servicios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Servicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(nullable = false, length = 1000)
    private String descripcion;

    /**
     * BigDecimal y no double: los decimales binarios pierden precision al
     * sumar dinero. 0.1 + 0.2 en double no da exactamente 0.3.
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    /** Hace las veces de stock. Ver la nota de la clase. */
    @Column(name = "cupos_disponibles", nullable = false)
    private Integer cuposDisponibles;

    /** Baja logica: un servicio dado de baja no se borra, deja de listarse. */
    @Column(nullable = false)
    private Boolean activo;

    @Column(name = "fecha_publicacion", nullable = false)
    private LocalDateTime fechaPublicacion;

    /**
     * LAZY y no EAGER: sin esto, cada vez que se lee un servicio Hibernate
     * traeria tambien la categoria completa aunque nadie la use.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    /** Quien publica y va a prestar el servicio. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profesional_id", nullable = false)
    private Usuario profesional;

    /**
     * cascade ALL: al guardar el servicio se guardan sus imagenes.
     * orphanRemoval: al sacar una imagen de la lista, se borra de la base.
     */
    @OneToMany(mappedBy = "servicio", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ServicioImagen> imagenes = new ArrayList<>();

    /** Zonas de cobertura. Crea la tabla intermedia servicio_zonas. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "servicio_zonas",
            joinColumns = @JoinColumn(name = "servicio_id"),
            inverseJoinColumns = @JoinColumn(name = "zona_id")
    )
    @Builder.Default
    private Set<Zona> zonas = new HashSet<>();

    @PrePersist
    protected void alCrear() {
        if (fechaPublicacion == null) {
            fechaPublicacion = LocalDateTime.now();
        }
        if (activo == null) {
            activo = true;
        }
    }

    /** Metodos de conveniencia que mantienen los dos lados de la relacion en sincronia. */
    public void agregarImagen(ServicioImagen imagen) {
        imagenes.add(imagen);
        imagen.setServicio(this);
    }

    public void quitarImagen(ServicioImagen imagen) {
        imagenes.remove(imagen);
        imagen.setServicio(null);
    }

    /** Reglas de negocio que dependen solo del propio servicio. */
    public boolean tieneCupos() {
        return activo && cuposDisponibles != null && cuposDisponibles > 0;
    }

    public boolean tieneCuposPara(int cantidad) {
        return tieneCupos() && cuposDisponibles >= cantidad;
    }
}
