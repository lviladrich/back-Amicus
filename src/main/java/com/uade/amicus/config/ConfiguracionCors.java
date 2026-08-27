package com.uade.amicus.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Permite que el front consuma esta API desde otro origen.
 *
 * Por defecto el navegador bloquea las peticiones entre origenes distintos
 * (politica del mismo origen). Como el front de React va a correr en el puerto
 * 5173 o 3000 y la API en el 8080, sin esta configuracion el navegador
 * rechazaria todas las respuestas.
 *
 * No hace falta para probar con Postman, que no aplica esa politica, pero si
 * para la entrega siguiente.
 */
@Configuration
public class ConfiguracionCors implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registro) {
        registro.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000", "http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE")
                .allowedHeaders("*");
    }
}
