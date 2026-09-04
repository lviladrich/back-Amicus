package com.uade.amicus.controller;

import com.uade.amicus.dto.request.ActualizarPerfilRequest;
import com.uade.amicus.dto.request.LoginRequest;
import com.uade.amicus.dto.request.RegistroRequest;
import com.uade.amicus.dto.response.UsuarioResponse;
import com.uade.amicus.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Registro y login.
 *
 * El controller no decide nada: recibe el pedido, se lo pasa al service y
 * traduce el resultado en una respuesta HTTP. Toda la logica esta en
 * UsuarioService.
 */
@Tag(name = "1. Autenticacion", description = "Registro de usuarios y login")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /**
     * @Valid dispara las validaciones declaradas en el DTO. Si alguna falla,
     * Spring lanza una excepcion que el manejador global traduce en un 400 con
     * el detalle campo por campo.
     *
     * 201 CREATED y no 200: se creo un recurso nuevo.
     */
    @Operation(summary = "Registrar un usuario",
            description = "Crea el usuario y su carrito. La contrasena se guarda hasheada con BCrypt y nunca se devuelve.")
    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.registrar(request));
    }

    /** 200 OK: el login no crea nada, solo verifica. */
    @Operation(summary = "Iniciar sesion",
            description = "Valida mail y contrasena. Devuelve el mismo mensaje si el mail no existe o si la contrasena es incorrecta, para no revelar que mails estan registrados.")
    @PostMapping("/login")
    public ResponseEntity<UsuarioResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(usuarioService.login(request));
    }

    @Operation(summary = "Buscar un usuario por id")
    @GetMapping("/usuarios/{id}")
    public ResponseEntity<UsuarioResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }

    @Operation(summary = "Editar el perfil",
            description = "Modifica nombre, apellido y mail. No permite cambiar username ni contrasena. "
                    + "Solo el propio usuario puede editar su perfil: si usuarioId no coincide con el id "
                    + "del path, devuelve 403.")
    @PutMapping("/usuarios/{id}")
    public ResponseEntity<UsuarioResponse> actualizarPerfil(@PathVariable Long id,
                                                            @RequestParam Long usuarioId,
                                                            @Valid @RequestBody ActualizarPerfilRequest request) {
        return ResponseEntity.ok(usuarioService.actualizarPerfil(id, usuarioId, request));
    }
}
