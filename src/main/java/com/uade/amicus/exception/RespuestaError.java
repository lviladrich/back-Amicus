package com.uade.amicus.exception;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Formato unico de error para toda la API.
 *
 * Que todos los errores tengan la misma forma le permite al front (y a quien
 * pruebe con Postman) manejarlos siempre igual, en vez de adivinar el formato
 * segun el endpoint.
 */
@Getter
@Builder
public class RespuestaError {

    private LocalDateTime timestamp;
    private int estado;
    private String error;
    private String mensaje;
    private String ruta;

    /** Solo se completa cuando falla la validacion: campo a campo, que estuvo mal. */
    private Map<String, String> errores;
}
