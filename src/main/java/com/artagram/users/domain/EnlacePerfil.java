package com.artagram.users.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class EnlacePerfil {

    @Column(nullable = false, length = 50)
    private String etiqueta;

    @Column(nullable = false, length = 500)
    private String url;

    public EnlacePerfil() {
    }

    public EnlacePerfil(String etiqueta, String url) {
        this.etiqueta = etiqueta;
        this.url = url;
    }

    public String getEtiqueta() { return etiqueta; }
    public String getUrl() { return url; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EnlacePerfil that)) return false;
        return Objects.equals(etiqueta, that.etiqueta) && Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(etiqueta, url);
    }
}
