# Cinema Ciencias Gateway Service

**API Gateway** y enrutador principal de Cinema Ciencias. Expone una única puerta de
entrada en el puerto **9000** y enruta el tráfico hacia los microservicios internos.
Se registra en **Eureka**, **valida el JWT** de todas las rutas protegidas (patrón *Trust the
Gateway*) y enruta de forma dinámica (`lb://`) hacia el monolito.

## Stack

| Capa | Tecnología |
|---|---|
| Lenguaje | Kotlin 2.2.21 |
| Framework | Spring Boot 3.4.3 + Spring Cloud Gateway MVC (servlet) |
| Service discovery | Netflix Eureka Client (Spring Cloud 2024.0.3) |
| JWT | jjwt 0.13 (solo valida; el Auth Service es quien emite) |
| Config | Spring Cloud Config Client (import opcional) |
| Build | Gradle 9.3.1 (wrapper) |
| Base de datos | Ninguna |
| Puerto | **9000** |

## Prerequisitos

- **JDK 21** instalado y disponible en `JAVA_HOME`
- **Registry Service** corriendo en `http://localhost:8761` (Eureka)
- **Auth Service** corriendo en `http://localhost:8082` (destino de la ruta)

## Puesta en marcha

### 1. Levantar las dependencias

Desde sus carpetas respectivas:

```bash
# Registry Service (8761)
cd ../cinema_ciencias_registry_service && ./gradlew bootRun

# Auth Service (8082)
cd ../cinema_ciencias_auth_service && ./gradlew bootRun
```

### 2. Ejecutar el gateway

```bash
./gradlew bootRun
```

El gateway queda escuchando en **http://localhost:9000**.

> El import del config server es `optional:configserver:http://localhost:8888`:
> si el Config Service no está corriendo, el gateway igual arranca (falla en
> silencio).

## Verificación

A través del gateway, el login del auth service:

```bash
curl -X POST http://localhost:9000/api/v1/cinema/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"pepe","password":"secreto123"}'
```

También podés ver el estado del registro en Eureka:
http://localhost:8761/eureka/ → el gateway debería aparecer como `GATEWAY-SERVICE`. Si en
`.run/logs/gateway.log` ves `Unable to set redirect follow using reflection`, el cliente de Eureka
no se está registrando (ver *Estado conocido* en el README raíz).

## Trust the Gateway

`JwtTrustFilter` es un filtro de servlet que corre antes del enrutamiento:

- Rutas **públicas**: `/api/v1/cinema/auth/**` y `POST /api/v1/cinema/user`.
- Cualquier otra ruta exige `Authorization: Bearer <JWT>`. Si el token falta, es inválido o expiró,
  responde `401` y la petición no sale del gateway.
- Si es válido, **descarta** cualquier header `X-User-*` / `X-Gateway-Secret` que haya mandado el
  cliente y agrega los propios: `X-User-Id`, `X-User-Name`, `X-User-Roles` (a partir de los claims
  `userId` y `roles` del JWT) y `X-Gateway-Secret`.
- `cinema.security.jwtSecret` y `cinema.security.gatewaySecret` los sirve el Config Server
  (`shared-config/application.yml`).

Prueba rápida: `curl -i http://localhost:9000/api/v1/cinema/theater` → `401`.

## Rutas

| Método | Ruta | Destino | Fuente |
|---|---|---|---|
| * | `/api/v1/cinema/auth/**` | `http://localhost:8082` | `application.yaml` |
| POST | `/api/v1/cinema/auth/**` | `http://localhost:8082` | `GatewayConfig.kt` (programática) |
| * | `/api/v1/cinema/user/**` | `http://localhost:8082` (Auth Service) | `GatewayConfig.kt` |
| * | `/api/v1/cinema/{theater,movie-screening,ticket,reservation}/**` | `lb://cinema-ciencias` (Eureka) | `application.yaml` (una ruta por recurso) |

> Gateway MVC 4.2 **no** trae *discovery locator*: el bloque `spring.cloud.gateway.discovery.locator`
> no tiene efecto. Las rutas dinámicas se declaran explícitamente con `lb://`. La ruta hacia Auth
> es estática porque su nombre en Eureka lleva guiones bajos (`cinema_ciencias_auth_service`) y
> `lb://` no los admite en el host.

> **Nota**: la ruta `auth-service-route` está definida **dos veces** (declarativa en
> YAML y programática en `GatewayConfig.kt`). Es redundante y puede causar
> comportamiento inesperado; conviene dejar solo la declarativa.

## Logging

El `GatewayLoggingFilter` registra el tráfico de las rutas de login
(`/auth/login`) con timestamps. El nivel de logging de Spring Web/Gateway está en
DEBUG/TRACE y se sirve desde el Config Server (`shared-config/gateway-service.yml`).

## Tests

```bash
./gradlew test
```

(Solo hay un smoke test de contexto.)

## Notas

- El registro en Eureka apunta a `http://localhost:8761/eureka/`.
- CORS: el auth service permite el origen `http://localhost:9000` (este gateway).