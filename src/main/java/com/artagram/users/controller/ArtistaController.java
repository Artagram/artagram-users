package com.artagram.users.controller;

import com.artagram.users.common.JwtUserResolver;
import com.artagram.users.dto.ArtistaEnVivo;
import com.artagram.users.dto.ArtistaResumen;
import com.artagram.users.dto.Seguimiento;
import com.artagram.users.service.ActividadEnVivoService;
import com.artagram.users.service.SeguidorService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/artistas")
public class ArtistaController {

    private final SeguidorService seguidorService;
    private final ActividadEnVivoService actividadEnVivo;
    private final JwtUserResolver userResolver;

    public ArtistaController(SeguidorService seguidorService, ActividadEnVivoService actividadEnVivo, JwtUserResolver userResolver) {
        this.seguidorService = seguidorService;
        this.actividadEnVivo = actividadEnVivo;
        this.userResolver = userResolver;
    }

    @GetMapping
    public ResponseEntity<List<ArtistaResumen>> buscar(@RequestParam(defaultValue = "") String q, HttpServletRequest request) {
        var usuario = userResolver.resolverRequerido(request);
        return ResponseEntity.ok(seguidorService.buscar(q, usuario.id()));
    }

    @GetMapping("/siguiendo/en-vivo")
    public ResponseEntity<List<ArtistaEnVivo>> siguiendoEnVivo(HttpServletRequest request) {
        return ResponseEntity.ok(actividadEnVivo.siguiendo(userResolver.resolverRequerido(request).id()));
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
