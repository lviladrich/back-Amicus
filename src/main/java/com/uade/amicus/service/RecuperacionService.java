package com.uade.amicus.service;

import com.uade.amicus.dto.request.RecuperarRequest;
import com.uade.amicus.dto.request.RestablecerRequest;
import com.uade.amicus.exception.ReglaDeNegocioException;
import com.uade.amicus.model.TokenRecuperacion;
import com.uade.amicus.model.Usuario;
import com.uade.amicus.repository.TokenRecuperacionRepository;
import com.uade.amicus.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Recuperar la contrasena en dos pasos:
 *
 * 1. solicitar: el usuario da su mail y se le manda un link con un token.
 * 2. restablecer: con ese token elige la contrasena nueva.
 *
 * Queda separado de UsuarioService porque tiene dependencias propias (tokens
 * y mail) que el registro y el login no necesitan.
 */
@Service
public class RecuperacionService {

    static final int MINUTOS_DE_VALIDEZ = 30;
    static final String MENSAJE_TOKEN_INVALIDO =
            "El link para cambiar la contrasena no es valido o ya vencio. Pedi uno nuevo.";

    private final UsuarioRepository usuarioRepository;
    private final TokenRecuperacionRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ServicioMail servicioMail;
    private final String urlFront;
    private final SecureRandom aleatorio = new SecureRandom();

    public RecuperacionService(UsuarioRepository usuarioRepository,
                               TokenRecuperacionRepository tokenRepository,
                               PasswordEncoder passwordEncoder,
                               ServicioMail servicioMail,
                               @Value("${amicus.front-url:http://localhost:5173}") String urlFront) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.servicioMail = servicioMail;
        this.urlFront = urlFront;
    }

    /**
     * No devuelve nada ni falla si el mail no existe: por el mismo motivo que el
     * login, una respuesta distinta permitiria averiguar que mails estan
     * registrados. Pedir un link nuevo anula los anteriores.
     */
    @Transactional
    public void solicitar(RecuperarRequest request) {
        usuarioRepository.findByEmail(request.email().trim())
                .filter(usuario -> Boolean.TRUE.equals(usuario.getActivo()))
                .ifPresent(this::generarYEnviar);
    }

    private void generarYEnviar(Usuario usuario) {
        tokenRepository.findByUsuarioAndUsadoFalse(usuario).forEach(anterior -> anterior.setUsado(true));

        String token = generarToken();
        tokenRepository.save(TokenRecuperacion.builder()
                .tokenHash(hashear(token))
                .usuario(usuario)
                .expiraEn(LocalDateTime.now().plusMinutes(MINUTOS_DE_VALIDEZ))
                .build());

        servicioMail.enviarRecuperacion(usuario.getEmail(), urlFront + "/restablecer?token=" + token);
    }

    /** El token sirve una sola vez: despues de usarlo queda marcado y ya no vale. */
    @Transactional
    public void restablecer(RestablecerRequest request) {
        TokenRecuperacion token = tokenRepository.findByTokenHash(hashear(request.token()))
                .filter(TokenRecuperacion::estaVigente)
                .orElseThrow(() -> new ReglaDeNegocioException(MENSAJE_TOKEN_INVALIDO));

        token.getUsuario().setPassword(passwordEncoder.encode(request.password()));
        token.setUsado(true);
    }

    /** 32 bytes aleatorios en base64 apto para URL: imposible de adivinar. */
    private String generarToken() {
        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hashear(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            // Toda JVM trae SHA-256: si falta, el problema es del entorno.
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }
}
