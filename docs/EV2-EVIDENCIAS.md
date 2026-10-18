# Evidencias técnicas EV2

Mapa breve de los criterios de evaluación hacia la implementación y la evidencia disponible en el repositorio.

| Criterio | Implementación | Archivos de referencia | Validación/evidencia |
| --- | --- | --- | --- |
| Configuración centralizada de exchanges, colas y routing keys | Valores bajo `pedidos360.messaging` compartidos por las aplicaciones | `backend/src/main/resources/application.yml`; `microservices/notificaciones-service/src/main/resources/application.yml`; `microservices/documentos-service/src/main/resources/application.yml` | Nombres y routing keys documentados en la sección Topología del README raíz |
| Beans explícitos `Queue`, `Exchange` y `Binding` | Topología declarada en Spring AMQP; las colas son quorum | `backend/src/main/java/cl/duoc/dsy1107/pedidos360/messaging/config/RabbitTopologyConfig.java` | Declaraciones `.durable(...).quorum()` y bindings explícitos |
| Configuración aislada de mensajería | Cada aplicación consumidor declara sus recursos y propiedades | `microservices/notificaciones-service/src/main/java/cl/duoc/dsy1107/pedidos360/notificaciones/config/RabbitNotificationConfig.java`; `microservices/documentos-service/src/main/java/cl/duoc/dsy1107/pedidos360/documentos/config/RabbitDocumentConfig.java` | `application.yml` de cada servicio incluye settings RabbitMQ propios |
| Consumidores `@RabbitListener` | Notificaciones consume eventos de solicitudes; Documentos escucha solicitudes aprobadas | `microservices/notificaciones-service/src/main/java/cl/duoc/dsy1107/pedidos360/notificaciones/consumer/NotificationConsumer.java`; `microservices/documentos-service/src/main/java/cl/duoc/dsy1107/pedidos360/documentos/consumer/DocumentConsumer.java` | Pruebas unitarias en `src/test/java` de cada servicio |
| ACK manual, retry, NACK y DLQ | ACK posterior al procesamiento; reintentos recuperables limitados; NACK con `requeue=false` hacia DLX/DLQ | `backend/src/main/resources/application.yml`; `microservices/notificaciones-service/src/main/resources/application.yml`; `microservices/documentos-service/src/main/resources/application.yml`; `microservices/notificaciones-service/src/main/java/cl/duoc/dsy1107/pedidos360/notificaciones/consumer/NotificationConsumer.java`; `microservices/documentos-service/src/main/java/cl/duoc/dsy1107/pedidos360/documentos/consumer/DocumentConsumer.java` | Tests `NotificationConsumerTest` y `DocumentConsumerTest`; conteos en README raíz |
| Endpoints REST Rabbit Admin | POST/PUT/DELETE para colas y exchanges; POST/DELETE para bindings | `microservices/rabbit-admin-service/src/main/java/cl/duoc/dsy1107/pedidos360/rabbitadmin/controller/RabbitAdminController.java` | `RabbitAdminControllerTest` |
| Crear/actualizar/eliminar recursos RabbitMQ | Operaciones encapsuladas con `RabbitAdmin`; update de cola exige recurso vacío y sin consumidores | `microservices/rabbit-admin-service/src/main/java/cl/duoc/dsy1107/pedidos360/rabbitadmin/service/RabbitAdminService.java` | `RabbitAdminServiceTest` |
| DTO y validación de entradas | Bean Validation para nombres, tipo de exchange y propiedades de binding/cola | `microservices/rabbit-admin-service/src/main/java/cl/duoc/dsy1107/pedidos360/rabbitadmin/dto/`; `microservices/rabbit-admin-service/src/main/java/cl/duoc/dsy1107/pedidos360/rabbitadmin/exception/` | Pruebas de controlador y servicio de Rabbit Admin |
| Frontend Angular | Login Cognito PKCE, configuración runtime y vistas de solicitante/aprobador | `frontend/src/app/`; `frontend/public/config.json` | 8 pruebas Angular; `npm run build`; `npm audit --omit=dev` |
| Pruebas automatizadas | Backend, tres microservicios y frontend | `backend/src/test/`; `microservices/*/src/test/`; `frontend/src/app/**/*.spec.ts` | Ejecución local registrada en el README raíz; workflow Java ejecuta `mvn -B clean verify` para las cuatro apps |
| Failover quorum de tres nodos | Clúster RabbitMQ de tres nodos con quorum queues | `rabbitmq/docker-compose.yml`; `rabbitmq/rabbitmq.conf`; `backend/src/main/java/cl/duoc/dsy1107/pedidos360/messaging/config/RabbitTopologyConfig.java` | Prueba controlada documentada en la sección «Failover verificado» del README raíz; líder migró de nodo 2 a nodo 3 y el mensaje se conservó |

## Nota de diseño AWS

- LOCAL VERIFIED FAILOVER: el clúster RabbitMQ local de tres nodos fue verificado con un cambio de liderazgo y recuperación comprobada del mensaje.
- AWS DEPLOYMENT DESIGN: la infraestructura planificada usa tres contenedores RabbitMQ sobre una sola instancia EC2 Amazon Linux 2023, con tolerancia a fallo de nodo/contenedor individual y sin afirmar disponibilidad ante pérdida total del host EC2.
- No se ha validado un despliegue AWS real ni una ejecución de Terraform en esta sesión.

No se incluyen capturas ni evidencia de despliegue cloud en este documento.
