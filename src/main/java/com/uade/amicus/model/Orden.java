package com.uade.amicus.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Resultado de un checkout: el comprobante de lo que el usuario contrato.
 *
 * A diferencia del Carrito, que es temporal y refleja precios actuales, la
 * Orden es un registro historico inmutable. Por eso guarda el total como
 * columna en vez de calcularlo: dentro de un ano tiene que seguir diciendo lo
 * mismo aunque todos los precios hayan cambiado.
 */
@Entity
@Table(name = "ordenes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Orden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private LocalDateTime fecha;

    /** Congelado al momento del checkout. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    /** STRING y no ORDINAL: si se agrega un estado, los guardados no se corren. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoOrden estado;

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrdenItem> items = new ArrayList<>();

    @PrePersist
    protected void alCrear() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
        if (estado == null) {
            estado = EstadoOrden.CONFIRMADA;
        }
    }

    public void agregarItem(OrdenItem item) {
        items.add(item);
        item.setOrden(this);
    }
}
