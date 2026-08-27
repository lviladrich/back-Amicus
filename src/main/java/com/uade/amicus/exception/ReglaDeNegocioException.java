package com.uade.amicus.exception;

/**
 * El pedido es sintacticamente valido pero rompe una regla del negocio.
 * Por ejemplo: contratar el propio servicio. Se traduce en un 400.
 */
public class ReglaDeNegocioException extends RuntimeException {

    public ReglaDeNegocioException(String mensaje) {
        super(mensaje);
    }
}
