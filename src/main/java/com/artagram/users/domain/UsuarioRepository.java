package com.artagram.users.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    List<Usuario> findTop20ByNombreArtistaContainingIgnoreCaseAndIdNotOrderByNombreArtistaAsc(String texto, UUID excluido);
    Optional<Usuario> findByNombreArtistaIgnoreCase(String nombreArtista);
    Optional<Usuario> findByNombreUsuarioIgnoreCase(String nombreUsuario);
    Optional<Usuario> findByCorreoIgnoreCase(String correo);
}
