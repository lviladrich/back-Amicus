package com.uade.amicus.repository;

import com.uade.amicus.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Acceso a datos de Usuario.
 *
 * No tiene implementacion: Spring Data genera el SQL a partir del nombre de
 * cada metodo. findByEmail se traduce en "select * from usuarios where email = ?".
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** Para el login: la consigna pide identificar al usuario por mail. */
    Optional<Usuario> findByEmail(String email);

    /** Para validar en el registro sin traer el usuario entero. */
    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}
