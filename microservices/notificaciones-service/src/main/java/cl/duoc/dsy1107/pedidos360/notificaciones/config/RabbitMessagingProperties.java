package cl.duoc.dsy1107.pedidos360.notificaciones.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pedidos360.messaging")
public record RabbitMessagingProperties(
        Exchanges exchanges,
        Queues queues,
        RoutingKeys routingKeys,
        Retry retry
) {

    public record Exchanges(
            String solicitudes,
            String deadLetter
    ) {}

    public record Queues(
            String notificaciones,
            String notificacionesDlq
    ) {}

    public record RoutingKeys(
            String solicitudCreada,
            String solicitudAprobada,
            String solicitudRechazada,
            String notificacionesDead
    ) {}

    public record Retry(
            int maxAttempts,
            long initialIntervalMs,
            double multiplier
    ) {}
}