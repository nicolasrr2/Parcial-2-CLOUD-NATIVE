# Pedidos360 - DSY1107 EV2

# Objetivo

Pedidos360 amplía la aplicación cloud-native con integración asíncrona mediante RabbitMQ. El backend Spring Boot persiste solicitudes en PostgreSQL y publica eventos de creación y decisión. Los servicios de notificaciones y documentos procesan esos eventos de forma independiente.

##Arquitectura
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

