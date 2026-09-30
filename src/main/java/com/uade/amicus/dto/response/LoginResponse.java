package com.uade.amicus.dto.response;

/**
 * Respuesta del login: el token y los datos del usuario que acaba de entrar.
 *
 * Van juntos para que el front resuelva el login en un solo pedido, sin tener
 * que consultar el perfil despues.
 *
 * tipo es siempre "Bearer": es la palabra que hay que anteponer al token en la
 * cabecera Authorization, y viaja en la respuesta para que el cliente no tenga
 * que tenerla escrita.
 */
public record LoginResponse(
        String token,
        String tipo,
        long expiraEnSegundos,
        UsuarioResponse usuario
) {
    public static LoginResponse de(String token, long expiraEnSegundos, UsuarioResponse usuario) {
        return new LoginResponse(token, "Bearer", expiraEnSegundos, usuario);
    }
}
