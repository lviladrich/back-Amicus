package com.uade.amicus.service;

import com.uade.amicus.dto.request.ActualizarCuposRequest;
import com.uade.amicus.dto.request.ImagenRequest;
import com.uade.amicus.dto.request.ServicioRequest;
import com.uade.amicus.dto.response.ServicioDetalleResponse;
import com.uade.amicus.dto.response.ServicioResumenResponse;
import com.uade.amicus.exception.OperacionNoPermitidaException;
import com.uade.amicus.exception.RecursoNoEncontradoException;
import com.uade.amicus.exception.ReglaDeNegocioException;
import com.uade.amicus.model.Servicio;
import com.uade.amicus.model.ServicioImagen;
import com.uade.amicus.repository.ResenaRepository;
import com.uade.amicus.repository.ServicioImagenRepository;
import com.uade.amicus.repository.ServicioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Alta, baja, modificacion y consulta de publicaciones.
 */
@Service
public class ServicioService {

    private final ServicioRepository servicioRepository;
    private final ServicioImagenRepository imagenRepository;
    private final ResenaRepository resenaRepository;
    private final CategoriaService categoriaService;
    private final UsuarioService usuarioService;
    private final ZonaService zonaService;

    /**
     * Se inyecta ResenaRepository y no ResenaService a proposito: aca solo hace
     * falta leer el promedio, y depender del service crearia un ciclo, porque
     * ResenaService ya depende de este.
     */
    public ServicioService(ServicioRepository servicioRepository,
                           ServicioImagenRepository imagenRepository,
                           ResenaRepository resenaRepository,
                           CategoriaService categoriaService,
                           UsuarioService usuarioService,
                           ZonaService zonaService) {
        this.servicioRepository = servicioRepository;
        this.imagenRepository = imagenRepository;
        this.resenaRepository = resenaRepository;
        this.categoriaService = categoriaService;
        this.usuarioService = usuarioService;
        this.zonaService = zonaService;
    }

    /**
     * Arma el detalle con su calificacion.
     *
     * Existe para que los seis lugares que devuelven un detalle lo hagan igual:
     * si uno llamara al DTO directo, ese endpoint devolveria el servicio sin
     * promedio y nadie se enteraria hasta verlo en la pantalla.
     */
    private ServicioDetalleResponse detalleDe(Servicio servicio) {
        return ServicioDetalleResponse.desde(
                servicio,
                resenaRepository.promedioDe(servicio.getId()),
                resenaRepository.countByServicioId(servicio.getId()));
    }

    /** Catalogo completo, ordenado alfabeticamente como pide la consigna. */
    @Transactional(readOnly = true)
    public List<ServicioResumenResponse> listar() {
        return servicioRepository.findByActivoTrueOrderByTituloAsc().stream()
                .map(ServicioResumenResponse::desde)
                .toList();
    }

