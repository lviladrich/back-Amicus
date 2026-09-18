package com.uade.amicus.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
     * Cadena de filtros para una API REST.
     *
     * - csrf deshabilitado: la proteccion CSRF existe para formularios HTML con
     *   sesion y cookies. Una API que recibe JSON desde Postman o desde un
     *   front en React no usa ese mecanismo, y con CSRF activo todo POST sin
     *   token devolveria 403.
     *
     * - sesion STATELESS: el servidor no guarda estado entre pedidos. Cada
     *   request tiene que traer su propia identidad (mas adelante, un token
     *   JWT). Es la base para que el back pueda escalar y para que el front no
     *   dependa de cookies.
     *
     * - permitAll: por ahora ningun endpoint exige autenticacion. Esta es la
     *   linea que se reemplaza por reglas de acceso (por ejemplo, que solo un
     *   ADMIN pueda borrar categorias) cuando se incorpore el filtro JWT.
     *   Hasta entonces la API se comporta igual que antes de sumar Spring
     *   Security, pero con UserDetails y roles ya definidos.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }
}
