package com.uade.amicus.repository;

import com.uade.amicus.model.EstadoOrden;
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

    /**
     * Si el usuario contrato ese servicio en una orden que sigue confirmada.
     *
     * Es la condicion para poder reseñarlo. Se pregunta por la existencia y no se
     * traen las ordenes: alcanza un booleano y la base lo resuelve sin
     * materializar filas.
     *
     * Se exige el estado como parametro en vez de dejarlo fijo en la consulta
     * para no escribir el nombre completo del enum dentro del JPQL.
     */
    @Query("""
            select count(i) > 0 from Orden o
            join o.items i
            where o.usuario.id = :usuarioId
              and i.servicio.id = :servicioId
              and o.estado = :estado
            """)
    boolean existeCompraConfirmada(@Param("usuarioId") Long usuarioId,
                                   @Param("servicioId") Long servicioId,
                                   @Param("estado") EstadoOrden estado);
}
