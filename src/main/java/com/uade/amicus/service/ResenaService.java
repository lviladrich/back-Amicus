package com.uade.amicus.service;

import com.uade.amicus.dto.request.ResenaRequest;
import com.uade.amicus.dto.response.PaginaResponse;
import com.uade.amicus.dto.response.ResenaResponse;
import com.uade.amicus.exception.ConflictoException;
import com.uade.amicus.exception.OperacionNoPermitidaException;
import com.uade.amicus.exception.RecursoNoEncontradoException;
import com.uade.amicus.exception.ReglaDeNegocioException;
import com.uade.amicus.model.EstadoOrden;
import com.uade.amicus.model.Resena;
import com.uade.amicus.model.Servicio;
import com.uade.amicus.model.Usuario;
import com.uade.amicus.repository.OrdenRepository;
import com.uade.amicus.repository.ResenaRepository;
import com.uade.amicus.util.Paginacion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Calificaciones de los servicios.
 *
 * Toda la logica esta en las tres reglas que decide quien puede escribir. Sin
 * ellas las reseñas no valen nada: un catalogo donde cualquiera puede calificar
 * cualquier cosa se llena de opiniones de gente que nunca contrato el servicio,
 * y el promedio deja de informar.
 */
@Service
public class ResenaService {

    private final ResenaRepository resenaRepository;
    private final OrdenRepository ordenRepository;
    private final ServicioService servicioService;
    private final UsuarioService usuarioService;

    public ResenaService(ResenaRepository resenaRepository,
                         OrdenRepository ordenRepository,
                         ServicioService servicioService,
                         UsuarioService usuarioService) {
        this.resenaRepository = resenaRepository;
        this.ordenRepository = ordenRepository;
        this.servicioService = servicioService;
        this.usuarioService = usuarioService;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<ResenaResponse> listarPorServicio(Long servicioId, int pagina, int tamanio) {
        // Que el servicio exista, para no devolver una lista vacia cuando en
        // realidad el id es inventado.
        servicioService.obtenerEntidad(servicioId);

        return PaginaResponse.desde(
                resenaRepository.findByServicioIdOrderByFechaDesc(servicioId, Paginacion.de(pagina, tamanio))
                        .map(ResenaResponse::desde));
    }

    /**
     * Las tres reglas, en orden de menor a mayor costo de verificar:
     *
     * 1. Nadie reseña su propio servicio. Seria calificarse a si mismo.
     * 2. Solo reseña quien contrato. Es la que le da valor al promedio, y se
     *    apoya en las ordenes: tiene que existir una CONFIRMADA con ese servicio.
     *    Una orden cancelada no habilita, porque el trabajo no se presto.
     * 3. Una sola reseña por persona y servicio, para que nadie pueda repetir su
     *    opinion y correr el promedio.
     */
    @Transactional
    public ResenaResponse crear(Long servicioId, Long autorId, ResenaRequest request) {
        Servicio servicio = servicioService.obtenerEntidad(servicioId);
        Usuario autor = usuarioService.obtenerEntidad(autorId);

        if (servicio.getProfesional().getId().equals(autorId)) {
            throw new ReglaDeNegocioException("No podes reseñar tu propio servicio");
        }

        if (!ordenRepository.existeCompraConfirmada(autorId, servicioId, EstadoOrden.CONFIRMADA)) {
            throw new OperacionNoPermitidaException(
                    "Solo podes reseñar un servicio que hayas contratado. No hay ninguna orden "
                            + "confirmada tuya que incluya \"" + servicio.getTitulo() + "\"");
        }

        if (resenaRepository.existsByServicioIdAndAutorId(servicioId, autorId)) {
            throw new ConflictoException(
                    "Ya reseñaste \"" + servicio.getTitulo() + "\". Podes borrar tu reseña "
                            + "y escribir otra");
        }

        Resena resena = Resena.builder()
                .servicio(servicio)
                .autor(autor)
                .puntaje(request.puntaje())
                .comentario(request.comentario())
                .build();

        return ResenaResponse.desde(resenaRepository.save(resena));
    }

    /**
     * Borrado fisico y no logico, al contrario que los servicios.
     *
     * Un servicio dado de baja tiene que sobrevivir porque las ordenes lo
     * referencian. Una reseña no la referencia nadie: si el autor la borra, se
     * va, y el promedio se recalcula solo.
     */
    @Transactional
    public void eliminar(Long resenaId, Long usuarioId) {
        Resena resena = resenaRepository.findById(resenaId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Resena", resenaId));

        if (usuarioId == null || !resena.getAutor().getId().equals(usuarioId)) {
            throw new OperacionNoPermitidaException(
                    "Solo el autor puede borrar su reseña");
        }

        resenaRepository.delete(resena);
    }
}
