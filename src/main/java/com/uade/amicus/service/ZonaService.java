package com.uade.amicus.service;

import com.uade.amicus.dto.request.ZonaRequest;
import com.uade.amicus.dto.response.ZonaResponse;
import com.uade.amicus.exception.ConflictoException;
import com.uade.amicus.exception.RecursoNoEncontradoException;
import com.uade.amicus.exception.ReglaDeNegocioException;
import com.uade.amicus.model.Zona;
import com.uade.amicus.repository.ServicioRepository;
import com.uade.amicus.repository.ZonaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class ZonaService {

    private final ZonaRepository zonaRepository;
    private final ServicioRepository servicioRepository;

    public ZonaService(ZonaRepository zonaRepository, ServicioRepository servicioRepository) {
        this.zonaRepository = zonaRepository;
        this.servicioRepository = servicioRepository;
    }

    @Transactional(readOnly = true)
    public List<ZonaResponse> listar() {
        return zonaRepository.findAllByOrderByNombreAsc().stream()
                .map(ZonaResponse::desde)
                .toList();
    }

    @Transactional
    public ZonaResponse crear(ZonaRequest request) {
        if (zonaRepository.existsByNombreIgnoreCase(request.nombre())) {
            throw new ReglaDeNegocioException("Ya existe la zona " + request.nombre());
        }
        return ZonaResponse.desde(zonaRepository.save(
                Zona.builder().nombre(request.nombre()).build()));
    }

    @Transactional(readOnly = true)
    public ZonaResponse buscarPorId(Long id) {
        return ZonaResponse.desde(obtenerEntidad(id));
    }

    @Transactional(readOnly = true)
    public Zona obtenerEntidad(Long id) {
        return zonaRepository.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Zona", id));
    }

    @Transactional
    public ZonaResponse actualizar(Long id, ZonaRequest request) {
        Zona zona = obtenerEntidad(id);
        if (zonaRepository.existsByNombreIgnoreCaseAndIdNot(request.nombre(), id)) {
            throw new ReglaDeNegocioException("Ya existe la zona " + request.nombre());
        }
        zona.setNombre(request.nombre());
        return ZonaResponse.desde(zona);
    }

    @Transactional
    public void eliminar(Long id) {
        Zona zona = obtenerEntidad(id);
        if (servicioRepository.existsByZonasId(id)) {
            throw new ConflictoException(
                    "No se puede eliminar la zona \"" + zona.getNombre()
                            + "\" porque tiene servicios asociados");
        }
        zonaRepository.delete(zona);
    }

    /**
     * Trae varias zonas por id validando que todas existan.
     *
     * Se hace en una sola consulta y no una por id: con diez zonas serian diez
     * viajes a la base para resolver algo que se resuelve con un "in".
     */
    @Transactional(readOnly = true)
    public Set<Zona> obtenerPorIds(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        Set<Zona> encontradas = zonaRepository.findByIdIn(ids);
        if (encontradas.size() != ids.size()) {
            throw new ReglaDeNegocioException("Alguna de las zonas indicadas no existe");
        }
        return encontradas;
    }
}
