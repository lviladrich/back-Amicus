package com.uade.amicus.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Un servicio que un usuario guardo para ver despues.
 *
 * La restriccion unica sobre (usuario_id, servicio_id) evita guardar dos veces
 * el mismo servicio: marcarlo de nuevo no crea otra fila.
 */
@Entity
@Table(
        name = "favoritos",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_favorito_usuario_servicio",
                columnNames = {"usuario_id", "servicio_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Favorito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servicio_id", nullable = false)
    private Servicio servicio;

    /** Para listar primero los ultimos que se guardaron. */
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();
}
