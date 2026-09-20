package com.uade.amicus.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uade.amicus.dto.request.RegistroRequest;
import com.uade.amicus.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integracion del registro.
 *
 * Se eligio @SpringBootTest con MockMvc y no un test unitario del service con
 * mocks: lo que interesa verificar aca es justamente lo que un mock daria por
 * sentado. Que @Valid se dispare, que el manejador global traduzca la
 * excepcion al codigo correcto, que el DTO de salida no incluya la contrasena
 * y que Spring Security deje pasar el endpoint son propiedades del conjunto,
 * no del service aislado.
 *
 * MockMvc recorre la cadena real de filtros y controllers sin levantar Tomcat
 * ni abrir un puerto, asi que el test corre rapido y no depende de la red. La
 * base es H2 en memoria (ver src/test/resources/application.properties).
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Cada test arma su propio mail a partir del nombre del caso. El contexto
     * de Spring se comparte entre los tests de la clase y la base no se limpia
     * entre uno y otro, asi que un mail fijo haria que el segundo test que lo
     * use choque contra el usuario que dejo el primero.
     */
    private RegistroRequest registroDe(String identificador) {
        return new RegistroRequest(
                identificador,
                identificador + "@amicus.com",
                "secreta123",
                "Ana",
                "Perez");
    }

    private String json(Object cuerpo) throws Exception {
        return objectMapper.writeValueAsString(cuerpo);
    }

    @Test
    @DisplayName("POST /api/auth/registro devuelve 201 con los datos del usuario creado")
    void registroExitoso() throws Exception {
        RegistroRequest request = registroDe("anaperez");

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value("anaperez"))
                .andExpect(jsonPath("$.email").value("anaperez@amicus.com"))
                // El rol lo asigna el sistema: nadie puede registrarse como ADMIN.
                .andExpect(jsonPath("$.rol").value("USUARIO"));

        assertThat(usuarioRepository.existsByEmail("anaperez@amicus.com")).isTrue();
    }

    /**
     * La razon de ser de los DTOs. Si el controller devolviera la entidad
     * Usuario, el hash de la contrasena viajaria en cada respuesta.
     */
    @Test
    @DisplayName("La respuesta del registro nunca incluye la contrasena")
    void registroNoDevuelveLaContrasena() throws Exception {
        String respuesta = mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(registroDe("sinpassword"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(respuesta).doesNotContain("secreta123");
    }

    /**
     * Y que ademas quede hasheada en la base: no alcanza con ocultarla en la
     * respuesta si se guardo en texto plano.
     */
    @Test
    @DisplayName("La contrasena se guarda hasheada con BCrypt, nunca en texto plano")
    void contrasenaSeGuardaHasheada() throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(registroDe("conhash"))))
                .andExpect(status().isCreated());

        String guardada = usuarioRepository.findByEmail("conhash@amicus.com")
                .orElseThrow()
                .getPassword();

        assertThat(guardada).isNotEqualTo("secreta123");
        // Prefijo del algoritmo que usa BCryptPasswordEncoder.
        assertThat(guardada).startsWith("$2a$");
    }

    @Test
    @DisplayName("Registrar un mail ya existente devuelve 400 y no crea un segundo usuario")
    void registroConMailRepetido() throws Exception {
        RegistroRequest primero = registroDe("repetido");

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(primero)))
                .andExpect(status().isCreated());

        // Mismo mail, distinto username: lo que choca es el mail.
        RegistroRequest segundo = new RegistroRequest(
                "otrousuario", primero.email(), "otraclave123", "Juan", "Gomez");

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(segundo)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString(primero.email())));

        assertThat(usuarioRepository.existsByUsername("otrousuario")).isFalse();
    }

    /**
     * Verifica que @Valid se dispare y que el manejador global arme el mapa de
     * errores campo por campo, en vez de devolver un 500 con la traza.
     */
    @Test
    @DisplayName("Un registro con campos invalidos devuelve 400 con el detalle por campo")
    void registroConCamposInvalidos() throws Exception {
        RegistroRequest invalido = new RegistroRequest(
                "ab",                 // username: minimo 3 caracteres
                "no-es-un-mail",      // email: formato invalido
                "123",                // password: minimo 6 caracteres
                "",                   // nombre: obligatorio
                "Perez");

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(invalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.username").exists())
                .andExpect(jsonPath("$.errores.email").exists())
                .andExpect(jsonPath("$.errores.password").exists())
                .andExpect(jsonPath("$.errores.nombre").exists());
    }

    @Test
    @DisplayName("Pedir un usuario inexistente devuelve 404 con el formato de error de la API")
    void usuarioInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/auth/usuarios/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.estado").value(404))
                .andExpect(jsonPath("$.ruta").value("/api/auth/usuarios/999999"));
    }

    /**
     * El caso que motivo el manejador de ConstraintViolationException: una
     * restriccion sobre un parametro suelto del controller, no sobre un DTO.
     * Antes terminaba en 500.
     */
    @Test
    @DisplayName("Un parametro que viola @Positive devuelve 400 y no 500")
    void parametroInvalidoDevuelve400() throws Exception {
        mockMvc.perform(get("/api/carrito").param("usuarioId", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.errores.usuarioId").exists());
    }
}
