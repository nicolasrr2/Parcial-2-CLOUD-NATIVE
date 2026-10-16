package cl.duoc.dsy1107.pedidos360.documentos.config;

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
            String documentos,
            String documentosDlq
    ) {}

    public record RoutingKeys(
            String solicitudAprobada,
            String documentosDead
    ) {}

    public record Retry(
            int maxAttempts,
            long initialIntervalMs,
            double multiplier
    ) {}
}