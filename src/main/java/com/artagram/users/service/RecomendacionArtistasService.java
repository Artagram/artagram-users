package com.artagram.users.service;

import com.artagram.users.domain.Perfil;
import com.artagram.users.domain.PerfilRepository;
import com.artagram.users.domain.SeguidorRepository;
import com.artagram.users.domain.UsuarioRepository;
import com.artagram.users.dto.ArtistaRecomendado;
import com.artagram.users.storage.StorageService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecomendacionArtistasService {

    private static final int LIMITE_MAXIMO = 20;

    private final SeguidorRepository seguidorRepo;
    private final UsuarioRepository usuarioRepo;
    private final PerfilRepository perfilRepo;
    private final StorageService storageService;

    public RecomendacionArtistasService(SeguidorRepository seguidorRepo,
                                        UsuarioRepository usuarioRepo,
                                        PerfilRepository perfilRepo,
                                        StorageService storageService) {
        this.seguidorRepo = seguidorRepo;
        this.usuarioRepo = usuarioRepo;
        this.perfilRepo = perfilRepo;
        this.storageService = storageService;
    }

    /** Primero los artistas que siguen tus seguidos; si no alcanzan, completa con los mas seguidos. */
    @Transactional(readOnly = true)
    public List<ArtistaRecomendado> recomendar(UUID usuarioId, int limite) {
        int tope = Math.clamp(limite, 1, LIMITE_MAXIMO);
        Map<UUID, Long> candidatos = new LinkedHashMap<>();
        seguidorRepo.buscarSeguidosPorMisSeguidos(usuarioId, PageRequest.of(0, tope))
                .forEach(c -> candidatos.put(c.getArtistaId(), c.getEnComun()));
        if (candidatos.size() < tope) {
            seguidorRepo.buscarMasSeguidos(usuarioId, PageRequest.of(0, tope * 2)).stream()
                    .filter(id -> !candidatos.containsKey(id))
                    .limit(tope - candidatos.size())
                    .forEach(id -> candidatos.put(id, 0L));
        }
        return candidatos.entrySet().stream()
                .flatMap(c -> aDto(c.getKey(), c.getValue()).stream())
                .toList();
    }

    private Optional<ArtistaRecomendado> aDto(UUID artistaId, long enComun) {
        return usuarioRepo.findById(artistaId).map(u -> new ArtistaRecomendado(
                u.getId(),
                u.getNombreArtista(),
                perfilRepo.findByUsuarioId(u.getId()).map(Perfil::getFotoUrl).map(storageService::resolverUrlPublica).orElse(null),
                seguidorRepo.countById_SeguidoId(u.getId()),
                enComun));
    }
}
