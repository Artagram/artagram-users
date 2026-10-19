package com.artagram.users.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ObraDestacadaRepository extends JpaRepository<ObraDestacada, UUID> {
    List<ObraDestacada> findByPerfilIdOrderByOrdenAsc(UUID perfilId);
    void deleteByPerfilId(UUID perfilId);
}
