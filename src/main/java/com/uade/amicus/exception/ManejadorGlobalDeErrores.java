package com.uade.amicus.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

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
 * Extiende ResponseEntityExceptionHandler, una clase de Spring que ya sabe
 * traducir sus propias excepciones al codigo correcto: 405 si el verbo no esta
 * soportado, 415 si falta el Content-Type, 400 si el JSON esta mal formado.
 *
 * Eso importa: sin heredar de ella, el manejador de Exception de mas abajo
 * atrapaba todas esas excepciones y las convertia en un 500, cuando en realidad
 * son errores del cliente y ya tenian su codigo correcto asignado.
 *
 * Lo unico que agregamos sobre ese comportamiento es el formato: todas las
 * respuestas de error, propias o de Spring, salen como RespuestaError.
 */
@RestControllerAdvice
public class ManejadorGlobalDeErrores extends ResponseEntityExceptionHandler {

    // ------------------------------------------------------------------
    // Excepciones propias del dominio
    // ------------------------------------------------------------------

    /** 404: se pidio algo que no existe. */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<RespuestaError> noEncontrado(RecursoNoEncontradoException ex,
                                                       HttpServletRequest request) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), null);
    }

    /** 400: el pedido rompe una regla del negocio. */
    @ExceptionHandler(ReglaDeNegocioException.class)
    public ResponseEntity<RespuestaError> reglaDeNegocio(ReglaDeNegocioException ex,
                                                         HttpServletRequest request) {
        return construir(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), null);
    }

    /** 409: el pedido choca con el estado actual. Caso tipico: sin cupos. */
    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<RespuestaError> conflicto(ConflictoException ex,
                                                    HttpServletRequest request) {
        return construir(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), null);
    }

    /** 401: mail o contrasena incorrectos. No se quien sos. */
    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<RespuestaError> credenciales(CredencialesInvalidasException ex,
                                                       HttpServletRequest request) {
        return construir(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getRequestURI(), null);
    }

    /** 403: se quien sos, y este recurso no es tuyo. */
    @ExceptionHandler(OperacionNoPermitidaException.class)
    public ResponseEntity<RespuestaError> noPermitido(OperacionNoPermitidaException ex,
                                                      HttpServletRequest request) {
        return construir(HttpStatus.FORBIDDEN, ex.getMessage(), request.getRequestURI(), null);
    }

    /**
     * 400: un parametro de la ruta o la query no se puede convertir al tipo
     * esperado. Por ejemplo /api/servicios/abc, donde se espera un numero.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<RespuestaError> tipoInvalido(MethodArgumentTypeMismatchException ex,
                                                       HttpServletRequest request) {
        String tipo = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "el tipo esperado";
        return construir(HttpStatus.BAD_REQUEST,
                "El parametro '" + ex.getName() + "' con valor '" + ex.getValue()
                        + "' no se puede convertir a " + tipo,
                request.getRequestURI(), null);
    }

    /**
     * 500: cualquier cosa realmente no prevista.
     *
     * Existe para que un error inesperado no devuelva la traza de Java al
     * cliente, que ademas de ser ilegible expone la estructura interna del
     * sistema.
     *
     * Ojo: este manejador es el ultimo recurso. Todo lo que Spring ya sabe
     * traducir lo resuelve la clase padre antes de llegar aca.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespuestaError> inesperado(Exception ex,
                                                     HttpServletRequest request) {
        return construir(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrio un error inesperado: " + ex.getMessage(),
                request.getRequestURI(), null);
    }

    // ------------------------------------------------------------------
    // Excepciones de Spring, con el formato de la API
    // ------------------------------------------------------------------

    /**
     * 400 por validacion: se dispara cuando un DTO anotado con @Valid no cumple
     * sus restricciones. Devuelve que campo fallo y por que.
     *
     * Se sobrescribe el metodo de la clase padre en vez de declarar un
     * @ExceptionHandler propio, porque el padre ya reclama esta excepcion y
     * tener las dos cosas seria ambiguo.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {

        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errores.put(error.getField(), error.getDefaultMessage()));

        RespuestaError cuerpo = cuerpoDeError(HttpStatus.BAD_REQUEST,
                "Hay campos invalidos en el pedido", rutaDe(request), errores);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(cuerpo);
    }

    /**
     * Da formato de RespuestaError a todo lo que maneja la clase padre: 405
     * cuando el verbo no esta soportado, 415 cuando falta el Content-Type, 400
     * cuando el JSON esta mal formado o falta un parametro obligatorio, 404
     * cuando la ruta no existe.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, @Nullable Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {

        HttpStatus estado = HttpStatus.valueOf(status.value());
        RespuestaError cuerpo = cuerpoDeError(estado, mensajeLegible(ex, estado),
                rutaDe(request), null);

        return ResponseEntity.status(status).headers(headers).body(cuerpo);
    }

    /** Traduce los mensajes tecnicos de Spring a algo entendible. */
    private String mensajeLegible(Exception ex, HttpStatus estado) {
        return switch (estado) {
            case METHOD_NOT_ALLOWED -> "El metodo HTTP no esta permitido en esta ruta";
            case UNSUPPORTED_MEDIA_TYPE -> "Falta la cabecera Content-Type: application/json";
            case NOT_FOUND -> "La ruta solicitada no existe";
            case BAD_REQUEST -> {
                String m = ex.getMessage();
                if (m != null && m.contains("JSON parse error")) {
                    yield "El cuerpo del pedido no es un JSON valido";
                }
                if (m != null && m.contains("Required request parameter")) {
                    yield "Falta un parametro obligatorio: " + m.replaceAll(".*parameter '([^']+)'.*", "$1");
                }
                yield m;
            }
            default -> ex.getMessage();
        };
    }

    // ------------------------------------------------------------------

    private ResponseEntity<RespuestaError> construir(HttpStatus estado, String mensaje,
                                                     String ruta,
                                                     Map<String, String> errores) {
        return ResponseEntity.status(estado).body(cuerpoDeError(estado, mensaje, ruta, errores));
    }

    private RespuestaError cuerpoDeError(HttpStatus estado, String mensaje, String ruta,
                                         Map<String, String> errores) {
        return RespuestaError.builder()
                .timestamp(LocalDateTime.now())
                .estado(estado.value())
                .error(estado.getReasonPhrase())
                .mensaje(mensaje)
                .ruta(ruta)
                .errores(errores)
                .build();
    }

    private String rutaDe(WebRequest request) {
        if (request instanceof ServletWebRequest servletRequest) {
            return servletRequest.getRequest().getRequestURI();
        }
        return request.getDescription(false).replace("uri=", "");
    }
}
