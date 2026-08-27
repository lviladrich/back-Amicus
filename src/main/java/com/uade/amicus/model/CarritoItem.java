package com.uade.amicus.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Una linea del carrito: un servicio con una cantidad.
 *
 * La restriccion unica sobre (carrito_id, servicio_id) evita que el mismo
 * servicio aparezca dos veces: agregarlo de nuevo suma cantidad en la linea que
 * ya existe.
 */
@Entity
@Table(
        name = "carrito_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_carrito_servicio",
                columnNames = {"carrito_id", "servicio_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarritoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "carrito_id", nullable = false)
    private Carrito carrito;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servicio_id", nullable = false)
    private Servicio servicio;

    @Column(nullable = false)
    private Integer cantidad;

    /** Se calcula con el precio actual del servicio, no con uno guardado. */
    public BigDecimal calcularSubtotal() {
        return servicio.getPrecio().multiply(BigDecimal.valueOf(cantidad));
    }
}
