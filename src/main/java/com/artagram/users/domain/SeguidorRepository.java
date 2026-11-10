package com.artagram.users.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SeguidorRepository extends JpaRepository<Seguidor, Seguidor.SeguidorId> {

    interface ArtistaEnComun {
        UUID getArtistaId();

        long getEnComun();
    }

    boolean existsById_SeguidorIdAndId_SeguidoId(UUID seguidorId, UUID seguidoId);

    void deleteById_SeguidorIdAndId_SeguidoId(UUID seguidorId, UUID seguidoId);

    long countById_SeguidoId(UUID seguidoId);

    long countById_SeguidorId(UUID seguidorId);

    @Query("SELECT s.id.seguidoId FROM Seguidor s WHERE s.id.seguidorId = :seguidorId")
    List<UUID> findSeguidosIdsBySeguidorId(@Param("seguidorId") UUID seguidorId);

    @Query("""
            SELECT b.id.seguidoId AS artistaId, COUNT(b) AS enComun
            FROM Seguidor a, Seguidor b
            WHERE a.id.seguidorId = :usuarioId
              AND b.id.seguidorId = a.id.seguidoId
              AND b.id.seguidoId <> :usuarioId
              AND b.id.seguidoId NOT IN (SELECT c.id.seguidoId FROM Seguidor c WHERE c.id.seguidorId = :usuarioId)
            GROUP BY b.id.seguidoId
            ORDER BY COUNT(b) DESC, b.id.seguidoId
            """)
    List<ArtistaEnComun> buscarSeguidosPorMisSeguidos(@Param("usuarioId") UUID usuarioId, Pageable pageable);

    @Query("""
            SELECT u.id
            FROM Usuario u LEFT JOIN Seguidor s ON s.id.seguidoId = u.id
            WHERE u.id <> :usuarioId
              AND u.id NOT IN (SELECT c.id.seguidoId FROM Seguidor c WHERE c.id.seguidorId = :usuarioId)
            GROUP BY u.id
            ORDER BY COUNT(s) DESC, u.id
            """)
    List<UUID> buscarMasSeguidos(@Param("usuarioId") UUID usuarioId, Pageable pageable);
}
