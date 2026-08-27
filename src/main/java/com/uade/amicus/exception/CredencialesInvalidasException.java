package com.uade.amicus.exception;

/**
 * Mail o contrasena incorrectos. Se traduce en un 401.
 */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException(String mensaje) {
        super(mensaje);
    }
}
