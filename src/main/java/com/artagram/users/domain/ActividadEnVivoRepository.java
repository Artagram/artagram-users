package com.artagram.users.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActividadEnVivoRepository extends JpaRepository<ActividadEnVivo, String> {

    @Query("""
            SELECT a FROM ActividadEnVivo a
            WHERE a.iniciadaEn > :desde
              AND a.usuarioId IN (SELECT s.id.seguidoId FROM Seguidor s WHERE s.id.seguidorId = :seguidorId)
            ORDER BY a.iniciadaEn DESC
            """)
    List<ActividadEnVivo> buscarDeSeguidos(@Param("seguidorId") UUID seguidorId, @Param("desde") Instant desde);

    @Modifying
    @Query("DELETE FROM ActividadEnVivo a WHERE a.iniciadaEn <= :limite")
    int borrarAntiguas(@Param("limite") Instant limite);
}
