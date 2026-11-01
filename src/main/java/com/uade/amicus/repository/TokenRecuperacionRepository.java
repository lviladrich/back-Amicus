package com.uade.amicus.repository;

import com.uade.amicus.model.TokenRecuperacion;
import com.uade.amicus.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, Long> {

    Optional<TokenRecuperacion> findByTokenHash(String tokenHash);

    /** Para anular los links anteriores cuando se pide uno nuevo. */
    List<TokenRecuperacion> findByUsuarioAndUsadoFalse(Usuario usuario);
}
