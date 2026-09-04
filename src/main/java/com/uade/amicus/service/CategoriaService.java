package com.uade.amicus.service;

import com.uade.amicus.dto.request.CategoriaRequest;
import com.uade.amicus.dto.response.CategoriaResponse;
import com.uade.amicus.exception.ConflictoException;
import com.uade.amicus.exception.RecursoNoEncontradoException;
import com.uade.amicus.exception.ReglaDeNegocioException;
import com.uade.amicus.model.Categoria;
import com.uade.amicus.repository.CategoriaRepository;
import com.uade.amicus.repository.ServicioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ServicioRepository servicioRepository;

    public CategoriaService(CategoriaRepository categoriaRepository, ServicioRepository servicioRepository) {
        this.categoriaRepository = categoriaRepository;
        this.servicioRepository = servicioRepository;
    }

    /**
     * readOnly = true en las lecturas: Hibernate no necesita rastrear cambios,
     * lo que ahorra memoria y hace la consulta mas rapida.
     */
    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar() {
        return categoriaRepository.findAllByOrderByNombreAsc().stream()
                .map(CategoriaResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoriaResponse buscarPorId(Long id) {
        return CategoriaResponse.desde(obtenerEntidad(id));
    }

    @Transactional
    public CategoriaResponse crear(CategoriaRequest request) {
        if (categoriaRepository.existsByNombreIgnoreCase(request.nombre())) {
            throw new ReglaDeNegocioException("Ya existe la categoria " + request.nombre());
        }

        Categoria categoria = Categoria.builder()
                .nombre(request.nombre())
                .descripcion(request.descripcion())
                .build();

        return CategoriaResponse.desde(categoriaRepository.save(categoria));
    }

    @Transactional
    public CategoriaResponse actualizar(Long id, CategoriaRequest request) {
        Categoria categoria = obtenerEntidad(id);
        categoria.setNombre(request.nombre());
        categoria.setDescripcion(request.descripcion());
        return CategoriaResponse.desde(categoria);
    }

    @Transactional(readOnly = true)
    public Categoria obtenerEntidad(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Categoria", id));
    }

    @Transactional
    public void eliminar(Long id) {
        Categoria categoria = obtenerEntidad(id);
        if (servicioRepository.existsByCategoriaId(id)) {
            throw new ConflictoException(
                    "No se puede eliminar la categoria \"" + categoria.getNombre()
                            + "\" porque tiene servicios asociados");
        }
        categoriaRepository.delete(categoria);
    }
}
