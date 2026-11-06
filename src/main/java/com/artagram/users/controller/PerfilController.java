package com.artagram.users.controller;

import com.artagram.users.common.JwtUserResolver;
import com.artagram.users.dto.ActualizarPerfilRequest;
import com.artagram.users.dto.PerfilResponse;
import com.artagram.users.service.PerfilService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class PerfilController {

    private final PerfilService perfilService;
    private final JwtUserResolver userResolver;

    public PerfilController(PerfilService perfilService, JwtUserResolver userResolver) {
        this.perfilService = perfilService;
        this.userResolver = userResolver;
    }

    @GetMapping("/api/perfil/me")
    public ResponseEntity<PerfilResponse> obtenerMiPerfil(HttpServletRequest request) {
        var usuario = userResolver.resolverRequerido(request);
        return ResponseEntity.ok(perfilService.obtenerPerfilPropio(usuario.id()));
    }

    @PutMapping("/api/perfil/me")
    public ResponseEntity<PerfilResponse> actualizarMiPerfil(@RequestBody ActualizarPerfilRequest req, HttpServletRequest request) {
        var usuario = userResolver.resolverRequerido(request);
        return ResponseEntity.ok(perfilService.actualizarPerfil(usuario.id(), req));
    }

    @PostMapping(value = "/api/perfil/me/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> subirFoto(@RequestParam(required = false) MultipartFile archivo, HttpServletRequest request) {
        var usuario = userResolver.resolverRequerido(request);
        return ResponseEntity.ok(Map.of("fotoUrl", perfilService.actualizarFoto(usuario.id(), archivo)));
    }

    @GetMapping("/api/perfiles/{nombreArtista}")
    public ResponseEntity<PerfilResponse> obtenerPerfilPublico(@PathVariable String nombreArtista, HttpServletRequest request) {
        UUID usuarioActualId = userResolver.resolverOpcional(request).map(JwtUserResolver.UsuarioAutenticado::id).orElse(null);
        return ResponseEntity.ok(perfilService.obtenerPerfilPublico(nombreArtista, usuarioActualId));
    }
}
