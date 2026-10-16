package cl.duoc.dsy1107.pedidos360.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pedidos360.messaging")
public record RabbitMessagingProperties(
        Exchanges exchanges,
        Queues queues,
        RoutingKeys routingKeys
) {

    public record Exchanges(
            String solicitudes,
            String deadLetter
    ) {}

    public record Queues(
            String notificaciones,
            String documentos,
            String notificacionesDlq,
            String documentosDlq
    ) {}

    public record RoutingKeys(
            String solicitudCreada,
            String solicitudAprobada,
            String solicitudRechazada,
            String notificacionesDead,
            String documentosDead
    ) {}
}
