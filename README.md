# Pedidos360 - DSY1107 EV2

## 1. Objetivo

Pedidos360 amplía la aplicación cloud-native con integración asíncrona mediante RabbitMQ. El backend Spring Boot persiste solicitudes en PostgreSQL y publica eventos de creación y decisión. Los servicios de notificaciones y documentos procesan esos eventos de forma independiente.

## 2. Arquitectura

```text
Angular
  |
  +--> Cognito OAuth2 Authorization Code + PKCE
          |
          v
      API Gateway (JWT y scopes)
          |
          v
      Backend Spring Boot
          |
          +--> PostgreSQL
          |
          +--> RabbitMQ topic exchange
                  |
                  +--> Cola de notificaciones --> Notificaciones
                  |
                  +--> Cola de documentos ------> Documentos
                  |
                  +--> DLX --> DLQ de cada consumidor

Rabbit Admin (microservicio REST administrativo independiente)
```

Rabbit Admin administra recursos RabbitMQ mediante la conexión del broker; no forma parte del flujo de solicitudes.

## 3. Topología RabbitMQ

Los nombres se configuran bajo `pedidos360.messaging` en `backend/src/main/resources/application.yml` y se consumen por las aplicaciones mediante sus propiedades de mensajería.

| Recurso | Nombre |
| --- | --- |
| Topic exchange principal | `pedidos360.solicitudes.events` |
| Topic exchange de dead letters | `pedidos360.dlx` |
| Cola principal de notificaciones | `pedidos360.notificaciones.queue` |
| Cola principal de documentos | `pedidos360.documentos.queue` |
| DLQ de notificaciones | `pedidos360.notificaciones.dlq` |
| DLQ de documentos | `pedidos360.documentos.dlq` |

| Exchange | Routing key | Cola destino |
| --- | --- | --- |
| `pedidos360.solicitudes.events` | `solicitud.creada` | `pedidos360.notificaciones.queue` |
| `pedidos360.solicitudes.events` | `solicitud.aprobada` | `pedidos360.notificaciones.queue` |
| `pedidos360.solicitudes.events` | `solicitud.rechazada` | `pedidos360.notificaciones.queue` |
| `pedidos360.solicitudes.events` | `solicitud.aprobada` | `pedidos360.documentos.queue` |
| `pedidos360.dlx` | `notificaciones.dead` | `pedidos360.notificaciones.dlq` |
| `pedidos360.dlx` | `documentos.dead` | `pedidos360.documentos.dlq` |

La topología se declara con beans `TopicExchange`, `Queue` y `Binding` en `backend/src/main/java/cl/duoc/dsy1107/pedidos360/messaging/config/RabbitTopologyConfig.java`; cada consumidor también declara sus propios recursos en su clase `Rabbit*Config`. Las colas operativas y las DLQ son **quorum queues** durables. Los exchanges son durables.

## 4. Flujo de eventos

- Al crear una solicitud, el backend publica `solicitud.creada`. La recibe Notificaciones.
- Al aprobar una solicitud, el backend publica `solicitud.aprobada`. La reciben Notificaciones y Documentos; Documentos procesa las solicitudes aprobadas.
- Al rechazar una solicitud, el backend publica `solicitud.rechazada`. La recibe Notificaciones.

El productor está en `backend/src/main/java/cl/duoc/dsy1107/pedidos360/messaging/producer/RabbitSolicitudEventPublisher.java`; los consumidores son `NotificationConsumer` y `DocumentConsumer` en los respectivos microservicios.

## 5. Confiabilidad y entrega

- Los consumidores usan ACK manual y confirman un evento después de procesarlo correctamente.
- El prefetch del listener está configurado en `1`.
- Los errores recuperables reintentan hasta 3 veces en cada consumidor, con espera inicial de 250 ms y multiplicador 2.0.
- Los errores no recuperables, inesperados o agotados los reintentos se rechazan con `NACK` y `requeue=false`; la configuración de la cola enruta el mensaje por `pedidos360.dlx` a su DLQ correspondiente.
- Las colas y exchanges principales son durables y las colas usan replicación quorum.
- El productor publica con `RabbitTemplate.convertAndSend`. El código de aplicación no asigna explícitamente el modo de entrega del mensaje; por ello esta documentación no afirma una configuración explícita de persistencia por mensaje.

## 6. Clúster RabbitMQ

### 6.1. LOCAL VERIFIED FAILOVER

`rabbitmq/docker-compose.yml` define tres nodos RabbitMQ 4 (`rabbitmq-1`, `rabbitmq-2` y `rabbitmq-3`), con volúmenes de datos independientes, healthchecks y puertos AMQP/administración separados. `rabbitmq/rabbitmq.conf` configura el descubrimiento de los tres nodos. Las colas quorum usan consenso Raft y una mayoría de sus miembros; con tres miembros, una cola puede mantener mayoría ante la pérdida de un nodo.

