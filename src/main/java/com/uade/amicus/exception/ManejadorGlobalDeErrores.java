package com.uade.amicus.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Traduce excepciones a respuestas HTTP, en un solo lugar para toda la API.
 *
 * Sin esta clase, cada metodo de cada controller necesitaria su propio
 * try/catch para decidir que codigo devolver. Con ella, los services lanzan la
 * excepcion que corresponde y se despreocupan del HTTP, que es justamente lo que
 * pide la separacion en capas.
 *
 * @RestControllerAdvice intercepta las excepciones de todos los controllers.
 */
@RestControllerAdvice
public class ManejadorGlobalDeErrores {

    /** 404: se pidio algo que no existe. */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<RespuestaError> noEncontrado(RecursoNoEncontradoException ex,
                                                       HttpServletRequest request) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    /** 400: el pedido rompe una regla del negocio. */
    @ExceptionHandler(ReglaDeNegocioException.class)
    public ResponseEntity<RespuestaError> reglaDeNegocio(ReglaDeNegocioException ex,
                                                         HttpServletRequest request) {
        return construir(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    /** 409: el pedido choca con el estado actual. Caso tipico: sin cupos. */
    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<RespuestaError> conflicto(ConflictoException ex,
                                                    HttpServletRequest request) {
        return construir(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    /** 401: mail o contrasena incorrectos. */
    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<RespuestaError> credenciales(CredencialesInvalidasException ex,
                                                       HttpServletRequest request) {
        return construir(HttpStatus.UNAUTHORIZED, ex.getMessage(), request, null);
    }

    /** 403: esta identificado pero el recurso no es suyo. */
    @ExceptionHandler(OperacionNoPermitidaException.class)
    public ResponseEntity<RespuestaError> noPermitido(OperacionNoPermitidaException ex,
                                                      HttpServletRequest request) {
        return construir(HttpStatus.FORBIDDEN, ex.getMessage(), request, null);
    }

    /**
     * 400 por validacion: se dispara cuando un DTO anotado con @Valid no cumple
     * sus restricciones. Devuelve que campo fallo y por que.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespuestaError> validacion(MethodArgumentNotValidException ex,
                                                     HttpServletRequest request) {
        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errores.put(error.getField(), error.getDefaultMessage()));

        return construir(HttpStatus.BAD_REQUEST,
                "Hay campos invalidos en el pedido", request, errores);
    }

    /**
     * 500: cualquier cosa no prevista.
     *
     * Existe para que un error inesperado no devuelva la traza de Java al
     * cliente, que ademas de ser feo expone la estructura interna del sistema.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespuestaError> inesperado(Exception ex,
                                                     HttpServletRequest request) {
        return construir(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrio un error inesperado: " + ex.getMessage(), request, null);
    }

    private ResponseEntity<RespuestaError> construir(HttpStatus estado, String mensaje,
                                                     HttpServletRequest request,
                                                     Map<String, String> errores) {
        RespuestaError cuerpo = RespuestaError.builder()
                .timestamp(LocalDateTime.now())
                .estado(estado.value())
                .error(estado.getReasonPhrase())
                .mensaje(mensaje)
                .ruta(request.getRequestURI())
                .errores(errores)
                .build();

        return ResponseEntity.status(estado).body(cuerpo);
    }
}
