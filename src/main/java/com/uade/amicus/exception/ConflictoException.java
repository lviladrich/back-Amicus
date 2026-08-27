package com.uade.amicus.exception;

/**
 * El pedido choca con el estado actual del sistema.
 *
 * El caso central del TPO: agregar al carrito un servicio sin cupos. No es un
 * error del cliente (el pedido esta bien formado), es que el recurso no esta
 * disponible en este momento. Por eso 409 y no 400.
 */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
