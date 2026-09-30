package com.uade.amicus.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Firma y validacion de los tokens JWT.
 *
 * Un JWT es un texto con tres partes separadas por puntos: quien es el usuario,
 * hasta cuando vale, y una firma calculada sobre las dos anteriores con una
 * clave que solo conoce el servidor. El contenido viaja a la vista (no esta
 * cifrado, solo codificado), pero no se puede alterar sin invalidar la firma.
 *
 * Eso es lo que permite que el servidor no guarde sesiones: no necesita
 * recordar quien inicio sesion, porque cada pedido trae la prueba consigo.
 */
@Component
public class ServicioJwt {

    private final SecretKey clave;
    private final long duracionMs;

    /**
     * El secreto se lee de la configuracion y no esta escrito en el codigo:
     * quien tenga el secreto puede fabricar tokens validos para cualquier
     * usuario, asi que en produccion se pasa por variable de entorno.
     *
     * HMAC-SHA256 exige una clave de al menos 256 bits, o sea 32 caracteres.
     */
    public ServicioJwt(@Value("${amicus.jwt.secreto}") String secreto,
                       @Value("${amicus.jwt.duracion-ms}") long duracionMs) {
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.duracionMs = duracionMs;
    }

    /**
     * Arma el token para un usuario ya autenticado.
     *
     * El subject es el mail, que es con lo que se identifica en esta API. Se
     * agrega el id como claim para que el front no tenga que pedirlo aparte.
     *
     * No se guarda el rol adentro: si un usuario cambia de rol, un token viejo
     * seguiria afirmando el rol anterior hasta vencer. El rol se relee de la
     * base en cada pedido, en FiltroJwt.
     */
    public String generar(UsuarioPrincipal principal) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(principal.getUsername())
                .claim("uid", principal.getUsuario().getId())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plusMillis(duracionMs)))
                .signWith(clave)
                .compact();
    }

    /**
     * Devuelve el mail que declara el token, despues de verificar la firma y
     * la fecha de vencimiento.
     *
     * Si algo no cierra lanza una JwtException, que FiltroJwt traduce en un
     * 401. Es importante que la verificacion venga antes de leer nada: un
     * token sin validar es texto que mando un desconocido.
     */
    public String mailDe(String token) {
        return Jwts.parser()
                .verifyWith(clave)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /** Segundos de vida del token, para informarselo al cliente en el login. */
    public long duracionEnSegundos() {
        return duracionMs / 1000;
    }
}
