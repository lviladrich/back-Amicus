package com.uade.amicus.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Arma un Pageable a partir de page/size, acotando valores fuera de rango en
 * vez de rechazarlos con un error: page negativa se lleva a 0, y un size
 * invalido o excesivo cae al valor por defecto.
 */
public final class Paginacion {

    public static final int TAMANIO_DEFECTO = 10;
    public static final int TAMANIO_MAXIMO = 50;

    private Paginacion() {
    }

    public static Pageable de(int pagina, int tamanio) {
        return de(pagina, tamanio, Sort.unsorted());
    }

    /** Igual que el anterior, pero con un orden. */
    public static Pageable de(int pagina, int tamanio, Sort orden) {
        int paginaSegura = Math.max(pagina, 0);
        int tamanioSeguro = (tamanio < 1 || tamanio > TAMANIO_MAXIMO) ? TAMANIO_DEFECTO : tamanio;
        return PageRequest.of(paginaSegura, tamanioSeguro, orden);
    }
}
