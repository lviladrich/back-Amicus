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
 * Carrito de compras de un usuario.
 *
 * Es @OneToOne y no @OneToMany porque cada usuario tiene un unico carrito
 * abierto: al hacer checkout no se cierra ni se archiva, se vacia. Lo que queda
 * como registro historico es la Orden.
 */
@Entity
@Table(name = "carritos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Carrito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @OneToMany(mappedBy = "carrito", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CarritoItem> items = new ArrayList<>();

    @PrePersist
    protected void alCrear() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }

    public void agregarItem(CarritoItem item) {
        items.add(item);
        item.setCarrito(this);
    }

    public void quitarItem(CarritoItem item) {
        items.remove(item);
        item.setCarrito(null);
    }

    /** La consigna pide poder vaciar el carrito. */
    public void vaciar() {
        items.forEach(item -> item.setCarrito(null));
        items.clear();
    }

    /**
     * El total no se guarda como columna: se calcula.
     * Si el precio de un servicio cambia, el carrito refleja el precio actual.
     * La Orden, en cambio, si congela el precio. Ver OrdenItem.
     */
    public BigDecimal calcularTotal() {
        return items.stream()
                .map(CarritoItem::calcularSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
