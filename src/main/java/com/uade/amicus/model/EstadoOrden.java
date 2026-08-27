package com.uade.amicus.model;

/**
 * Estados posibles de una orden.
 *
 * Es un enum y no un String suelto porque el compilador no deja escribir un
 * estado que no exista. Con texto libre, un "CONFIRMDA" mal tipeado se guarda
 * sin que nadie se entere.
 */
public enum EstadoOrden {
    CONFIRMADA,
    CANCELADA
}
