package com.uade.amicus.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Persona registrada en Amicus.
 *
 * Un mismo usuario puede publicar servicios y contratar los de otros, igual que
 * en TaskRabbit. Por eso no hay entidades separadas de cliente y profesional.
 */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    /** Hash BCrypt. Nunca se guarda la contrasena en texto plano. */
    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, length = 80)
    private String apellido;

    @Column(name = "fecha_alta", nullable = false)
    private LocalDateTime fechaAlta;

    @Column(nullable = false)
    private Boolean activo;

    /**
     * Servicios que este usuario publico.
     * mappedBy indica que la columna profesional_id vive en la tabla servicios.
     */
    @OneToMany(mappedBy = "profesional")
    @Builder.Default
    private List<Servicio> serviciosPublicados = new ArrayList<>();

    /** Se ejecuta justo antes del INSERT, asi nadie olvida inicializar estos campos. */
    @PrePersist
    protected void alCrear() {
        if (fechaAlta == null) {
            fechaAlta = LocalDateTime.now();
        }
        if (activo == null) {
            activo = true;
        }
    }
}
