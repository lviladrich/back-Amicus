package com.uade.amicus.service;

import com.uade.amicus.dto.request.ZonaRequest;
import com.uade.amicus.dto.response.ZonaResponse;
import com.uade.amicus.exception.ReglaDeNegocioException;
import com.uade.amicus.model.Zona;
import com.uade.amicus.repository.ZonaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class ZonaService {

    private final ZonaRepository zonaRepository;

    public ZonaService(ZonaRepository zonaRepository) {
        this.zonaRepository = zonaRepository;
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
