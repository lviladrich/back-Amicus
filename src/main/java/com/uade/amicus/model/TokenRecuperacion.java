package com.uade.amicus.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Pedido para recuperar la contrasena. Se crea cuando el usuario pide el link
 * y se usa una sola vez, cuando elige la contrasena nueva.
 *
 * En la tabla se guarda el hash SHA-256 del token y no el token: si alguien
 * llegara a leer la base, no podria usar los links pendientes para entrar a
 * las cuentas. Es el mismo criterio que con las contrasenas.
 */
@Entity
@Table(name = "tokens_recuperacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenRecuperacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** SHA-256 en hexadecimal: siempre 64 caracteres. */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "expira_en", nullable = false)
    private LocalDateTime expiraEn;

    @Column(nullable = false)
    private Boolean usado;

    public boolean estaVigente() {
        return !usado && LocalDateTime.now().isBefore(expiraEn);
    }

    @PrePersist
    protected void alCrear() {
        if (usado == null) {
            usado = false;
        }
    }
}
