package com.uade.amicus.exception;

/**
 * El usuario esta identificado pero no tiene permiso sobre ese recurso.
 *
 * Caso tipico: intentar modificar o dar de baja la publicacion de otro.
 *
 * Se traduce en un 403 FORBIDDEN y no en un 401. La diferencia importa:
 * 401 significa "no se quien sos", 403 significa "se quien sos y no podes
 * hacer esto".
 */
public class OperacionNoPermitidaException extends RuntimeException {

    public OperacionNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}
