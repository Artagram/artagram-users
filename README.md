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

## Diseño y Arquitectura

Arquitectura en capas clásica (Controller → Service → Repository), con dos
puntos de extensión reales resueltos por interfaz + inyección de
dependencias: almacenamiento de archivos (`StorageProvider`) y pasarela de
pagos (`ProveedorPago`). Los controladores son delgados: resuelven el
usuario autenticado vía `JwtUserResolver` y delegan toda la lógica a su
service correspondiente.

```mermaid
classDiagram
    class PerfilController
    class ArtistaController
    class SuscripcionController
    class MediaController
    class InternalUserController

    class PerfilService
    class SeguidorService
    class SuscripcionService
    class StorageService

    class JwtUserResolver
    class RsaTokenValidator

    class StorageProvider {
        <<interface>>
        +guardar(ruta, contenido)
        +leer(ruta)
        +urlPrefirmadaSubida(ruta, ttl)
    }
    class DiskStorageProvider
    class S3StorageProvider

    class ProveedorPago {
        <<interface>>
        +getNombre()
        +crearPreferencia(usuarioId, plan, retornoUrl)
    }
    class MercadoPagoProvider
    class FakePagoProvider

    class Usuario
    class Perfil
    class Seguidor
    class Suscripcion
    class UsoCuenta

    class UsersEventPublisher
    class UsersEventListener

    PerfilController --> PerfilService
    PerfilController --> JwtUserResolver
    ArtistaController --> SeguidorService
    SuscripcionController --> SuscripcionService
    MediaController --> StorageService
    InternalUserController --> SuscripcionService

    PerfilService --> StorageService
    PerfilService --> Perfil
    PerfilService --> Usuario
    SeguidorService --> Seguidor
    SeguidorService --> UsersEventPublisher
    SuscripcionService --> ProveedorPago
    SuscripcionService --> Suscripcion
    SuscripcionService --> UsoCuenta
    SuscripcionService --> UsersEventPublisher

    StorageService --> StorageProvider
    StorageProvider <|.. DiskStorageProvider
    StorageProvider <|.. S3StorageProvider

    ProveedorPago <|.. MercadoPagoProvider
    ProveedorPago <|.. FakePagoProvider
    MercadoPagoProvider --> FakePagoProvider : fallback si no hay access token

    JwtUserResolver --> RsaTokenValidator
```

### Evaluación SOLID

- **SRP (bien resuelto):** cada service tiene una sola razón de cambio —
  `PerfilService` solo perfil/obras destacadas, `SeguidorService` solo el
  grafo de seguidores, `SuscripcionService` solo plan/cuota/pago,
  `StorageService` solo resolución de rutas/URLs. Los controladores no
  tienen lógica de negocio, solo mapean HTTP → service.
- **OCP / DIP (el punto fuerte del servicio):** `StorageProvider` y
  `ProveedorPago` son interfaces reales inyectadas por constructor; agregar
  un proveedor nuevo (p. ej. Azure Blob o Stripe) no requiere tocar
  `StorageService` ni `SuscripcionService`, solo implementar la interfaz y
  registrar el bean. Esto es Open/Closed e Inversión de Dependencias
  aplicados correctamente, no solo en el papel.
- **OCP (mejorable):** `MercadoPagoProvider` no es una estrategia pura —
  internamente decide si delega a `FakePagoProvider` según si hay
  `access-token` configurado, en vez de que Spring elija el bean correcto
  vía `@ConditionalOnProperty`/perfiles. Funciona, pero mezclar la
  implementación "real" con el fallback en la misma clase hace que agregar
  un tercer proveedor implique tocar esta clase, no solo añadir una nueva.
  También `SuscripcionService.procesarPagoAprobado` fija los límites del
  plan PRO con literales (`5120`, `500`) en el método: pasar a un enum/tabla
  `Plan` sería más abierto a nuevos niveles sin editar el service.
- **LSP:** sin violaciones observadas — ambas implementaciones de
  `StorageProvider` y `ProveedorPago` son sustituibles sin que el llamador
  conozca cuál está activa.
- **ISP:** `StorageProvider` usa métodos `default` para las URLs
  prefirmadas (`urlPrefirmadaSubida/Lectura`), así `DiskStorageProvider` no
  está obligado a implementar algo que no soporta. Buen uso de ISP vía
  defaults en lugar de forzar una interfaz más chica aparte.

### Preparación para AWS

- **Base de datos:** `db_usuarios` en PostgreSQL → migrar a **RDS for
  PostgreSQL** (o Aurora PostgreSQL si se busca failover automático). Las
  variables `DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD` ya están
  externalizadas, solo hay que apuntarlas al endpoint de RDS.
- **Secretos:** `artagram.pago.mercadopago-access-token` y las credenciales
  de DB deben ir a **AWS Secrets Manager** (o Parameter Store si el costo
  importa más que la rotación automática), nunca a variables de entorno en
  texto plano en la definición de la tarea de ECS/EKS.
- **Almacenamiento:** `S3StorageProvider` ya habla el protocolo S3 nativo
  (hoy contra MinIO); apuntar `S3_ENDPOINT` a `s3.amazonaws.com` (o quitar
  el endpoint custom y dejar que el SDK resuelva la región) es el único
  cambio necesario — no hace falta tocar código.
- **Escalado horizontal:** el servicio es stateless (JWT sin sesión de
  servidor), así que corre bien detrás de un Application Load Balancer con
  múltiples tareas Fargate/EC2; el único estado compartido real es la base
  de datos.
- **Cola de eventos:** `UsersEventPublisher`/`UsersEventListener` usan
  RabbitMQ (`artagram.events`) vía Spring AMQP — en AWS esto es **Amazon MQ
  for RabbitMQ** (cambio de endpoint únicamente) o, si se quiere nativo de
  AWS, migrar a SNS/SQS (cambio de código, no trivial).
