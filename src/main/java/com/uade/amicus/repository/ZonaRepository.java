package com.uade.amicus.repository;

import com.uade.amicus.model.Zona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface ZonaRepository extends JpaRepository<Zona, Long> {

    List<Zona> findAllByOrderByNombreAsc();

    /** Trae varias zonas de una sola consulta al asignarlas a un servicio. */
    Set<Zona> findByIdIn(Set<Long> ids);

    boolean existsByNombreIgnoreCase(String nombre);

    /** Para renombrar sin que choque contra el propio registro. */
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
