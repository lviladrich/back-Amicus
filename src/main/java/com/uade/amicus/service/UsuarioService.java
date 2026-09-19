package com.uade.amicus.service;

import com.uade.amicus.dto.request.ActualizarPerfilRequest;
import com.uade.amicus.dto.request.LoginRequest;
import com.uade.amicus.dto.request.RegistroRequest;
import com.uade.amicus.dto.response.UsuarioResponse;
import com.uade.amicus.exception.CredencialesInvalidasException;
import com.uade.amicus.exception.OperacionNoPermitidaException;
import com.uade.amicus.exception.RecursoNoEncontradoException;
import com.uade.amicus.exception.ReglaDeNegocioException;
import com.uade.amicus.model.Carrito;
import com.uade.amicus.model.Rol;
import com.uade.amicus.model.Usuario;
import com.uade.amicus.repository.CarritoRepository;
import com.uade.amicus.repository.UsuarioRepository;
import com.uade.amicus.security.UsuarioPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final CarritoRepository carritoRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    /**
     * Inyeccion por constructor y no por campo con @Autowired.
     *
     * Permite marcar las dependencias como final (nadie las cambia despues de
     * construido el objeto) y deja explicito de que depende esta clase: si el
     * constructor tiene ocho parametros, el service esta haciendo demasiado.
     */
    public UsuarioService(UsuarioRepository usuarioRepository,
                          CarritoRepository carritoRepository,
                          PasswordEncoder passwordEncoder,
                          AuthenticationManager authenticationManager) {
        this.usuarioRepository = usuarioRepository;
        this.carritoRepository = carritoRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
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
                // El rol lo decide el sistema, nunca el cliente: si viniera en el
                // request, cualquiera podria registrarse como ADMIN.
                .rol(Rol.USUARIO)
                .build();

        Usuario guardado = usuarioRepository.save(usuario);

        carritoRepository.save(Carrito.builder().usuario(guardado).build());

        return UsuarioResponse.desde(guardado);
    }

    /**
     * Login por mail y contrasena.
     *
     * Se delega en el AuthenticationManager en vez de comparar la contrasena a
     * mano: es el mismo camino que va a recorrer cada pedido HTTP autenticado
     * (por ejemplo, con Basic Auth contra los endpoints de ADMIN), asi que el
     * login queda probando el mecanismo real y no una version paralela de la
     * autenticacion.
     *
     * El mensaje de error es el mismo para "el mail no existe" y para "la
     * contrasena no coincide" (las dos caen en BadCredentialsException). Es a
     * proposito: si fueran distintos, cualquiera podria averiguar que mails
     * estan registrados probando de a uno.
     */
    @Transactional(readOnly = true)
    public UsuarioResponse login(LoginRequest request) {
        Authentication autenticacion;
        try {
            autenticacion = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (DisabledException ex) {
            throw new CredencialesInvalidasException("La cuenta esta deshabilitada");
        } catch (BadCredentialsException ex) {
            throw new CredencialesInvalidasException("Mail o contrasena incorrectos");
        }

        UsuarioPrincipal principal = (UsuarioPrincipal) autenticacion.getPrincipal();
        return UsuarioResponse.desde(principal.getUsuario());
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

    /**
     * Edicion de perfil. No permite cambiar username ni password: son datos
     * mas sensibles y quedan fuera de este endpoint.
     *
     * Igual que en ServicioService.validarPropietario, la identidad viaja como
     * parametro y se compara contra el id del recurso.
     */
    @Transactional
    public UsuarioResponse actualizarPerfil(Long id, Long usuarioId, ActualizarPerfilRequest request) {
        Usuario usuario = obtenerEntidad(id);

        if (usuarioId == null || !id.equals(usuarioId)) {
            throw new OperacionNoPermitidaException("Solo el propio usuario puede editar su perfil");
        }

        if (!usuario.getEmail().equalsIgnoreCase(request.email())
                && usuarioRepository.existsByEmail(request.email())) {
            throw new ReglaDeNegocioException("Ya existe un usuario con el mail " + request.email());
        }

        usuario.setNombre(request.nombre());
        usuario.setApellido(request.apellido());
        usuario.setEmail(request.email());

        return UsuarioResponse.desde(usuario);
    }
}
