package com.uade.amicus.repository;

import com.uade.amicus.model.Favorito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FavoritoRepository extends JpaRepository<Favorito, Long> {

    /** Solo servicios activos: uno dado de baja deja de aparecer en favoritos. */
    @Query("""
            SELECT f FROM Favorito f
            JOIN FETCH f.servicio s
            WHERE f.usuario.id = :usuarioId AND s.activo = true
            ORDER BY f.creadoEn DESC
            """)
    List<Favorito> listarDelUsuario(@Param("usuarioId") Long usuarioId);

    Optional<Favorito> findByUsuarioIdAndServicioId(Long usuarioId, Long servicioId);

    boolean existsByUsuarioIdAndServicioId(Long usuarioId, Long servicioId);
}
