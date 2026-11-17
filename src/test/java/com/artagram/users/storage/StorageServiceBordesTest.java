package com.artagram.users.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.artagram.users.common.ApiException;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class StorageServiceBordesTest {

    private final StorageProvider provider = mock(StorageProvider.class);
    private final StorageService disco = new StorageService(provider, "http://minio:9000", "", "bucket", "disk");

    @Test
    void guardarLeerYUrls() throws Exception {
        assertThat(disco.guardar("avatars", "a.png", new byte[]{1}, "image/png")).startsWith("avatars/").endsWith(".png");
        assertThat(disco.guardar("avatars", "sinext", new byte[]{1}, "image/png")).doesNotContain(".");
        doThrow(new IOException("lleno")).when(provider).guardar(anyString(), any(), anyLong(), anyString());
        assertThatThrownBy(() -> disco.guardar("a", "a.png", new byte[]{1}, "image/png")).isInstanceOf(ApiException.class);

        when(provider.leer("no")).thenThrow(new IOException("no"));
        assertThatThrownBy(() -> disco.leer("no")).isInstanceOf(ApiException.class);

        when(provider.urlPrefirmadaSubida(anyString(), eq("image/png"), any())).thenReturn(Optional.empty());
        var presign = disco.generarUrlSubida("f.png", "image/png");
        assertThat(presign.uploadUrl()).isEqualTo(presign.publicUrl()).startsWith("/media/avatars/");
        assertThat(disco.generarUrlSubida("sinext", "image/png").publicUrl()).doesNotContain(".");
        assertThat(disco.resolverUrlPublica("")).isNull();
        assertThat(disco.resolverUrlPublica("https://x/y.png")).isEqualTo("https://x/y.png");
    }
}
