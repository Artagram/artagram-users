package com.artagram.users.service;

import com.artagram.users.common.ApiException;
import com.artagram.users.domain.Seguidor;
import com.artagram.users.domain.SeguidorRepository;
import com.artagram.users.domain.Usuario;
import com.artagram.users.domain.UsuarioRepository;
import com.artagram.users.dto.ArtistaResumen;
import com.artagram.users.dto.Seguimiento;
import com.artagram.users.events.UsersEventPublisher;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeguidorService {

    private final SeguidorRepository seguidorRepo;
    private final UsuarioRepository usuarioRepo;
    private final UsersEventPublisher eventPublisher;

    public SeguidorService(SeguidorRepository seguidorRepo,
                           UsuarioRepository usuarioRepo,
                           UsersEventPublisher eventPublisher) {
        this.seguidorRepo = seguidorRepo;
        this.usuarioRepo = usuarioRepo;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Seguimiento toggleSeguir(UUID seguidorId, UUID seguidoId) {
        if (seguidorId.equals(seguidoId)) {
            throw ApiException.badRequest("SEGUIDOR_INVALIDO", "No puedes seguirte a ti mismo");
        }

        boolean yaSigue = seguidorRepo.existsById_SeguidorIdAndId_SeguidoId(seguidorId, seguidoId);
        boolean nuevoEstadoLoSigo;

        if (yaSigue) {
            seguidorRepo.deleteById_SeguidorIdAndId_SeguidoId(seguidorId, seguidoId);
            nuevoEstadoLoSigo = false;
            eventPublisher.publicarFollowEliminado(seguidorId, seguidoId);
        } else {
            seguidorRepo.save(new Seguidor(seguidorId, seguidoId));
            nuevoEstadoLoSigo = true;
            eventPublisher.publicarFollowCreado(seguidorId, seguidoId);
        }

        long total = seguidorRepo.countById_SeguidoId(seguidoId);
        return new Seguimiento(nuevoEstadoLoSigo, total);
    }

    public ArtistaResumen resumenArtista(UUID artistaId, UUID solicitanteId) {
        Usuario usuario = usuarioRepo.findById(artistaId)
                .orElseThrow(() -> ApiException.notFound("ARTISTA_NO_ENCONTRADO", "Artista no encontrado"));

        boolean loSigo = solicitanteId != null && seguidorRepo.existsById_SeguidorIdAndId_SeguidoId(solicitanteId, artistaId);
        long total = seguidorRepo.countById_SeguidoId(artistaId);

        return new ArtistaResumen(usuario.getId(), usuario.getNombreArtista(), loSigo, total);
    }
}
