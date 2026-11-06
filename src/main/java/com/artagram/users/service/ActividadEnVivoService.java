package com.artagram.users.service;

import com.artagram.users.domain.ActividadEnVivo;
import com.artagram.users.domain.ActividadEnVivoRepository;
import com.artagram.users.domain.Perfil;
import com.artagram.users.domain.PerfilRepository;
import com.artagram.users.domain.Usuario;
import com.artagram.users.domain.UsuarioRepository;
import com.artagram.users.dto.ArtistaEnVivo;
import com.artagram.users.storage.StorageService;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActividadEnVivoService {

    /** Red de seguridad por si se pierde el evento de fin de sesion. */
    private static final Duration VIGENCIA = Duration.ofHours(2);

    private final ActividadEnVivoRepository actividadRepo;
    private final UsuarioRepository usuarioRepo;
    private final PerfilRepository perfilRepo;
    private final StorageService storageService;

    public ActividadEnVivoService(ActividadEnVivoRepository actividadRepo,
                                  UsuarioRepository usuarioRepo,
                                  PerfilRepository perfilRepo,
                                  StorageService storageService) {
        this.actividadRepo = actividadRepo;
        this.usuarioRepo = usuarioRepo;
        this.perfilRepo = perfilRepo;
        this.storageService = storageService;
    }

    @Transactional
    public void iniciar(String actividadId, UUID usuarioId, String tipo, UUID recursoId) {
        actividadRepo.save(new ActividadEnVivo(actividadId, usuarioId, tipo, recursoId));
    }

    @Transactional
    public void terminar(String actividadId) {
        actividadRepo.deleteById(actividadId);
    }

    /** Cada artista sale una vez; si esta en varios sitios se prefiere el lienzo, que se puede abrir. */
    @Transactional(readOnly = true)
    public List<ArtistaEnVivo> siguiendo(UUID seguidorId) {
        Map<UUID, ActividadEnVivo> porArtista = new LinkedHashMap<>();
        for (ActividadEnVivo a : actividadRepo.buscarDeSeguidos(seguidorId, Instant.now().minus(VIGENCIA))) {
            porArtista.merge(a.getUsuarioId(), a, (actual, otra) -> !actual.esLienzo() && otra.esLienzo() ? otra : actual);
        }
        return porArtista.values().stream().flatMap(a -> aDto(a).stream()).toList();
    }

    private Optional<ArtistaEnVivo> aDto(ActividadEnVivo a) {
        return usuarioRepo.findById(a.getUsuarioId()).map((Usuario u) -> new ArtistaEnVivo(
                u.getId(),
                u.getNombreArtista(),
                perfilRepo.findByUsuarioId(u.getId()).map(Perfil::getFotoUrl).map(storageService::resolverUrlPublica).orElse(null),
                a.getTipo(),
                a.esLienzo() ? a.getRecursoId() : null,
                a.getIniciadaEn().toString()));
    }

    @Scheduled(fixedDelay = 600_000)
    @Transactional
    public void limpiarAntiguas() {
        actividadRepo.borrarAntiguas(Instant.now().minus(VIGENCIA));
    }
}
