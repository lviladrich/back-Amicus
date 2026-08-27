package com.uade.amicus.service;

import com.uade.amicus.dto.response.OrdenResponse;
import com.uade.amicus.exception.ConflictoException;
import com.uade.amicus.exception.RecursoNoEncontradoException;
import com.uade.amicus.exception.ReglaDeNegocioException;
import com.uade.amicus.model.Carrito;
import com.uade.amicus.model.CarritoItem;
import com.uade.amicus.model.EstadoOrden;
import com.uade.amicus.model.Orden;
import com.uade.amicus.model.OrdenItem;
import com.uade.amicus.model.Servicio;
import com.uade.amicus.repository.OrdenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Confirmacion de la compra.
 *
 * Es la operacion mas delicada del sistema porque toca tres cosas a la vez:
 * crea una orden, descuenta cupos de varios servicios y vacia el carrito. Si
 * alguna de las tres fallara a la mitad, la base quedaria inconsistente: cupos
 * descontados sin orden que los justifique, o una orden sin descuento.
 *
 * Por eso vive en su propio service y por eso el metodo es @Transactional.
 */
@Service
public class CheckoutService {

    private final CarritoService carritoService;
    private final OrdenRepository ordenRepository;

    public CheckoutService(CarritoService carritoService,
                           OrdenRepository ordenRepository) {
        this.carritoService = carritoService;
        this.ordenRepository = ordenRepository;
    }

    /**
     * Confirma el carrito y genera la orden.
     *
     * Que hace @Transactional: abre una transaccion al entrar y la confirma al
     * salir sin errores. Si en cualquier punto se lanza una excepcion no
     * controlada, deshace TODO lo hecho hasta ese momento. No queda una compra a
     * medias.
     *
     * El orden de los pasos importa:
     *
     * 1. Validar TODOS los items antes de tocar nada. Si el tercero de cuatro no
     *    tiene cupos, no se descontaron los dos primeros.
     * 2. Recien despues descontar, construir la orden y vaciar el carrito.
     *
     * Es el mismo criterio de una transferencia bancaria: primero se verifica
     * que haya saldo, despues se mueve la plata.
     */
    @Transactional
    public OrdenResponse confirmar(Long usuarioId) {
        Carrito carrito = carritoService.obtenerEntidad(usuarioId);
        List<CarritoItem> items = carrito.getItems();

        if (items.isEmpty()) {
            throw new ReglaDeNegocioException("El carrito esta vacio");
        }

        // Paso 1: validar todo antes de modificar nada.
        for (CarritoItem item : items) {
            Servicio servicio = item.getServicio();

            if (Boolean.FALSE.equals(servicio.getActivo())) {
                throw new ConflictoException(
                        "El servicio \"" + servicio.getTitulo() + "\" ya no esta disponible");
            }
            if (!servicio.tieneCuposPara(item.getCantidad())) {
                throw new ConflictoException(
                        "El servicio \"" + servicio.getTitulo() + "\" solo tiene "
                                + servicio.getCuposDisponibles() + " cupos y pediste "
                                + item.getCantidad());
            }
        }

        // Paso 2: construir la orden congelando titulo y precio de cada linea.
        Orden orden = Orden.builder()
                .usuario(carrito.getUsuario())
                .estado(EstadoOrden.CONFIRMADA)
                .total(BigDecimal.ZERO)
                .build();

        BigDecimal total = BigDecimal.ZERO;

        for (CarritoItem item : items) {
            Servicio servicio = item.getServicio();

            // El descuento de cupos que pide la consigna.
            servicio.setCuposDisponibles(servicio.getCuposDisponibles() - item.getCantidad());

            OrdenItem ordenItem = OrdenItem.desde(item);
            orden.agregarItem(ordenItem);
            total = total.add(ordenItem.getSubtotal());
        }

        orden.setTotal(total);
        Orden guardada = ordenRepository.save(orden);

        // Paso 3: el carrito queda vacio, la orden es el registro de la compra.
        carrito.vaciar();

        return OrdenResponse.desde(guardada);
    }

    @Transactional(readOnly = true)
    public OrdenResponse buscarPorId(Long id) {
        Orden orden = ordenRepository.buscarConItems(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Orden", id));
        return OrdenResponse.desde(orden);
    }

    @Transactional(readOnly = true)
    public List<OrdenResponse> listarPorUsuario(Long usuarioId) {
        return ordenRepository.findByUsuarioIdOrderByFechaDesc(usuarioId).stream()
                .map(OrdenResponse::desde)
                .toList();
    }
}
