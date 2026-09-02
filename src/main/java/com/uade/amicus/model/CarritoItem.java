package com.uade.amicus.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Una linea del carrito: un servicio, una cantidad de visitas y cada cuanto se
 * repiten.
 *
 * La restriccion unica sobre (carrito_id, servicio_id) evita que el mismo
 * servicio aparezca dos veces: agregarlo de nuevo suma cantidad en la linea que
 * ya existe. Por eso una linea tiene una sola frecuencia: si se agrega el mismo
 * servicio con otra, la nueva reemplaza a la anterior en vez de abrir una
 * segunda linea.
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

    /** Cantidad de visitas contratadas. Cada visita consume un cupo. */
    @Column(nullable = false)
    private Integer cantidad;

    /**
     * Cada cuanto se repiten esas visitas. UNICA por defecto, asi una linea sin
     * recurrencia se comporta igual que antes de existir este campo.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Frecuencia frecuencia = Frecuencia.UNICA;

    /**
     * Se calcula con el precio actual del servicio, no con uno guardado.
     *
     * La frecuencia no entra en la cuenta: ocho visitas cuestan lo mismo sean
     * semanales o mensuales. Lo que cambia es cuando se prestan, no el precio.
     */
    public BigDecimal calcularSubtotal() {
        return servicio.getPrecio().multiply(BigDecimal.valueOf(cantidad));
    }
}
