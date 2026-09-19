package com.uade.amicus.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

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
     * - csrf deshabilitado: la proteccion CSRF existe para formularios HTML con
     *   sesion y cookies. Una API que recibe JSON desde Postman o desde un
     *   front en React no usa ese mecanismo, y con CSRF activo todo POST sin
     *   token devolveria 403.
     *
     * - sesion STATELESS: el servidor no guarda estado entre pedidos. Cada
     *   request tiene que traer su propia identidad. Por eso el metodo de
     *   autenticacion es httpBasic: manda mail y contrasena en cada pedido, sin
     *   necesitar una sesion ni un token todavia.
     *
     * - reglas de acceso: crear, modificar y borrar categorias o zonas requiere
     *   el rol ADMIN, porque son datos del catalogo del sistema, no de un
     *   usuario en particular. Listarlas sigue siendo publico, y el resto de la
     *   API (servicios, carrito, ordenes, resenas) no cambia: sigue manejando
     *   sus propios permisos con el usuarioId, como hasta ahora.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/categorias/**", "/api/zonas/**").permitAll()
                        .requestMatchers("/api/categorias/**", "/api/zonas/**").hasRole("ADMIN")
                        .anyRequest().permitAll())
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
