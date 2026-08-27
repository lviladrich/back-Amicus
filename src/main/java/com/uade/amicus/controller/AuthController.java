package com.uade.amicus.controller;

import com.uade.amicus.dto.request.LoginRequest;
import com.uade.amicus.dto.request.RegistroRequest;
import com.uade.amicus.dto.response.UsuarioResponse;
import com.uade.amicus.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Registro y login.
 *
 * El controller no decide nada: recibe el pedido, se lo pasa al service y
 * traduce el resultado en una respuesta HTTP. Toda la logica esta en
 * UsuarioService.
 */
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
    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.registrar(request));
    }

    /** 200 OK: el login no crea nada, solo verifica. */
    @PostMapping("/login")
    public ResponseEntity<UsuarioResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(usuarioService.login(request));
    }

    @GetMapping("/usuarios/{id}")
    public ResponseEntity<UsuarioResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }
}