    /** Catalogo filtrado. Los filtros nulos simplemente no filtran. */
    @Transactional(readOnly = true)
    public List<ServicioResumenResponse> buscar(Long categoriaId, Long zonaId,
                                                String texto, boolean conCupo) {
        String textoLimpio = (texto == null || texto.isBlank()) ? null : texto.trim();
        return servicioRepository.buscar(categoriaId, zonaId, textoLimpio, conCupo).stream()
                .map(ServicioResumenResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public ServicioDetalleResponse buscarDetalle(Long id) {
        Servicio servicio = servicioRepository.buscarDetalle(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Servicio", id));
        return detalleDe(servicio);
    }

    @Transactional(readOnly = true)
    public List<ServicioResumenResponse> listarPorProfesional(Long profesionalId) {
        return servicioRepository.findByProfesionalIdAndActivoTrueOrderByTituloAsc(profesionalId)
                .stream()
                .map(ServicioResumenResponse::desde)
                .toList();
    }

    /**
     * Alta de publicacion.
     *
     * La consigna pide adjuntar una o mas fotos, la descripcion y la categoria.
     */
    @Transactional
    public ServicioDetalleResponse crear(ServicioRequest request) {
        Servicio servicio = Servicio.builder()
                .titulo(request.titulo())
                .descripcion(request.descripcion())
                .precio(request.precio())
                .cuposDisponibles(request.cuposDisponibles())
                .categoria(categoriaService.obtenerEntidad(request.categoriaId()))
                .profesional(usuarioService.obtenerEntidad(request.profesionalId()))
                .zonas(zonaService.obtenerPorIds(request.zonaIds()))
                .build();

        if (request.imagenes() != null) {
            int orden = 0;
            for (String url : request.imagenes()) {
                servicio.agregarImagen(ServicioImagen.builder()
                        .url(url)
                        .ordenVisualizacion(orden++)
                        .build());
            }
        }

        return detalleDe(servicioRepository.save(servicio));
    }

    /**
     * Modificacion.
     *
     * No hace falta llamar a save(): dentro de una transaccion, Hibernate
     * detecta los cambios sobre una entidad administrada y los escribe al
     * confirmar. Se llama dirty checking.
     */
    @Transactional
    public ServicioDetalleResponse actualizar(Long id, Long usuarioId, ServicioRequest request) {
        Servicio servicio = obtenerEntidad(id);
        validarPropietario(servicio, usuarioId, "modificar");

        servicio.setTitulo(request.titulo());
        servicio.setDescripcion(request.descripcion());
        servicio.setPrecio(request.precio());
        servicio.setCuposDisponibles(request.cuposDisponibles());
        servicio.setCategoria(categoriaService.obtenerEntidad(request.categoriaId()));
        servicio.setZonas(zonaService.obtenerPorIds(request.zonaIds()));

        return detalleDe(servicio);
    }

    /**
     * Ajuste de cupos.
     *
     * La consigna dice que "el usuario QUE CREA dicho producto podra manejar el
     * stock del mismo", asi que la validacion de propietario no es un extra:
     * es parte del requisito.
     */
    @Transactional
    public ServicioDetalleResponse actualizarCupos(Long id, Long usuarioId,
                                                   ActualizarCuposRequest request) {
        Servicio servicio = obtenerEntidad(id);
        validarPropietario(servicio, usuarioId, "manejar los cupos de");
        servicio.setCuposDisponibles(request.cuposDisponibles());
        return detalleDe(servicio);
    }

    /**
     * Baja LOGICA.
     *
     * No se borra la fila porque el servicio puede estar referenciado en ordenes
     * ya confirmadas.
     */
    @Transactional
    public void eliminar(Long id, Long usuarioId) {
        Servicio servicio = obtenerEntidad(id);
        validarPropietario(servicio, usuarioId, "dar de baja");
        if (Boolean.FALSE.equals(servicio.getActivo())) {
            throw new ReglaDeNegocioException("El servicio ya estaba dado de baja");
        }
        servicio.setActivo(false);
    }

    @Transactional
    public ServicioDetalleResponse agregarImagen(Long servicioId, Long usuarioId,
                                                 ImagenRequest request) {
        Servicio servicio = obtenerEntidad(servicioId);
        validarPropietario(servicio, usuarioId, "agregar fotos a");
        servicio.agregarImagen(ServicioImagen.builder()
                .url(request.url())
                .ordenVisualizacion(request.ordenVisualizacion() != null
                        ? request.ordenVisualizacion()
                        : servicio.getImagenes().size())
                .build());
        return detalleDe(servicio);
    }

    @Transactional
    public void eliminarImagen(Long servicioId, Long usuarioId, Long imagenId) {
        Servicio servicio = obtenerEntidad(servicioId);
        validarPropietario(servicio, usuarioId, "quitar fotos de");
        ServicioImagen imagen = imagenRepository.findById(imagenId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Imagen", imagenId));

        if (!imagen.getServicio().getId().equals(servicioId)) {
            throw new ReglaDeNegocioException("La imagen no pertenece a ese servicio");
        }
        servicio.quitarImagen(imagen);
    }

    @Transactional(readOnly = true)
    public Servicio obtenerEntidad(Long id) {
        return servicioRepository.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Servicio", id));
    }

    /**
     * Verifica que quien pide la operacion sea el profesional que publico el
     * servicio.
     *
     * Sin esto, cualquiera que conozca el id de una publicacion podria cambiarle
     * el precio o darla de baja. La consigna lo pide de forma implicita: dice
     * que "el usuario QUE CREA dicho producto" es quien maneja su stock.
     *
     * Lanza 403 y no 401: el usuario esta identificado, lo que no tiene es
     * permiso sobre este recurso en particular.
     */
    private void validarPropietario(Servicio servicio, Long usuarioId, String accion) {
        if (usuarioId == null) {
            throw new OperacionNoPermitidaException(
                    "Falta indicar quien realiza la operacion");
        }
        if (!servicio.getProfesional().getId().equals(usuarioId)) {
            throw new OperacionNoPermitidaException(
                    "Solo el profesional que publico el servicio puede " + accion
                            + " \"" + servicio.getTitulo() + "\"");
        }
    }
}
