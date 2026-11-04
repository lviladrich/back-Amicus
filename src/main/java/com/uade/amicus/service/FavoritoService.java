package com.uade.amicus.service;

import com.uade.amicus.dto.response.ServicioResumenResponse;
import com.uade.amicus.exception.RecursoNoEncontradoException;
import com.uade.amicus.model.Favorito;
import com.uade.amicus.model.Servicio;
import com.uade.amicus.repository.FavoritoRepository;
import com.uade.amicus.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Favoritos del usuario: listar, agregar y quitar.
 *
 * Las tres operaciones devuelven la lista completa actualizada, igual que el
 * carrito: asi el front reemplaza lo que tenia sin calcular nada por su cuenta.
 * Agregar y quitar son idempotentes: repetirlos no da error ni duplica filas.
 */
@Service
public class FavoritoService {

    private final FavoritoRepository favoritoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ServicioService servicioService;

    public FavoritoService(FavoritoRepository favoritoRepository,
                           UsuarioRepository usuarioRepository,
                           ServicioService servicioService) {
        this.favoritoRepository = favoritoRepository;
        this.usuarioRepository = usuarioRepository;
        this.servicioService = servicioService;
    }

    @Transactional(readOnly = true)
    public List<ServicioResumenResponse> listar(Long usuarioId) {
        return favoritoRepository.listarDelUsuario(usuarioId).stream()
                .map(favorito -> ServicioResumenResponse.desde(favorito.getServicio()))
                .toList();
    }

    @Transactional
    public List<ServicioResumenResponse> agregar(Long usuarioId, Long servicioId) {
        Servicio servicio = servicioService.obtenerEntidad(servicioId);
        // Uno dado de baja se trata como inexistente, igual que en el catalogo
        if (!Boolean.TRUE.equals(servicio.getActivo())) {
            throw RecursoNoEncontradoException.de("Servicio", servicioId);
        }

        if (!favoritoRepository.existsByUsuarioIdAndServicioId(usuarioId, servicioId)) {
            favoritoRepository.save(Favorito.builder()
                    .usuario(usuarioRepository.getReferenceById(usuarioId))
                    .servicio(servicio)
                    .build());
        }
        return listar(usuarioId);
    }

    @Transactional
    public List<ServicioResumenResponse> quitar(Long usuarioId, Long servicioId) {
        favoritoRepository.findByUsuarioIdAndServicioId(usuarioId, servicioId)
                .ifPresent(favoritoRepository::delete);
        return listar(usuarioId);
    }
}
