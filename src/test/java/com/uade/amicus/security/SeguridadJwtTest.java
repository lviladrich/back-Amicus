package com.uade.amicus.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uade.amicus.dto.request.LoginRequest;
import com.uade.amicus.dto.request.RegistroRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Login con JWT y cabeceras de seguridad.
 *
 * Se prueba contra el endpoint de ABM de zonas, que es el unico que hoy exige
 * rol ADMIN: es donde se puede ver si el token sirve de verdad para autorizar
 * y no solo si se emite bien.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SeguridadJwtTest {

    private static final String ADMIN_MAIL = "admin@amicus.com";
    private static final String ADMIN_PASS = "admin123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String tokenDeAdmin;

    @BeforeEach
    void obtenerTokenDeAdmin() throws Exception {
        tokenDeAdmin = loguear(ADMIN_MAIL, ADMIN_PASS).get("token").asText();
    }

    private JsonNode loguear(String mail, String password) throws Exception {
        String cuerpo = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(mail, password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(cuerpo);
    }

    @Test
    @DisplayName("El login devuelve un token JWT con los datos del usuario")
    void loginDevuelveToken() throws Exception {
        JsonNode respuesta = loguear(ADMIN_MAIL, ADMIN_PASS);

        assertThat(respuesta.get("tipo").asText()).isEqualTo("Bearer");
        assertThat(respuesta.get("expiraEnSegundos").asLong()).isPositive();
        assertThat(respuesta.get("usuario").get("email").asText()).isEqualTo(ADMIN_MAIL);
        // Un JWT son tres partes separadas por punto: cabecera, datos y firma.
        assertThat(respuesta.get("token").asText().split("\\.")).hasSize(3);
        // El token no debe llevar la contrasena adentro: el contenido viaja legible.
        assertThat(respuesta.get("token").asText()).doesNotContain(ADMIN_PASS);
    }

    @Test
    @DisplayName("Un endpoint de ADMIN acepta el token y rechaza al que no lo trae")
    void tokenAutorizaEndpointDeAdmin() throws Exception {
        String zona = """
                {"nombre":"Zona creada con token"}""";

        mockMvc.perform(post("/api/zonas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDeAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(zona))
                .andExpect(status().isCreated());

        // Sin credenciales, el mismo pedido no pasa.
        mockMvc.perform(post("/api/zonas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Zona sin token"}"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("El token de un usuario comun no alcanza para un endpoint de ADMIN")
    void tokenDeUsuarioComunNoAlcanza() throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegistroRequest(
                                "comun", "comun@amicus.com", "secreta123", "Ana", "Perez"))))
                .andExpect(status().isCreated());

        String token = loguear("comun@amicus.com", "secreta123").get("token").asText();

        mockMvc.perform(post("/api/zonas")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Zona prohibida"}"""))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.estado").value(403));
    }

    /**
     * El caso que justifica verificar la firma: si alcanzara con que el token
     * tenga la forma correcta, cualquiera se haria pasar por admin editando el
     * contenido, que viaja legible.
     */
    @Test
    @DisplayName("Un token con la firma alterada devuelve 401 con el formato de la API")
    void tokenAlteradoDevuelve401() throws Exception {
        String alterado = tokenDeAdmin.substring(0, tokenDeAdmin.length() - 4) + "AAAA";

        mockMvc.perform(get("/api/ordenes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + alterado)
                        .param("usuarioId", "1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401))
                .andExpect(jsonPath("$.mensaje").value("El token es invalido o expiro"));
    }

    @Test
    @DisplayName("Las respuestas de la API traen las cabeceras contra XSS y clickjacking")
    void cabecerasDeSeguridad() throws Exception {
        mockMvc.perform(get("/api/zonas"))
                .andExpect(status().isOk())
                // Impide que el navegador reinterprete el JSON como HTML.
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("Content-Security-Policy",
                        "default-src 'none'; frame-ancestors 'none'; sandbox"));
    }

    /**
     * La CSP estricta vale para la API, no para Swagger: es una pagina HTML de
     * verdad y con "default-src 'none'" no cargaria ni su propio CSS.
     */
    @Test
    @DisplayName("Swagger no recibe la CSP estricta de la API")
    void swaggerSinCspEstricta() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(header().doesNotExist("Content-Security-Policy"));
    }
}
