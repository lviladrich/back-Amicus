package com.uade.amicus.repository;

import com.uade.amicus.model.Carrito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CarritoRepository extends JpaRepository<Carrito, Long> {

    Optional<Carrito> findByUsuarioId(Long usuarioId);

    /**
     * Trae el carrito con sus items y los servicios de cada item en una sola
     * consulta. Sin esto, calcular el total dispararia una consulta por linea.
     */
    @Query("""
            select distinct c from Carrito c
            left join fetch c.items i
            left join fetch i.servicio
            where c.usuario.id = :usuarioId
            """)
    Optional<Carrito> buscarConItems(@Param("usuarioId") Long usuarioId);
}
