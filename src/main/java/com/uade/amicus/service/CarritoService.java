package com.uade.amicus.service;

import com.uade.amicus.dto.request.ActualizarCantidadRequest;
import com.uade.amicus.dto.request.AgregarItemRequest;
import com.uade.amicus.dto.response.CarritoResponse;
import com.uade.amicus.exception.ConflictoException;
import com.uade.amicus.exception.RecursoNoEncontradoException;
import com.uade.amicus.exception.ReglaDeNegocioException;
import com.uade.amicus.model.Carrito;
import com.uade.amicus.model.CarritoItem;
import com.uade.amicus.model.Frecuencia;
import com.uade.amicus.model.Servicio;
import com.uade.amicus.repository.CarritoItemRepository;
import com.uade.amicus.repository.CarritoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestion del carrito: agregar, modificar, eliminar un item y vaciarlo.
 * Son las cuatro operaciones que pide la consigna.
 *
 * El checkout NO esta aca sino en CheckoutService, porque es una operacion de
 * otra naturaleza: no modifica el carrito, crea una orden y descuenta cupos.
 */
@Service
public class CarritoService {

    private final CarritoRepository carritoRepository;
    private final CarritoItemRepository itemRepository;
    private final ServicioService servicioService;

    public CarritoService(CarritoRepository carritoRepository,
                          CarritoItemRepository itemRepository,
                          ServicioService servicioService) {
        this.carritoRepository = carritoRepository;
        this.itemRepository = itemRepository;
        this.servicioService = servicioService;
    }

    @Transactional(readOnly = true)
    public CarritoResponse ver(Long usuarioId) {
        return CarritoResponse.desde(obtenerEntidad(usuarioId));
    }

    /**
     * Agrega un servicio al carrito.
     *
     * Concentra tres reglas de negocio de la consigna:
     *
     * 1. Si el servicio no tiene cupos, no se puede agregar. Es el caso que la
     *    consigna menciona explicitamente ("en caso de que el producto no tenga
     *    stock... no podra agregarlo al carrito"). Devuelve 409 y no 400 porque
     *    el pedido esta bien formado, lo que falla es el estado del recurso.
     *
     * 2. No se puede pedir mas cantidad que cupos disponibles.
     *
     * 3. Nadie contrata su propio servicio.
     *
     * Si el servicio ya estaba en el carrito se suma cantidad en la linea
     * existente, en vez de duplicarla. La restriccion unica de la base lo
     * garantiza incluso si esta logica fallara.
     */
    @Transactional
    public CarritoResponse agregarItem(Long usuarioId, AgregarItemRequest request) {
        Carrito carrito = obtenerEntidad(usuarioId);
        Servicio servicio = servicioService.obtenerEntidad(request.servicioId());

        if (servicio.getProfesional().getId().equals(usuarioId)) {
            throw new ReglaDeNegocioException("No podes contratar tu propio servicio");
        }
        if (!servicio.tieneCupos()) {
            throw new ConflictoException(
                    "El servicio \"" + servicio.getTitulo() + "\" no tiene cupos disponibles");
        }

        CarritoItem existente = itemRepository
                .findByCarritoIdAndServicioId(carrito.getId(), servicio.getId())
                .orElse(null);

        int cantidadFinal = (existente != null)
                ? existente.getCantidad() + request.cantidad()
                : request.cantidad();

        if (!servicio.tieneCuposPara(cantidadFinal)) {
            throw new ReglaDeNegocioException(
                    "Solo quedan " + servicio.getCuposDisponibles() + " cupos de \""
                            + servicio.getTitulo() + "\"");
        }

        Frecuencia frecuencia = existente != null && request.frecuencia() == null
                ? existente.getFrecuencia()
                : request.frecuenciaOUnica();

        validarRecurrencia(frecuencia, cantidadFinal);

        if (existente != null) {
            existente.setCantidad(cantidadFinal);

            // Si no se envia una nueva frecuencia, se conserva la que ya tenia el item.
            existente.setFrecuencia(frecuencia);
        } else {
            carrito.agregarItem(CarritoItem.builder()
                    .servicio(servicio)
                    .cantidad(request.cantidad())
                    .frecuencia(frecuencia)
                    .build());
        }

        return CarritoResponse.desde(carrito);
    }

    @Transactional
    public CarritoResponse actualizarCantidad(Long usuarioId, Long itemId,
                                              ActualizarCantidadRequest request) {
        Carrito carrito = obtenerEntidad(usuarioId);
        CarritoItem item = buscarItemDelCarrito(carrito, itemId);

        if (!item.getServicio().tieneCuposPara(request.cantidad())) {
            throw new ReglaDeNegocioException(
                    "Solo quedan " + item.getServicio().getCuposDisponibles() + " cupos de \""
                            + item.getServicio().getTitulo() + "\"");
        }

        // Frecuencia nula significa "dejala como estaba".
        Frecuencia frecuencia = request.frecuencia() != null
                ? request.frecuencia()
                : item.getFrecuencia();
        validarRecurrencia(frecuencia, request.cantidad());

        item.setCantidad(request.cantidad());
        item.setFrecuencia(frecuencia);
        return CarritoResponse.desde(carrito);
    }

    /** La consigna pide poder eliminar un item del carrito. */
    @Transactional
    public CarritoResponse eliminarItem(Long usuarioId, Long itemId) {
        Carrito carrito = obtenerEntidad(usuarioId);
        carrito.quitarItem(buscarItemDelCarrito(carrito, itemId));
        return CarritoResponse.desde(carrito);
    }

    /** La consigna pide poder vaciar el carrito. */
    @Transactional
    public CarritoResponse vaciar(Long usuarioId) {
        Carrito carrito = obtenerEntidad(usuarioId);
        carrito.vaciar();
        return CarritoResponse.desde(carrito);
    }

    @Transactional(readOnly = true)
    public Carrito obtenerEntidad(Long usuarioId) {
        return carritoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El usuario con id " + usuarioId + " no tiene carrito"));
    }

    /**
     * Verifica que el item pedido pertenezca al carrito de quien lo pide.
     * Sin esto, un usuario podria borrar items del carrito de otro pasando su id.
     */
    /**
     * Una recurrencia de una sola visita no es una recurrencia.
     *
     * Sin esta validacion se podria guardar "1 visita SEMANAL", que no quiere
     * decir nada: no hay nada que repetir. Para contratar una sola vez existe
     * UNICA, y el estado invalido queda directamente fuera de la base.
     */
    private void validarRecurrencia(Frecuencia frecuencia, int cantidad) {
        if (frecuencia.esRecurrente() && cantidad < 2) {
            throw new ReglaDeNegocioException(
                    "Una contratacion " + frecuencia.name().toLowerCase()
                            + " necesita al menos 2 visitas. Para contratar una sola vez, "
                            + "usa la frecuencia UNICA");
        }
    }

    private CarritoItem buscarItemDelCarrito(Carrito carrito, Long itemId) {
        return carrito.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El item " + itemId + " no esta en el carrito"));
    }
}