En una prueba controlada y temporal de una quorum queue de tres miembros, el líder inicial `rabbit@rabbitmq-2` se detuvo. Se eligió automáticamente `rabbit@rabbitmq-3`; los nodos 1 y 3 mantuvieron quorum y el mensaje siguió disponible. Después se reinició el nodo 2, volvió a estar saludable y el clúster quedó nuevamente con tres nodos. El mensaje continuó disponible y la cola temporal se eliminó al finalizar la prueba. Esta evidencia corresponde a una cola temporal de prueba, no a una intervención sobre colas de producción.

### 6.2. AWS DEPLOYMENT DESIGN

La verificación de AWS del 2026-10-18 confirmó la instancia EC2 `i-08654b87974c0af16` en `us-east-1a` y tres contenedores RabbitMQ en ejecución sobre ese único host. Docker quedó activo tras una recuperación manual por SSM. Los tres contenedores respondieron a `rabbitmq-diagnostics -q ping`; `rabbit@rabbitmq-1`, `rabbit@rabbitmq-2` y `rabbit@rabbitmq-3` estaban registrados como Disk Nodes y Running Nodes. Se observó RabbitMQ 4.3.6 con Erlang 27.3.4.19, sin alarmas ni particiones de red y con `quorum_queue` habilitado.

Los tres nodos RabbitMQ están en una sola instancia EC2. Esto verifica clustering y recuperación a nivel de nodo/proceso; **no** da alta disponibilidad ante la pérdida completa del host EC2.

### 6.3. AWS VERIFIED END-TO-END

El 2026-10-18 se verificó el flujo cloud completo con la solicitud `#1` (`PERMISO EV2`, motivo `Prueba E2E RabbitMQ`), desde estado inicial `PENDIENTE` hasta estado final `APROBADA`. El backend publicó `solicitud.creada` y `solicitud.aprobada`; Notificaciones procesó ambos eventos y confirmó cada uno con ACK. Documentos procesó la aprobación, generó el documento y confirmó el evento con ACK. El E2E cloud finalizó como exitoso.

La primera lectura de CloudWatch mostró únicamente el evento de creación debido al tiempo de ingestión de logs. Una revalidación posterior, de solo lectura, confirmó el evento de aprobación y los ACK de todos los consumidores. Al finalizar, las cuatro colas estaban vacías:

| Cola | Tipo | Mensajes | Consumidores |
| --- | --- | ---: | ---: |
| `pedidos360.notificaciones.queue` | quorum | 0 | 1 |
| `pedidos360.notificaciones.dlq` | quorum | 0 | 0 |
| `pedidos360.documentos.queue` | quorum | 0 | 1 |
| `pedidos360.documentos.dlq` | quorum | 0 | 0 |

La autenticación Cognito PKCE y los roles/scopes también se verificaron visualmente en la aplicación cloud: Solicitante (`solicitantes`, `solicitudes/read`, `solicitudes/write`) y Aprobador (`aprobadores`, `solicitudes/read`, `solicitudes/approve`). Esta evidencia de aplicación cloud es independiente de la prueba local de failover de tres nodos.

## 7. Rabbit Admin REST API

El controlador tiene prefijo `/admin` y expone:

| Método | Ruta | Acción |
| --- | --- | --- |
| `POST` | `/admin/queues` | Crear cola |
| `PUT` | `/admin/queues/{name}` | Actualizar cola |
| `DELETE` | `/admin/queues/{name}` | Eliminar cola |
| `POST` | `/admin/exchanges` | Crear exchange |
| `PUT` | `/admin/exchanges/{name}` | Actualizar exchange |
| `DELETE` | `/admin/exchanges/{name}` | Eliminar exchange |
| `POST` | `/admin/bindings` | Crear binding |
| `DELETE` | `/admin/bindings` | Eliminar binding |

Las rutas corresponden a `microservices/rabbit-admin-service/src/main/java/cl/duoc/dsy1107/pedidos360/rabbitadmin/controller/RabbitAdminController.java`. Los DTOs validan nombres, tipos y campos. RabbitAdminService crea colas durables quorum y exchanges durables. Para actualizar una cola, exige que exista, tenga cero mensajes y cero consumidores, valida que el nombre del cuerpo coincida con el de la ruta y luego la elimina y recrea. Las operaciones fallidas se traducen a errores de dominio/API.

## 8. Seguridad

