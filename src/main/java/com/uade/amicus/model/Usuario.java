package com.uade.amicus.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

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
     * STRING y no ORDINAL: en la tabla se guarda "ADMIN" y no un 1. Con ORDINAL,
     * reordenar el enum cambiaria el rol de todos los usuarios ya guardados.
     *
     * ColumnDefault: la columna se agrego cuando ya habia usuarios cargados en
     * las bases locales de cada integrante. Sin un default a nivel de tabla,
     * Hibernate no puede agregar una columna NOT NULL a filas existentes y la
     * aplicacion no arranca. Con el default, los usuarios previos pasan a ser
     * USUARIO automaticamente.
     */
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'USUARIO'")
    @Column(nullable = false, length = 20)
    private Rol rol;

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
        if (rol == null) {
            rol = Rol.USUARIO;
        }
    }
}
