package com.uade.amicus.config;

import com.uade.amicus.security.FiltroJwt;
import com.uade.amicus.security.PuenteDeErroresDeSeguridad;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ContentSecurityPolicyHeaderWriter;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Configuracion de Spring Security.
 *
 * Dos beans:
 *
 * 1. PasswordEncoder: el codificador de contrasenas, unico para toda la app.
 *    Se declara aca y no dentro del service para que haya una sola instancia
 *    compartida y para poder cambiar el algoritmo en un unico lugar.
 *
 * 2. SecurityFilterChain: la cadena de filtros que Spring Security interpone
 *    delante de todos los controllers. Sin este bean, el starter de seguridad
 *    aplica su configuracion por defecto: login por formulario, contrasena
 *    aleatoria en la consola y todos los endpoints bloqueados.
 */
@Configuration
@EnableWebSecurity
public class ConfiguracionSeguridad {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * El objeto que sabe autenticar: recibe mail y contrasena, busca el
     * usuario con UsuarioDetailsService y compara con PasswordEncoder.
     *
     * Sin exponerlo como bean, UsuarioService no tiene forma de pedirle a
     * Spring Security que autentique y termina reimplementando a mano lo que
     * esta clase ya resuelve.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Cadena de filtros para una API REST.
     *
     * - csrf deshabilitado, y esta vez con un motivo concreto. Un ataque CSRF
     *   funciona porque el navegador adjunta solo las credenciales que guarda
     *   el: la cookie de sesion. Un sitio hostil puede entonces disparar un
     *   POST contra esta API y viajaria autenticado sin que el usuario se
     *   entere. Aca no hay cookie de sesion (la politica es STATELESS) y el
     *   token JWT viaja en la cabecera Authorization, que el navegador NUNCA
     *   completa solo: la tiene que escribir el JavaScript del front, y la
     *   politica de mismo origen impide que un sitio ajeno lea el token para
     *   hacerlo. Sin credencial automatica no hay CSRF, y por eso la defensa
     *   sobra. La contracara es que el front NO debe guardar el token en una
     *   cookie: ahi si volveria el problema y habria que reactivar csrf.
     *
     * - sesion STATELESS: el servidor no guarda estado entre pedidos. Cada
     *   request trae su propia identidad, ahora en el token JWT que emite
     *   /api/auth/login. Se mantiene httpBasic como segunda via porque los
     *   scripts de prueba del catalogo lo usan.
     *
     * - reglas de acceso: crear, modificar y borrar categorias o zonas requiere
     *   el rol ADMIN, porque son datos del catalogo del sistema, no de un
     *   usuario en particular. Listarlas sigue siendo publico, y el resto de la
     *   API (servicios, carrito, ordenes, resenas) no cambia: sigue manejando
     *   sus propios permisos con el usuarioId, como hasta ahora.
     *
     * - errores: cuando un filtro rechaza el pedido, el 401 y el 403 se
     *   entregan al puente para que salgan como RespuestaError, igual que el
     *   resto de la API. El puente se declara dos veces porque httpBasic trae
     *   su propio entry point para las credenciales incorrectas, distinto del
     *   que se usa cuando directamente no hay credenciales.
     *
     * - cabeceras: defensa en profundidad contra XSS. Una API que devuelve
     *   JSON no es por si misma un vector (Jackson escapa todo lo que
     *   serializa, asi que un titulo con <script> vuelve como texto y no como
     *   etiqueta), pero estas cabeceras acotan el dano si alguna respuesta
     *   termina interpretandose como HTML. Ver el metodo cabeceras() abajo.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   PuenteDeErroresDeSeguridad puente,
                                                   FiltroJwt filtroJwt) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .headers(ConfiguracionSeguridad::cabeceras)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/categorias/**", "/api/zonas/**").permitAll()
                        .requestMatchers("/api/categorias/**", "/api/zonas/**").hasRole("ADMIN")
                        .anyRequest().permitAll())
                .httpBasic(basic -> basic.authenticationEntryPoint(puente))
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(puente)
                        .accessDeniedHandler(puente))
                // Antes del filtro de usuario y contrasena: si el pedido trae
                // un token valido, queda autenticado y httpBasic ni se entera.
                .addFilterBefore(filtroJwt, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Cabeceras de seguridad de las respuestas.
     *
     * Spring Security ya manda por su cuenta X-Content-Type-Options: nosniff
     * (impide que el navegador adivine que una respuesta JSON es en realidad
     * HTML y la ejecute), X-Frame-Options: DENY y Cache-Control sin guardado.
     * Se agregan las dos que no trae:
     *
     * - Content-Security-Policy: le prohibe al navegador ejecutar scripts o
     *   cargar recursos a partir de una respuesta de la API. Si un dato con
     *   <script> adentro llegara a interpretarse como HTML, el script no
     *   corre. frame-ancestors repite en CSP lo que X-Frame-Options dice en
     *   viejo, contra clickjacking.
     *
     * - Referrer-Policy: evita que al seguir un link se filtre a un sitio
     *   ajeno la URL de la que se venia, que en una API suele llevar ids.
     *
     * La politica se restringe a /api/**: Swagger es una pagina HTML de verdad
     * y con esta CSP no cargaria ni su propio CSS.
     */
    private static void cabeceras(HeadersConfigurer<HttpSecurity> headers) {
        RequestMatcher soloApi = PathPatternRequestMatcher.withDefaults().matcher("/api/**");

        headers
                .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(soloApi,
                        new ContentSecurityPolicyHeaderWriter(
                                "default-src 'none'; frame-ancestors 'none'; sandbox")))
                .referrerPolicy(ref -> ref.policy(
                        ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER));
    }
}
