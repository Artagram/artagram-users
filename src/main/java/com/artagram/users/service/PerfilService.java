package com.artagram.users.service;

import com.artagram.users.common.ApiException;
import com.artagram.users.domain.EnlacePerfil;
import com.artagram.users.domain.ObraDestacada;
import com.artagram.users.domain.ObraDestacadaRepository;
import com.artagram.users.domain.Perfil;
import com.artagram.users.domain.PerfilRepository;
import com.artagram.users.domain.Usuario;
import com.artagram.users.domain.UsuarioRepository;
import com.artagram.users.dto.ActualizarPerfilRequest;
import com.artagram.users.dto.PerfilResponse;
import com.artagram.users.storage.StorageService;
import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PerfilService {

    private final PerfilRepository perfilRepo;
    private final ObraDestacadaRepository obraRepo;
    private final UsuarioRepository usuarioRepo;
    private final StorageService storageService;

    private static final Map<String, String> EXTENSIONES_FOTO = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp");

    public PerfilService(PerfilRepository perfilRepo,
                         ObraDestacadaRepository obraRepo,
                         UsuarioRepository usuarioRepo,
                         StorageService storageService) {
        this.perfilRepo = perfilRepo;
        this.obraRepo = obraRepo;
        this.usuarioRepo = usuarioRepo;
        this.storageService = storageService;
    }

    public PerfilResponse obtenerPerfilPropio(UUID usuarioId) {
        Usuario usuario = usuarioRepo.findById(usuarioId)
                .orElseThrow(() -> ApiException.notFound("USUARIO_NO_ENCONTRADO", "Usuario no encontrado"));
        Perfil perfil = perfilRepo.findByUsuarioId(usuarioId)
                .orElseGet(() -> perfilRepo.save(new Perfil(UUID.randomUUID(), usuario, "Artista en ArtaGram", null)));

        return aDto(perfil, true);
    }

    public PerfilResponse obtenerPerfilPublico(String nombreArtista, UUID usuarioActualId) {
        Usuario usuario = usuarioRepo.findByNombreArtistaIgnoreCase(nombreArtista)
                .or(() -> usuarioRepo.findByNombreUsuarioIgnoreCase(nombreArtista))
                .orElseThrow(() -> ApiException.notFound("ARTISTA_NO_ENCONTRADO", "Artista no encontrado"));

        Perfil perfil = perfilRepo.findByUsuarioId(usuario.getId())
                .orElseGet(() -> perfilRepo.save(new Perfil(UUID.randomUUID(), usuario, "Artista en ArtaGram", null)));

        boolean esPropio = usuarioActualId != null && usuarioActualId.equals(usuario.getId());
        return aDto(perfil, esPropio);
    }

    @Transactional
    public PerfilResponse actualizarPerfil(UUID usuarioId, ActualizarPerfilRequest req) {
        Usuario usuario = usuarioRepo.findById(usuarioId)
                .orElseThrow(() -> ApiException.notFound("USUARIO_NO_ENCONTRADO", "Usuario no encontrado"));

        Perfil perfil = perfilRepo.findByUsuarioId(usuarioId)
                .orElseGet(() -> new Perfil(UUID.randomUUID(), usuario, "", null));

        if (req.nombreArtista() != null && !req.nombreArtista().isBlank()) {
            usuario.setNombreArtista(req.nombreArtista().trim());
            usuarioRepo.save(usuario);
        }

        if (req.biografia() != null) {
            perfil.setBiografia(req.biografia());
        }
        if (req.fotoUrl() != null) {
            perfil.setFotoUrl(req.fotoUrl());
        }

        if (req.enlaces() != null) {
            Set<EnlacePerfil> nuevosEnlaces = new HashSet<>();
            for (var e : req.enlaces()) {
                if (e.etiqueta() != null && e.url() != null) {
                    nuevosEnlaces.add(new EnlacePerfil(e.etiqueta(), e.url()));
                }
            }
            perfil.setEnlaces(nuevosEnlaces);
        }

        perfil.setActualizadoEn(Instant.now());
        perfil = perfilRepo.save(perfil);

        if (req.obrasDestacadas() != null) {
            obraRepo.deleteByPerfilId(perfil.getId());
            int orden = 0;
            for (var o : req.obrasDestacadas()) {
                obraRepo.save(new ObraDestacada(UUID.randomUUID(), perfil, o.titulo(), o.imagenUrl(), orden++));
            }
        }

        return aDto(perfil, true);
    }

    /** Sin transaccion: la subida a S3 no debe retener una conexion de base de datos. */
    public String actualizarFoto(UUID usuarioId, MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw ApiException.badRequest("DATOS_INVALIDOS", "La imagen es obligatoria");
        }
        String extension = EXTENSIONES_FOTO.get(archivo.getContentType());
        if (extension == null) {
            throw ApiException.badRequest("FORMATO_NO_SOPORTADO", "Solo se admiten imágenes JPG, PNG o WEBP");
        }
        Usuario usuario = usuarioRepo.findById(usuarioId)
                .orElseThrow(() -> ApiException.notFound("USUARIO_NO_ENCONTRADO", "Usuario no encontrado"));

        String ruta;
        try {
            ruta = storageService.guardar("avatars", "foto" + extension, archivo.getBytes(), archivo.getContentType());
        } catch (IOException ex) {
            throw ApiException.badRequest("ERROR_STORAGE", "No se pudo leer el archivo");
        }

        Perfil perfil = perfilRepo.findByUsuarioId(usuarioId)
                .orElseGet(() -> new Perfil(UUID.randomUUID(), usuario, "Artista en ArtaGram", null));
        perfil.setFotoUrl(ruta);
        perfil.setActualizadoEn(Instant.now());
        perfilRepo.save(perfil);
        return storageService.resolverUrlPublica(ruta);
    }

    private PerfilResponse aDto(Perfil p, boolean esPropio) {
        List<PerfilResponse.EnlaceDto> enlaces = p.getEnlaces().stream()
                .map(e -> new PerfilResponse.EnlaceDto(e.getEtiqueta(), e.getUrl()))
                .toList();

        List<PerfilResponse.ObraDestacadaDto> obras = obraRepo.findByPerfilIdOrderByOrdenAsc(p.getId()).stream()
                .map(o -> new PerfilResponse.ObraDestacadaDto(o.getTitulo(), storageService.resolverUrlPublica(o.getImagenUrl())))
                .toList();

        return new PerfilResponse(
                p.getUsuario().getId(),
                p.getUsuario().getNombreArtista(),
                p.getBiografia(),
                storageService.resolverUrlPublica(p.getFotoUrl()),
                enlaces,
                obras,
                esPropio,
                p.getUsuario().isPremium()
        );
    }
}
