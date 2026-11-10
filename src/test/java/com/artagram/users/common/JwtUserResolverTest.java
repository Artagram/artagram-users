package com.artagram.users.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class JwtUserResolverTest {

    private final JwtUserResolver resolver = new JwtUserResolver(mock(RsaTokenValidator.class));

    @Test
    void leeElRolYElNombreConLasCabecerasDelGateway() {
        UUID id = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", id.toString());
        request.addHeader("X-User-Role", "ADMIN");
        request.addHeader("X-User-Name", "sam_pixel");

        JwtUserResolver.UsuarioAutenticado usuario = resolver.resolverOpcional(request).orElseThrow();

        assertThat(usuario.id()).isEqualTo(id);
        assertThat(usuario.roles()).isEqualTo("ADMIN");
        assertThat(usuario.nombre()).isEqualTo("sam_pixel");
    }

    @Test
    void sinNombreUsaElGenerico() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", UUID.randomUUID().toString());
        request.addHeader("X-User-Name", "");

        assertThat(resolver.resolverOpcional(request).orElseThrow().nombre()).isEqualTo("artista");
    }
}
