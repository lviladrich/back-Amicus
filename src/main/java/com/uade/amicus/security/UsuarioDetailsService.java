package com.uade.amicus.security;

import com.uade.amicus.model.Usuario;
import com.uade.amicus.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Le dice a Spring Security como encontrar a un usuario.
 *
 * UserDetails describe a un usuario ya cargado; UserDetailsService es quien lo
 * busca. Tiene un solo metodo, loadUserByUsername, que Spring Security invoca
 * cada vez que necesita autenticar a alguien: recibe el identificador, va al
 * repositorio y devuelve el UserDetails, o lanza UsernameNotFoundException.
 *
 * Con solo declarar este bean, Spring Boot deja de generar el usuario en
 * memoria con contrasena aleatoria que imprime en la consola y pasa a usar
 * nuestra tabla de usuarios.
 */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * El parametro se llama username por la interfaz, pero en Amicus el
     * identificador de login es el mail, asi que se busca con findByEmail.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No existe un usuario con el mail " + email));
        return new UsuarioPrincipal(usuario);
    }
}
