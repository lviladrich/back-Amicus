package com.uade.amicus.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Traduce el token JWT de la cabecera Authorization en un usuario autenticado.
 *
 * Corre una vez por pedido, antes de los filtros de autorizacion. Si encuentra
 * un token valido deja el usuario cargado en el SecurityContext, que es de
 * donde Spring Security lee despues para decidir si el pedido pasa.
 *
 * Si no hay cabecera, no hace nada: el pedido sigue como anonimo y son las
 * reglas de la cadena las que deciden si eso alcanza. Asi los endpoints
 * publicos siguen funcionando sin token.
 */
@Component
public class FiltroJwt extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";

    private final ServicioJwt servicioJwt;
    private final UsuarioDetailsService usuarioDetailsService;
    private final PuenteDeErroresDeSeguridad puente;

    public FiltroJwt(ServicioJwt servicioJwt,
                     UsuarioDetailsService usuarioDetailsService,
                     PuenteDeErroresDeSeguridad puente) {
        this.servicioJwt = servicioJwt;
        this.usuarioDetailsService = usuarioDetailsService;
        this.puente = puente;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String cabecera = request.getHeader(HttpHeaders.AUTHORIZATION);

        // Sin token, o ya autenticado por otra via (httpBasic), no hay nada que hacer.
        if (cabecera == null || !cabecera.startsWith(PREFIJO)
                || SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(request, response);
            return;
        }

        try {
            // El rol se relee de la base y no se toma del token: si el usuario
            // fue dado de baja o cambio de rol, el cambio pega de inmediato.
            UserDetails usuario = usuarioDetailsService.loadUserByUsername(
                    servicioJwt.mailDe(cabecera.substring(PREFIJO.length())));

            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            usuario, null, usuario.getAuthorities()));

        } catch (JwtException | UsernameNotFoundException ex) {
            // Token con la firma cambiada, vencido, o de un usuario que ya no
            // existe. Se corta aca con un 401 con el formato del resto de la
            // API, en vez de dejar seguir el pedido como anonimo: el cliente
            // mando una credencial y merece saber que no sirve.
            SecurityContextHolder.clearContext();
            puente.commence(request, response,
                    new BadCredentialsException("El token es invalido o expiro"));
            return;
        }

        chain.doFilter(request, response);
    }
}
