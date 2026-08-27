package com.uade.amicus.repository;

import com.uade.amicus.model.Orden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrdenRepository extends JpaRepository<Orden, Long> {

    /** Historial del usuario, de la mas reciente a la mas vieja. */
    List<Orden> findByUsuarioIdOrderByFechaDesc(Long usuarioId);

    @Query("""
            select distinct o from Orden o
            left join fetch o.items
            where o.id = :id
            """)
    Optional<Orden> buscarConItems(@Param("id") Long id);
}
