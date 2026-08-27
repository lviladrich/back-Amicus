package com.uade.amicus.controller;

import com.uade.amicus.dto.request.ZonaRequest;
import com.uade.amicus.dto.response.ZonaResponse;
import com.uade.amicus.service.ZonaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/zonas")
public class ZonaController {

    private final ZonaService zonaService;

    public ZonaController(ZonaService zonaService) {
        this.zonaService = zonaService;
    }

    @GetMapping
    public ResponseEntity<List<ZonaResponse>> listar() {
        return ResponseEntity.ok(zonaService.listar());
    }

    @PostMapping
    public ResponseEntity<ZonaResponse> crear(@Valid @RequestBody ZonaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(zonaService.crear(request));
    }
}
