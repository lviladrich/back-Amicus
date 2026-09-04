package com.uade.amicus.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Envoltorio propio para listados paginados.
 *
 * No se serializa el Page de Spring Data directo: no es un contrato JSON
 * estable (cambia entre versiones) y Spring ya avisa al arrancar que no
 * conviene devolverlo tal cual.
 */
public record PaginaResponse<T>(
        List<T> contenido,
        int pagina,
        int tamanio,
        long totalElementos,
        int totalPaginas,
        boolean esPrimera,
        boolean esUltima
) {
    public static <T> PaginaResponse<T> desde(Page<T> page) {
        return new PaginaResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }
}
