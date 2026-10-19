package com.artagram.users.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    private UUID id;

    @Column(name = "nombre_usuario", nullable = false, unique = true, length = 50)
    private String nombreUsuario;

    @Column(name = "nombre_artista", nullable = false, length = 100)
    private String nombreArtista;

    @Column(nullable = false, unique = true, length = 100)
    private String correo;

    @Column(nullable = false)
    private boolean premium = false;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn = Instant.now();

    public Usuario() {
    }

    public Usuario(UUID id, String nombreUsuario, String nombreArtista, String correo, boolean premium) {
        this.id = id != null ? id : UUID.randomUUID();
        this.nombreUsuario = nombreUsuario;
        this.nombreArtista = nombreArtista;
        this.correo = correo;
        this.premium = premium;
        this.creadoEn = Instant.now();
    }

    public UUID getId() { return id; }
    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }
    public String getNombreArtista() { return nombreArtista; }
    public void setNombreArtista(String nombreArtista) { this.nombreArtista = nombreArtista; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public boolean isPremium() { return premium; }
    public void setPremium(boolean premium) { this.premium = premium; }
    public Instant getCreadoEn() { return creadoEn; }
}
