package com.uade.amicus.exception;

/**
 * Se pidio algo que no existe. El manejador global la traduce en un 404.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }

    /** Atajo para el caso mas comun: "Servicio con id 7 no encontrado". */
    public static RecursoNoEncontradoException de(String entidad, Long id) {
        return new RecursoNoEncontradoException(entidad + " con id " + id + " no encontrado");
    }
}
