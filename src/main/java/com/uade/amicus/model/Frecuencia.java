package com.uade.amicus.model;

/**
 * Cada cuanto se repite una visita contratada.
 *
 * Es lo que diferencia a un marketplace de servicios de uno de productos: no se
 * compra "una unidad" de un electricista, se contrata una visita, y muchas veces
 * se la quiere repetir. El caso tipico es la limpieza semanal.
 *
 * La cantidad de visitas sigue siendo el campo cantidad de CarritoItem: la
 * frecuencia solo dice cada cuanto se repiten. "8 visitas SEMANAL" es un
 * contrato de dos meses, y consume 8 cupos, porque un cupo es una visita que el
 * profesional se compromete a tomar.
 */
public enum Frecuencia {
    UNICA,
    SEMANAL,
    QUINCENAL,
    MENSUAL;

    /** Una sola visita no es una recurrencia, aunque sea un valor valido. */
    public boolean esRecurrente() {
        return this != UNICA;
    }
}
