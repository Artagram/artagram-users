# artagram-users

Microservicio de Gestión de Usuarios, Perfiles Artísticos, Seguidores, Cuotas y Suscripciones para ArtaGram.

## Tecnologías
- Java 21, Spring Boot 3.3.4
- Spring Data JPA + PostgreSQL (`db_usuarios`)
- Spring AMQP (RabbitMQ: publica `follow.created`, `follow.deleted`, `subscription.updated`; consume `user.registered`, `user.deleted`)
- Validación de JWT RS256 con llave pública RSA
- Integración de pagos (MercadoPago / Fake)
- AWS SDK v2 para fotos de perfil / avatars

## Endpoints
- `GET /api/perfil/me`
- `PUT /api/perfil/me`
- `GET /api/perfiles/{nombreArtista}`
- `POST /api/artistas/{id}/seguir`
- `GET /api/artistas/{id}`
- `GET /api/suscripcion/actual`
- `POST /api/suscripcion/checkout`
- `POST /api/suscripcion/confirmar`
- `GET /internal/users/{id}/plan`
- `POST /api/media/presign`
- `GET /media/**`

## Variables de Entorno
Ver `.env.example`.
