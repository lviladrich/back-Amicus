package com.uade.amicus.repository;

import com.uade.amicus.model.Resena;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResenaRepository extends JpaRepository<Resena, Long> {

    /** Las mas nuevas primero, que es lo que espera ver cualquiera. */
    List<Resena> findByServicioIdOrderByFechaDesc(Long servicioId);

    /** Una sola reseña por persona y servicio. */
    boolean existsByServicioIdAndAutorId(Long servicioId, Long autorId);

    long countByServicioId(Long servicioId);

    /**
     * Promedio calculado por la base y no en Java.
     *
     * Traer todas las reseñas de un servicio para sacarles el promedio a mano
     * seria mover cientos de filas por la red para devolver un solo numero.
     *
     * Devuelve null cuando el servicio todavia no tiene reseñas: avg() sobre un
     * conjunto vacio no es 0, es "no se sabe". Un servicio nuevo no vale cero
     * estrellas, simplemente no tiene calificacion.
     */
    @Query("select avg(r.puntaje) from Resena r where r.servicio.id = :servicioId")
    Double promedioDe(@Param("servicioId") Long servicioId);
}
