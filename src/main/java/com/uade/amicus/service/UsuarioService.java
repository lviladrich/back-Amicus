package com.uade.amicus.service;

import com.uade.amicus.dto.request.LoginRequest;
import com.uade.amicus.dto.request.RegistroRequest;
import com.uade.amicus.dto.response.UsuarioResponse;
import com.uade.amicus.exception.CredencialesInvalidasException;
import com.uade.amicus.exception.RecursoNoEncontradoException;
import com.uade.amicus.exception.ReglaDeNegocioException;
import com.uade.amicus.model.Carrito;
import com.uade.amicus.model.Usuario;
import com.uade.amicus.repository.CarritoRepository;
import com.uade.amicus.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final CarritoRepository carritoRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Inyeccion por constructor y no por campo con @Autowired.
     *
     * Permite marcar las dependencias como final (nadie las cambia despues de
     * construido el objeto) y deja explicito de que depende esta clase: si el
     * constructor tiene ocho parametros, el service esta haciendo demasiado.
     */
    public UsuarioService(UsuarioRepository usuarioRepository,
                          CarritoRepository carritoRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.carritoRepository = carritoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registro. Crea el usuario y su carrito en la misma transaccion.
     *
     * El carrito se crea aca y no la primera vez que se agrega algo, para que
     * todo usuario tenga siempre uno y el resto del codigo no tenga que
     * preguntarse si existe.
     */
    @Transactional
    public UsuarioResponse registrar(RegistroRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new ReglaDeNegocioException("Ya existe un usuario con el mail " + request.email());
        }
        if (usuarioRepository.existsByUsername(request.username())) {
            throw new ReglaDeNegocioException("El nombre de usuario " + request.username() + " ya esta en uso");
        }

        Usuario usuario = Usuario.builder()
                .username(request.username())
                .email(request.email())
                // Se guarda el hash, nunca la contrasena que escribio el usuario.
                .password(passwordEncoder.encode(request.password()))
                .nombre(request.nombre())
                .apellido(request.apellido())
                .build();

        Usuario guardado = usuarioRepository.save(usuario);

        carritoRepository.save(Carrito.builder().usuario(guardado).build());

        return UsuarioResponse.desde(guardado);
    }

    /**
     * Login por mail y contrasena.
     *
     * El mensaje de error es el mismo para "el mail no existe" y para "la
     * contrasena no coincide". Es a proposito: si fueran distintos, cualquiera
     * podria averiguar que mails estan registrados probando de a uno.
     */
    @Transactional(readOnly = true)
    public UsuarioResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new CredencialesInvalidasException("Mail o contrasena incorrectos"));

        if (!passwordEncoder.matches(request.password(), usuario.getPassword())) {
            throw new CredencialesInvalidasException("Mail o contrasena incorrectos");
        }

        if (Boolean.FALSE.equals(usuario.getActivo())) {
            throw new CredencialesInvalidasException("La cuenta esta deshabilitada");
        }

        return UsuarioResponse.desde(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(Long id) {
        return UsuarioResponse.desde(obtenerEntidad(id));
    }

    /** Uso interno: los demas services necesitan la entidad, no el DTO. */
    @Transactional(readOnly = true)
    public Usuario obtenerEntidad(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Usuario", id));
    }
}
