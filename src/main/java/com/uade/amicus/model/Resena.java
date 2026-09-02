package com.uade.amicus.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Calificacion que un cliente le deja a un servicio que contrato.
 *
 * En un marketplace de oficios la reputacion es el producto: nadie deja entrar a
 * un desconocido a su casa a tocar la instalacion de gas sin ver antes que opino
 * el resto. Por eso la reseña no es un adorno del catalogo.
 *
 * La restriccion unica sobre (servicio_id, autor_id) permite una sola reseña por
 * persona y por servicio. Sin eso, un mismo usuario podria inflar o hundir el
 * promedio repitiendo la misma opinion. Que solo pueda reseñar quien realmente
 * contrato el servicio se valida en ResenaService, porque depende de las
 * ordenes y no de esta tabla.
 */
@Entity
@Table(
        name = "resenas",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_resena_servicio_autor",
                columnNames = {"servicio_id", "autor_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resena {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servicio_id", nullable = false)
    private Servicio servicio;

    /** Quien escribe. Se llama autor y no usuario para no confundirlo con el profesional. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    /** De 1 a 5. El rango se valida en el DTO de entrada con @Min y @Max. */
    @Column(nullable = false)
    private Integer puntaje;

    /** Opcional: se puede calificar sin escribir nada. */
    @Column(length = 1000)
    private String comentario;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @PrePersist
    protected void alCrear() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
    }
}