La autenticación de la SPA usa Amazon Cognito con OAuth2 Authorization Code + PKCE. La Lambda de pre-token generation agrega scopes al access token según los grupos funcionales `solicitantes` y `aprobadores`; el frontend interpreta los grupos y scopes recibidos y envía el token como Bearer a la API. La autorización del backend combina los grupos (`SOLICITANTES` / `APROBADORES`, normalizados desde `cognito:groups`) con los scopes `solicitudes/read`, `solicitudes/write` y `solicitudes/approve` mediante `@PreAuthorize`. El API Gateway valida JWT/scopes antes de enrutar al backend. El backend configura validación JWT como resource server y valida issuer/audience. La asociación de grupos y scopes está en `terraform/lambda/user-token-ms/index.mjs`.

## 9. Frontend Angular

El frontend usa Angular 20 y presenta vistas para solicitantes (CRUD de solicitudes) y aprobadores (decisiones). La configuración runtime se carga desde `frontend/public/config.json`; el archivo versionado contiene valores vacíos seguros salvo el redirect local `http://localhost:4200/`. No deben incorporarse credenciales reales al repositorio. Para ejecutar localmente, sustituya los valores de configuración en su copia de trabajo y no confirme valores de entorno real.

## 10. Pruebas automatizadas

Los conteos siguientes son los resultados de `mvn -B clean verify` o Karma en esta entrega local; el total corresponde a las pruebas ejecutadas en esos comandos.

| Aplicación | Pruebas |
| --- | ---: |
| Backend | 7 |
| Notificaciones | 4 |
| Documentos | 4 |
| Rabbit Admin | 10 |
| Angular | 8 |
| **Total** | **33** |

## 11. Ejecución local

Requisitos: Java 17, Maven, Node.js 22, npm, Docker Compose y PostgreSQL local en `localhost:5432` para el backend. El backend define valores locales por defecto en su `application.yml`; configure localmente el acceso a PostgreSQL y RabbitMQ según su entorno. El servicio RabbitMQ usa credenciales desde `rabbitmq/.env` y no se debe compartir su contenido.

Desde PowerShell, en la raíz:

```powershell
# Crear la configuración local de RabbitMQ desde el ejemplo seguro (una sola vez).
if (-not (Test-Path rabbitmq\.env)) {
    Copy-Item rabbitmq\.env.example rabbitmq\.env
} else {
    Write-Output "rabbitmq\.env ya existe; se conserva sin cambios."
}
Set-Location rabbitmq
docker compose up -d
Set-Location ..
```

Ejecute cada servicio Spring Boot en una terminal separada desde su directorio:

```powershell
Set-Location backend
mvn spring-boot:run
```

```powershell
Set-Location microservices\notificaciones-service
mvn spring-boot:run
```

```powershell
Set-Location microservices\documentos-service
mvn spring-boot:run
```

```powershell
Set-Location microservices\rabbit-admin-service
mvn spring-boot:run
```

Para Angular, configure los valores runtime locales en `frontend/public/config.json` sin confirmar datos reales y ejecute:

```powershell
Set-Location frontend
npm ci
npm start
```

La SPA queda disponible por defecto en `http://localhost:4200/`. Los puertos de los servicios Java y de la consola RabbitMQ se describen en sus `application.yml` y `rabbitmq/docker-compose.yml`.

## 12. Estructura del repositorio

| Ruta | Contenido |
| --- | --- |
| `backend/` | API Spring Boot, persistencia PostgreSQL, seguridad JWT y productor de eventos |
| `frontend/` | Aplicación Angular 20 |
| `microservices/` | Consumidores de notificaciones/documentos y API Rabbit Admin |
| `rabbitmq/` | Clúster Docker Compose de tres nodos y configuración RabbitMQ |
| `terraform/` | Infraestructura como código para recursos cloud |

## 13. CI/CD

El repositorio conserva cuatro workflows:

| Workflow | Propósito |
| --- | --- |
| `01-frontend-compilacion.yml` | Instala dependencias, ejecuta tests Angular, compila y publica el artefacto estático |
| `02-frontend-despliegue.yml` | Workflow manual de build y despliegue del frontend en Amplify |
| `03-backend-compilacion.yml` | Ejecuta `clean verify` en backend y los tres microservicios Java; publica el JAR del backend |
| `04-backend-despliegue.yml` | Workflow manual de build/publicación de imagen y actualización del servicio ECS |

Los workflows 02 (frontend) y 04 (backend y microservicios) finalizaron correctamente. El frontend, el backend/ALB y los cuatro servicios ECS fueron verificados en cloud como parte del E2E del 2026-10-18. La topología RabbitMQ cloud sigue limitada a un solo host EC2 y no representa alta disponibilidad ante pérdida del host.
