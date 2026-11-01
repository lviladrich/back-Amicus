package com.uade.amicus.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uade.amicus.dto.request.LoginRequest;
import com.uade.amicus.dto.request.RecuperarRequest;
import com.uade.amicus.dto.request.RegistroRequest;
import com.uade.amicus.dto.request.RestablecerRequest;
import com.uade.amicus.service.ServicioMail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integracion de recuperar la contrasena.
 *
 * El mail se reemplaza por un mock: asi el test puede leer el link que se
 * hubiera mandado y recorrer el flujo completo como lo haria el usuario.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RecuperacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ServicioMail servicioMail;

    private String json(Object cuerpo) throws Exception {
        return objectMapper.writeValueAsString(cuerpo);
    }

    private void registrar(String identificador) throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegistroRequest(
                                identificador, identificador + "@amicus.com", "secreta123", "Ana", "Perez"))))
                .andExpect(status().isCreated());
    }

    /** Pide el link y devuelve el token que viajaria en el mail. */
    private String pedirToken(String email) throws Exception {
        clearInvocations(servicioMail);
        mockMvc.perform(post("/api/auth/recuperar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RecuperarRequest(email))))
                .andExpect(status().isNoContent());

        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(servicioMail).enviarRecuperacion(eq(email), link.capture());
        assertThat(link.getValue()).contains("/restablecer?token=");
        return link.getValue().substring(link.getValue().indexOf("token=") + "token=".length());
    }

    private void restablecer(String token, String password, int estadoEsperado) throws Exception {
        mockMvc.perform(post("/api/auth/restablecer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RestablecerRequest(token, password))))
                .andExpect(status().is(estadoEsperado));
    }

    private void login(String email, String password, int estadoEsperado) throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest(email, password))))
                .andExpect(status().is(estadoEsperado));
    }

    @Test
    @DisplayName("Con el token del mail se cambia la contrasena y se puede entrar con la nueva")
    void flujoCompleto() throws Exception {
        registrar("recupera");
        String token = pedirToken("recupera@amicus.com");

        restablecer(token, "nuevaClave456", 204);

        login("recupera@amicus.com", "nuevaClave456", 200);
        login("recupera@amicus.com", "secreta123", 401);
    }

    @Test
    @DisplayName("Un mail que no existe tambien devuelve 204 y no manda nada")
    void mailInexistenteNoRevelaNada() throws Exception {
        clearInvocations(servicioMail);
        mockMvc.perform(post("/api/auth/recuperar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RecuperarRequest("nadie@amicus.com"))))
                .andExpect(status().isNoContent());

        verify(servicioMail, never()).enviarRecuperacion(anyString(), anyString());
    }

    @Test
    @DisplayName("El token sirve una sola vez")
    void tokenUsadoNoSirveDeNuevo() throws Exception {
        registrar("unasola");
        String token = pedirToken("unasola@amicus.com");

        restablecer(token, "nuevaClave456", 204);
        restablecer(token, "otraClave789", 400);
    }

    @Test
    @DisplayName("Pedir un link nuevo anula el anterior")
    void linkNuevoAnulaElAnterior() throws Exception {
        registrar("doslinks");
        String viejo = pedirToken("doslinks@amicus.com");
        String nuevo = pedirToken("doslinks@amicus.com");

        restablecer(viejo, "nuevaClave456", 400);
        restablecer(nuevo, "nuevaClave456", 204);
    }

    @Test
    @DisplayName("Un token inventado devuelve 400 con el formato de error de la API")
    void tokenInvalidoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/restablecer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RestablecerRequest("no-existe", "nuevaClave456"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    @DisplayName("Datos invalidos devuelven 400 con el detalle por campo")
    void datosInvalidos() throws Exception {
        mockMvc.perform(post("/api/auth/recuperar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RecuperarRequest("no-es-un-mail"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.email").exists());

        mockMvc.perform(post("/api/auth/restablecer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RestablecerRequest("", "123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.token").exists())
                .andExpect(jsonPath("$.errores.password").exists());
    }
}
