package com.artagram.users.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "obras_destacadas")
public class ObraDestacada {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "perfil_id", nullable = false)
    private Perfil perfil;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(name = "imagen_url", nullable = false, length = 500)
    private String imagenUrl;

    @Column(nullable = false)
    private int orden = 0;

    public ObraDestacada() {
    }

    public ObraDestacada(UUID id, Perfil perfil, String titulo, String imagenUrl, int orden) {
        this.id = id != null ? id : UUID.randomUUID();
        this.perfil = perfil;
        this.titulo = titulo;
        this.imagenUrl = imagenUrl;
        this.orden = orden;
    }

    public UUID getId() { return id; }
    public Perfil getPerfil() { return perfil; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }
    public int getOrden() { return orden; }
    public void setOrden(int orden) { this.orden = orden; }
}
