package com.artagram.users.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

class StorageServiceTest {

    private String urlPublica(String endpoint) {
        return new StorageService(mock(StorageProvider.class), endpoint, "", "artagram-uploads", "s3")
                .resolverUrlPublica("avatars/a.png");
    }

    @Test
    void sinEndpointLasFotosSalenPorMedia() {
        assertThat(urlPublica("")).isEqualTo("/media/avatars/a.png");
    }

    @Test
    void conEndpointSeUsaLaUrlDelBucket() {
        assertThat(urlPublica("http://localhost:9000")).isEqualTo("http://localhost:9000/artagram-uploads/avatars/a.png");
    }

    @Test
    void elEndpointPublicoReemplazaAlInternoEnLaUrl() {
        String url = new StorageService(mock(StorageProvider.class), "http://minio:9000", "http://localhost:9000",
                "artagram-uploads", "s3").resolverUrlPublica("publicaciones/a.png");

        assertThat(url).isEqualTo("http://localhost:9000/artagram-uploads/publicaciones/a.png");
    }
}
