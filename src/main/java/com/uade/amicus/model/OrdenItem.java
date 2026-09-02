package com.uade.amicus.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Una linea de una orden ya confirmada.
 *
 * Esta es la clase que mejor muestra la diferencia entre un dato vivo y un dato
 * historico. CarritoItem apunta al servicio y le pregunta el precio cada vez.
 * OrdenItem copia el titulo y el precio en el momento de la compra.
 *
 * Por que: si el electricista sube su servicio de 25.000 a 30.000, la orden que
 * el cliente pago el mes pasado tiene que seguir diciendo 25.000. Sin la copia,
 * el historial de compras se reescribiria solo cada vez que alguien cambia un
 * precio, y el comprobante dejaria de servir como comprobante.
 *
 * Lo mismo con el titulo: si el profesional edita o da de baja la publicacion,
 * la orden sigue mostrando que fue lo que se contrato.
 */
@Entity
@Table(name = "orden_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_id", nullable = false)
    private Orden orden;

    /**
     * Se conserva la referencia por trazabilidad, pero los datos que se muestran
     * son los copiados de abajo, no los que tenga el servicio hoy.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servicio_id", nullable = false)
    private Servicio servicio;

    /** Copia congelada del titulo al momento de la compra. */
    @Column(name = "titulo_servicio", nullable = false, length = 120)
    private String tituloServicio;

    /** Cantidad de visitas contratadas. */
    @Column(nullable = false)
    private Integer cantidad;

    /**
     * Frecuencia con la que se contrataron esas visitas.
     *
     * Tambien se congela: forma parte de lo que se contrato. Si el cliente pidio
     * ocho visitas semanales, el comprobante tiene que decir eso, aunque despues
     * vuelva a contratar el mismo servicio una sola vez.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Frecuencia frecuencia;

    /** Copia congelada del precio al momento de la compra. */
    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    /**
     * Se guarda calculado en vez de recalcularse: si mañana cambia la forma de
     * calcular (un descuento, un impuesto), las ordenes viejas no se alteran.
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    /** Construye la linea historica a partir de una linea del carrito. */
    public static OrdenItem desde(CarritoItem item) {
        return OrdenItem.builder()
                .servicio(item.getServicio())
                .tituloServicio(item.getServicio().getTitulo())
                .cantidad(item.getCantidad())
                .frecuencia(item.getFrecuencia())
                .precioUnitario(item.getServicio().getPrecio())
                .subtotal(item.calcularSubtotal())
                .build();
    }
}
