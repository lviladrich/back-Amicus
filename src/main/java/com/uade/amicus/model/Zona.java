package com.uade.amicus.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Barrio o partido donde un profesional presta servicio.
 *
 * Un servicio cubre varias zonas y una zona tiene muchos servicios: de ahi que
 * la relacion sea @ManyToMany. Es el caso que justifica esa anotacion en el
 * modelo, y no una relacion inventada para cumplir la consigna.
 */
@Entity
@Table(name = "zonas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Zona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String nombre;
}
