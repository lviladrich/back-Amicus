package com.uade.amicus.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Lleva los errores de seguridad hasta ManejadorGlobalDeErrores.
 *
 * Un @RestControllerAdvice solo ve las excepciones que se lanzan desde un
 * controller para adentro. Los filtros de Spring Security corren ANTES: cuando
 * rechazan un pedido (sin credenciales, o sin el rol necesario), el pedido nunca
 * llega al controller, el manejador global no se entera y la respuesta sale con
 * el formato por defecto de Spring y no como RespuestaError.
 *
 * Spring Security deja elegir que hacer en esos dos casos:
 *
 * - AuthenticationEntryPoint: no se quien sos (401).
 * - AccessDeniedHandler: se quien sos, y no te alcanza el rol (403).
 *
 * Esta clase implementa las dos interfaces y en ambos casos hace lo mismo: le
 * entrega la excepcion al HandlerExceptionResolver, que es la pieza de Spring MVC
 * que busca el @ExceptionHandler correspondiente. Asi el formato del error se
 * sigue decidiendo en un unico lugar, en vez de armar aca un segundo JSON a mano
 * que haya que mantener igual al primero.
 */
@Component
public class PuenteDeErroresDeSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final HandlerExceptionResolver resolver;

    /**
     * Spring registra mas de un HandlerExceptionResolver. El que recorre los
     * @ExceptionHandler se llama "handlerExceptionResolver", y el @Qualifier
     * pide ese por nombre.
     */
    public PuenteDeErroresDeSeguridad(
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) {
        resolver.resolveException(request, response, null, ex);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) {
        resolver.resolveException(request, response, null, ex);
    }
}
