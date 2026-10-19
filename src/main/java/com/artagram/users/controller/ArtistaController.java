package com.artagram.users.controller;

import com.artagram.users.common.JwtUserResolver;
import com.artagram.users.dto.ArtistaResumen;
import com.artagram.users.dto.Seguimiento;
import com.artagram.users.service.SeguidorService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/artistas")
public class ArtistaController {

    private final SeguidorService seguidorService;
    private final JwtUserResolver userResolver;

    public ArtistaController(SeguidorService seguidorService, JwtUserResolver userResolver) {
        this.seguidorService = seguidorService;
        this.userResolver = userResolver;
    }

    @PostMapping("/{id}/seguir")
    public ResponseEntity<Seguimiento> toggleSeguir(@PathVariable UUID id, HttpServletRequest request) {
        var usuario = userResolver.resolverRequerido(request);
        return ResponseEntity.ok(seguidorService.toggleSeguir(usuario.id(), id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArtistaResumen> resumenArtista(@PathVariable UUID id, HttpServletRequest request) {
        UUID usuarioActualId = userResolver.resolverOpcional(request).map(JwtUserResolver.UsuarioAutenticado::id).orElse(null);
        return ResponseEntity.ok(seguidorService.resumenArtista(id, usuarioActualId));
    }
}
