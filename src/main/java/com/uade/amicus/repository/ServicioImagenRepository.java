package com.uade.amicus.repository;

import com.uade.amicus.model.ServicioImagen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServicioImagenRepository extends JpaRepository<ServicioImagen, Long> {

    List<ServicioImagen> findByServicioIdOrderByOrdenVisualizacionAsc(Long servicioId);
}
