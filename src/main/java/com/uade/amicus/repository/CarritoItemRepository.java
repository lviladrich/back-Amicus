package com.uade.amicus.repository;

import com.uade.amicus.model.CarritoItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CarritoItemRepository extends JpaRepository<CarritoItem, Long> {

    /** Si el servicio ya esta en el carrito, se suma cantidad en vez de duplicar la linea. */
    Optional<CarritoItem> findByCarritoIdAndServicioId(Long carritoId, Long servicioId);
}
