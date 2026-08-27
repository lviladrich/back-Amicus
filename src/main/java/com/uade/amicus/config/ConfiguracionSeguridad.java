package com.uade.amicus.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Expone el codificador de contrasenas como bean para que los services puedan
 * pedirlo por inyeccion.
 *
 * Se declara aca y no dentro del service para que haya una sola instancia
 * compartida y para poder cambiar el algoritmo en un unico lugar.
 */
@Configuration
public class ConfiguracionSeguridad {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
