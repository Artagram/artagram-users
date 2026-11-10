package com.artagram.users.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Una fila por suscripcion abierta: la misma persona puede tener varias pestanas. */
@Entity
@Table(name = "actividad_en_vivo")
public class ActividadEnVivo {

    public static final String TIPO_LIENZO = "LIENZO";

    @Id
    private String id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(name = "recurso_id", nullable = false)
    private UUID recursoId;

    @Column(name = "iniciada_en", nullable = false)
    private Instant iniciadaEn;

    protected ActividadEnVivo() {
    }

    public ActividadEnVivo(String id, UUID usuarioId, String tipo, UUID recursoId) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.tipo = tipo;
        this.recursoId = recursoId;
        this.iniciadaEn = Instant.now();
    }

    public UUID getUsuarioId() { return usuarioId; }
    public String getTipo() { return tipo; }
    public UUID getRecursoId() { return recursoId; }
    public Instant getIniciadaEn() { return iniciadaEn; }
    public boolean esLienzo() { return TIPO_LIENZO.equals(tipo); }
}
