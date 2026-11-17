package com.artagram.users.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.artagram.users.common.ApiException;
import com.artagram.users.common.GlobalExceptionHandler;
import com.artagram.users.common.JwtUserResolver;
import com.artagram.users.common.RsaTokenValidator;
import com.artagram.users.dto.CheckoutResponse;
import com.artagram.users.dto.PresignResponse;
import com.artagram.users.service.ActividadEnVivoService;
import com.artagram.users.service.PerfilService;
import com.artagram.users.service.RecomendacionArtistasService;
import com.artagram.users.service.SeguidorService;
import com.artagram.users.service.SuscripcionService;
import com.artagram.users.storage.StorageService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ControladoresUsersTest {

    private final UUID yo = UUID.randomUUID();
    private RsaTokenValidator validator;
    private SeguidorService seguidores;
    private ActividadEnVivoService actividad;
    private RecomendacionArtistasService recomendaciones;
    private PerfilService perfiles;
    private SuscripcionService suscripciones;
    private StorageService storage;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        validator = mock(RsaTokenValidator.class);
        JwtUserResolver resolver = new JwtUserResolver(validator);
        seguidores = mock(SeguidorService.class);
        actividad = mock(ActividadEnVivoService.class);
        recomendaciones = mock(RecomendacionArtistasService.class);
        perfiles = mock(PerfilService.class);
        suscripciones = mock(SuscripcionService.class);
        storage = mock(StorageService.class);
        mvc = MockMvcBuilders.standaloneSetup(
                        new ArtistaController(seguidores, actividad, recomendaciones, resolver),
                        new PerfilController(perfiles, resolver),
                        new SuscripcionController(suscripciones, resolver),
                        new InternalUserController(suscripciones),
                        new MediaController(storage, resolver))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void artistasConCabecerasDelGateway() throws Exception {
        UUID otro = UUID.randomUUID();
        mvc.perform(get("/api/artistas").param("q", "ana").header("X-User-Id", yo.toString())).andExpect(status().isOk());
        mvc.perform(get("/api/artistas/recomendados").header("X-User-Id", yo.toString()).header("X-User-Name", " "))
                .andExpect(status().isOk());
        mvc.perform(get("/api/artistas/siguiendo/en-vivo").header("X-User-Id", yo.toString())).andExpect(status().isOk());
        mvc.perform(post("/api/artistas/" + otro + "/seguir").header("X-User-Id", yo.toString())).andExpect(status().isOk());
        mvc.perform(get("/api/artistas/" + otro)).andExpect(status().isOk());

        verify(seguidores).buscar("ana", yo);
        verify(recomendaciones).recomendar(yo, 10);
        verify(actividad).siguiendo(yo);
        verify(seguidores).toggleSeguir(yo, otro);
        verify(seguidores).resumenArtista(otro, null);
    }

    @Test
    void perfilConBearerValido() throws Exception {
        Claims claims = Jwts.claims().subject(yo.toString()).add("nombre", "ana").add("rol", "ADMIN").add("premium", true).build();
        when(validator.validarYExtraer("jwt")).thenReturn(claims);
        when(perfiles.actualizarFoto(eq(yo), any())).thenReturn("/media/avatars/f.png");

        mvc.perform(get("/api/perfil/me").header("Authorization", "Bearer jwt")).andExpect(status().isOk());
        mvc.perform(put("/api/perfil/me").header("Authorization", "Bearer jwt").contentType(MediaType.APPLICATION_JSON)
                .content("{\"biografia\":\"hola\"}")).andExpect(status().isOk());
        mvc.perform(multipart("/api/perfil/me/foto").file(new MockMultipartFile("archivo", "f.png", "image/png", new byte[]{1}))
                .header("Authorization", "Bearer jwt")).andExpect(status().isOk())
                .andExpect(jsonPath("$.fotoUrl").value("/media/avatars/f.png"));
        mvc.perform(get("/api/perfiles/ana").header("Authorization", "Bearer jwt")).andExpect(status().isOk());

        verify(perfiles).obtenerPerfilPropio(yo);
        verify(perfiles).obtenerPerfilPublico("ana", yo);
    }

    @Test
    void tokenInvalidoOAusenteEs401() throws Exception {
        when(validator.validarYExtraer("malo")).thenThrow(new IllegalArgumentException("firma"));
        Claims sinNombre = Jwts.claims().subject(yo.toString()).build();
        when(validator.validarYExtraer("sin-nombre")).thenReturn(sinNombre);

        mvc.perform(get("/api/perfil/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/perfil/me").header("Authorization", "Bearer malo")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/perfil/me").header("Authorization", "Basic x")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/perfil/me").header("Authorization", "Bearer sin-nombre")).andExpect(status().isOk());
    }

    @Test
    void suscripcionYPlanInterno() throws Exception {
        when(suscripciones.crearCheckout(eq(yo), any())).thenReturn(new CheckoutResponse("/ok", "tx", "FAKE"));

        mvc.perform(get("/api/suscripcion/actual").header("X-User-Id", yo.toString())).andExpect(status().isOk());
        mvc.perform(post("/api/suscripcion/checkout").header("X-User-Id", yo.toString()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"plan\":\"PRO\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.proveedor").value("FAKE"));
        mvc.perform(post("/api/suscripcion/checkout").header("X-User-Id", yo.toString()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"plan\":\"\"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/suscripcion/confirmar").header("X-User-Id", yo.toString())).andExpect(status().isOk());
        mvc.perform(get("/internal/users/" + yo + "/plan")).andExpect(status().isOk());

        verify(suscripciones).procesarPagoAprobado(yo, "sim_manual");
        verify(suscripciones).obtenerPlanDetalle(yo);
    }

    @Test
    void mediaYErrores() throws Exception {
        when(storage.generarUrlSubida("a.png", "image/png")).thenReturn(new PresignResponse("/up", "/media/a.png", "k"));
        when(storage.leer(any())).thenReturn(new ByteArrayResource(new byte[]{1}));
        when(perfiles.obtenerPerfilPublico(eq("nadie"), any())).thenThrow(ApiException.notFound("ARTISTA_NO_ENCONTRADO", "no"));
        when(perfiles.obtenerPerfilPublico(eq("boom"), any())).thenThrow(new IllegalStateException("x"));

        mvc.perform(post("/api/media/presign").header("X-User-Id", yo.toString()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"filename\":\"a.png\",\"contentType\":\"image/png\"}")).andExpect(status().isOk());
        mvc.perform(get("/media/avatars/a.png")).andExpect(header().string("Content-Type", "image/png"));
        mvc.perform(get("/media/avatars/a.webp")).andExpect(header().string("Content-Type", "image/webp"));
        mvc.perform(get("/media/avatars/a.jpg")).andExpect(header().string("Content-Type", "image/jpeg"));
        mvc.perform(get("/api/perfiles/nadie")).andExpect(status().isNotFound());
        mvc.perform(get("/api/perfiles/boom")).andExpect(status().isInternalServerError());
    }
}
