package com.artagram.users.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SeguidorRepository extends JpaRepository<Seguidor, Seguidor.SeguidorId> {

    boolean existsById_SeguidorIdAndId_SeguidoId(UUID seguidorId, UUID seguidoId);

    void deleteById_SeguidorIdAndId_SeguidoId(UUID seguidorId, UUID seguidoId);

    long countById_SeguidoId(UUID seguidoId);

    long countById_SeguidorId(UUID seguidorId);

    @Query("SELECT s.id.seguidoId FROM Seguidor s WHERE s.id.seguidorId = :seguidorId")
    List<UUID> findSeguidosIdsBySeguidorId(@Param("seguidorId") UUID seguidorId);
}
