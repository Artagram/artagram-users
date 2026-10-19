package com.artagram.users.seed;

import com.artagram.users.domain.EnlacePerfil;
import com.artagram.users.domain.ObraDestacada;
import com.artagram.users.domain.ObraDestacadaRepository;
import com.artagram.users.domain.Perfil;
import com.artagram.users.domain.PerfilRepository;
import com.artagram.users.domain.Seguidor;
import com.artagram.users.domain.SeguidorRepository;
import com.artagram.users.domain.Suscripcion;
import com.artagram.users.domain.SuscripcionRepository;
import com.artagram.users.domain.UsoCuenta;
import com.artagram.users.domain.UsoCuentaRepository;
import com.artagram.users.domain.Usuario;
import com.artagram.users.domain.UsuarioRepository;
import com.artagram.users.storage.StorageService;
import java.io.InputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
@Profile("seed")
public class UsersDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(UsersDataSeeder.class);

    private record UserSeed(UUID id, String user, String name, String email, boolean premium, String bio, String avatar) {}

    private final UsuarioRepository usuarioRepo;
    private final PerfilRepository perfilRepo;
    private final ObraDestacadaRepository obraRepo;
    private final SeguidorRepository seguidorRepo;
    private final SuscripcionRepository subRepo;
    private final UsoCuentaRepository usoRepo;
    private final StorageService storageService;
    private final ResourceLoader resourceLoader;

    public UsersDataSeeder(UsuarioRepository usuarioRepo,
                           PerfilRepository perfilRepo,
                           ObraDestacadaRepository obraRepo,
                           SeguidorRepository seguidorRepo,
                           SuscripcionRepository subRepo,
                           UsoCuentaRepository usoRepo,
                           StorageService storageService,
                           ResourceLoader resourceLoader) {
        this.usuarioRepo = usuarioRepo;
        this.perfilRepo = perfilRepo;
        this.obraRepo = obraRepo;
        this.seguidorRepo = seguidorRepo;
        this.subRepo = subRepo;
        this.usoRepo = usoRepo;
        this.storageService = storageService;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void run(String... args) {
        log.info("Iniciando seed de datos para artagram-users...");

        List<UserSeed> seeds = List.of(
            new UserSeed(UUID.fromString("a1b2c3d4-0000-4000-8000-000000000001"), "lucia_acuarela", "Lucía Mora", "lucia.mora@correo.com", false, "Acuarelista. Pinto jardines y luz de tarde. Bogotá 🌿", "avatar_0.jpg"),
            new UserSeed(UUID.fromString("a1b2c3d4-0000-4000-8000-000000000002"), "sam_pixel", "Sam Rivera", "sam.rivera@correo.com", true, "Arte digital y pixel art. Ex-diseñador de videojuegos.", "avatar_1.jpg"),
            new UserSeed(UUID.fromString("a1b2c3d4-0000-4000-8000-000000000003"), "noa_tinta", "Noa Delgado", "noa.delgado@correo.com", false, "Tinta china y retrato. Publico el proceso completo siempre.", "avatar_2.jpg"),
            new UserSeed(UUID.fromString("a1b2c3d4-0000-4000-8000-000000000004"), "ivan_urbano", "Iván Rueda", "ivan.rueda@correo.com", false, "Muralista. Me interesa el arte colaborativo en espacios públicos.", "avatar_3.jpg"),
            new UserSeed(UUID.fromString("a1b2c3d4-0000-4000-8000-000000000005"), "maria_paz_art", "María Paz", "maria.paz@correo.com", false, "Ilustradora freelance. Talleres los sábados.", "avatar_4.jpg"),
            new UserSeed(UUID.fromString("a1b2c3d4-0000-4000-8000-000000000006"), "diego_grabado", "Diego León", "diego.leon@correo.com", false, "Grabado y xilografía. Lento pero seguro.", "avatar_5.jpg"),
            new UserSeed(UUID.fromString("a1b2c3d4-0000-4000-8000-000000000007"), "pincel_artagram", "Curador ArtaGram", "prueba.artagram@correo.com", true, "Curador oficial de la comunidad ArtaGram. 🎨", "avatar_0.jpg"),
            new UserSeed(UUID.fromString("a1b2c3d4-0001-4000-8000-000000000001"), "camila_oleo", "Camila Oleo", "camila.oleo@correo.com", false, "Pintora al óleo sobre lienzo. Retratos figurativos y texturas empastadas. 🎨", "avatar_0.jpg"),
            new UserSeed(UUID.fromString("a1b2c3d4-0002-4000-8000-000000000002"), "mateo_3d", "Mateo 3D", "mateo.3d@correo.com", false, "Modelado orgánico, renders conceptuales y escultura digital. ✨", "avatar_1.jpg"),
            new UserSeed(UUID.fromString("a1b2c3d4-0003-4000-8000-000000000003"), "valeria_botanica", "Valeria Botánica", "valeria.botanica@correo.com", false, "Ilustración científica y botánica en acuarela y gouache. Flores de los Andes. 🌸", "avatar_2.jpg"),
            new UserSeed(UUID.fromString("a1b2c3d4-0004-4000-8000-000000000004"), "andres_concept", "Andrés Concept", "andres.concept@correo.com", false, "Concept artist para animación y videojuegos. Creación de mundos sci-fi. 🚀", "avatar_3.jpg"),
            new UserSeed(UUID.fromString("a1b2c3d4-0005-4000-8000-000000000005"), "elena_collage", "Elena Collage", "elena.collage@correo.com", false, "Técnicas mixtas, collage análogo y fanzines independientes. Experimentación visual. ✂️", "avatar_4.jpg")
        );

        for (var s : seeds) {
            Usuario u = usuarioRepo.findById(s.id()).orElseGet(() -> {
                Usuario nuevo = new Usuario(s.id(), s.user(), s.name(), s.email(), s.premium());
                return usuarioRepo.save(nuevo);
            });

            if (!perfilRepo.findByUsuarioId(u.getId()).isPresent()) {
                String fotoUrl = guardarAvatarSiExiste(s.avatar());
                Perfil p = new Perfil(UUID.randomUUID(), u, s.bio(), fotoUrl);
                p.setEnlaces(Set.of(new EnlacePerfil("Portfolio", "https://" + s.user() + ".artagram.local")));
                p = perfilRepo.save(p);

                obraRepo.save(new ObraDestacada(UUID.randomUUID(), p, "Destacada 1", fotoUrl, 0));
            }

            if (!usoRepo.findByUsuarioId(u.getId()).isPresent()) {
                long maxMb = s.premium() ? 5120 : 512;
                int maxObras = s.premium() ? 500 : 50;
                usoRepo.save(new UsoCuenta(UUID.randomUUID(), u.getId(), 25, maxMb, 2, maxObras));
            }

            if (s.premium() && !subRepo.findTopByUsuarioIdOrderByFechaInicioDesc(u.getId()).isPresent()) {
                subRepo.save(new Suscripcion(UUID.randomUUID(), u.getId(), "PRO", "ACTIVA", Instant.now(), Instant.now().plus(365, ChronoUnit.DAYS), "STRIPE_MOCK", "sub_seed_demo"));
            }
        }

        UUID luciaId = UUID.fromString("a1b2c3d4-0000-4000-8000-000000000001");
        UUID samId = UUID.fromString("a1b2c3d4-0000-4000-8000-000000000002");
        if (!seguidorRepo.existsById_SeguidorIdAndId_SeguidoId(luciaId, samId)) {
            seguidorRepo.save(new Seguidor(luciaId, samId));
        }
        if (!seguidorRepo.existsById_SeguidorIdAndId_SeguidoId(samId, luciaId)) {
            seguidorRepo.save(new Seguidor(samId, luciaId));
        }

        log.info("Seed de datos para artagram-users completado exitosamente.");
    }

    private String guardarAvatarSiExiste(String nombre) {
        try {
            Resource r = resourceLoader.getResource("classpath:seed/images/" + nombre);
            if (r.exists()) {
                try (InputStream is = r.getInputStream()) {
                    byte[] bytes = is.readAllBytes();
                    return storageService.guardar("avatars", nombre, bytes, "image/jpeg");
                }
            }
        } catch (Exception ex) {
            log.warn("No se pudo cargar avatar {}: {}", nombre, ex.getMessage());
        }
        return "avatars/" + nombre;
    }
}
