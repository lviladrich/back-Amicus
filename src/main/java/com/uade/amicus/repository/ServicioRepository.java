package com.uade.amicus.repository;

import com.uade.amicus.model.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServicioRepository extends JpaRepository<Servicio, Long> {

    /**
     * Catalogo de la home. La consigna pide el listado ordenado alfabeticamente.
     * Solo servicios activos: los dados de baja no se listan.
     */
    List<Servicio> findByActivoTrueOrderByTituloAsc();

    /**
     * Busqueda con filtros opcionales.
     *
     * El patron ":param is null or condicion" permite que un filtro no enviado
     * simplemente no filtre, sin escribir una consulta distinta por combinacion.
     *
     * left join a zonas: si el servicio no tiene zonas cargadas, igual aparece
     * cuando no se filtra por zona.
     */
    @Query("""
            select distinct s from Servicio s
            left join s.zonas z
            where s.activo = true
              and (:categoriaId is null or s.categoria.id = :categoriaId)
              and (:zonaId is null or z.id = :zonaId)
              and (:texto is null or lower(s.titulo) like lower(concat('%', :texto, '%'))
                                  or lower(s.descripcion) like lower(concat('%', :texto, '%')))
              and (:conCupo = false or s.cuposDisponibles > 0)
            order by s.titulo asc
            """)
    List<Servicio> buscar(@Param("categoriaId") Long categoriaId,
                          @Param("zonaId") Long zonaId,
                          @Param("texto") String texto,
                          @Param("conCupo") boolean conCupo);

    /**
     * Detalle del servicio. join fetch trae en una sola consulta el servicio con
     * su categoria, su profesional y sus zonas, en vez de una consulta por cada
     * relacion. Es la solucion al problema N+1.
     */
    @Query("""
            select distinct s from Servicio s
            left join fetch s.categoria
            left join fetch s.profesional
            left join fetch s.zonas
            where s.id = :id
            """)
    Optional<Servicio> buscarDetalle(@Param("id") Long id);

    /** Publicaciones de un usuario, para su panel. */
    List<Servicio> findByProfesionalIdAndActivoTrueOrderByTituloAsc(Long profesionalId);
}
