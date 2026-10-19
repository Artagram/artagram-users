package com.artagram.users.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SuscripcionRepository extends JpaRepository<Suscripcion, UUID> {
    Optional<Suscripcion> findTopByUsuarioIdOrderByFechaInicioDesc(UUID usuarioId);
}
