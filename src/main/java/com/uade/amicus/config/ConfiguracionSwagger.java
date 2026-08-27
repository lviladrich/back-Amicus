package com.uade.amicus.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Documentacion interactiva de la API.
 *
 * springdoc recorre los @RestController al arrancar y genera una pagina web con
 * todos los endpoints, sus parametros, los cuerpos que esperan y los que
 * devuelven. Cada uno trae un boton "Try it out" para ejecutarlo desde el
 * navegador.
 *
 * Disponible en:  http://localhost:8080/swagger-ui.html
 *
 * Esta clase solo agrega los datos de portada. La documentacion en si se genera
 * sola a partir del codigo, que es la gracia: no puede quedar desactualizada
 * respecto de lo que la API realmente hace.
 */
@Configuration
public class ConfiguracionSwagger {

    @Bean
    public OpenAPI configurarOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Amicus API")
                        .version("1.0")
                        .description("""
                                Marketplace de servicios del hogar. Un usuario publica servicios \
                                con cupos disponibles y otro los contrata mediante un carrito y un \
                                checkout que descuenta esos cupos.

                                Trabajo Practico Obligatorio de Aplicaciones Interactivas, UADE, \
                                segundo cuatrimestre 2026.

                                Los endpoints de carrito y ordenes requieren el parametro \
                                usuarioId. En un sistema real ese dato saldria de un token JWT.""")
                        .contact(new Contact().name("Equipo Amicus")))
                .servers(List.of(new Server()
                        .url("http://localhost:8080")
                        .description("Entorno local")));
    }
}
