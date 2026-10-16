package cl.duoc.dsy1107.pedidos360.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RabbitMessagingProperties.class)
public class RabbitTopologyConfig {

    private final RabbitMessagingProperties properties;

    public RabbitTopologyConfig(RabbitMessagingProperties properties) {
        this.properties = properties;
    }

    @Bean
    public TopicExchange solicitudesExchange() {
        return new TopicExchange(
                properties.exchanges().solicitudes(),
                true,
                false
        );
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(
                properties.exchanges().deadLetter(),
                true,
                false
        );
    }

    @Bean
    public Queue notificacionesQueue() {
        return QueueBuilder
                .durable(properties.queues().notificaciones())
                .quorum()
                .deadLetterExchange(properties.exchanges().deadLetter())
                .deadLetterRoutingKey(properties.routingKeys().notificacionesDead())
                .build();
    }

    @Bean
    public Queue documentosQueue() {
        return QueueBuilder
                .durable(properties.queues().documentos())
                .quorum()
                .deadLetterExchange(properties.exchanges().deadLetter())
                .deadLetterRoutingKey(properties.routingKeys().documentosDead())
                .build();
    }

    @Bean
    public Queue notificacionesDlq() {
        return QueueBuilder
                .durable(properties.queues().notificacionesDlq())
                .quorum()
                .build();
    }

    @Bean
    public Queue documentosDlq() {
        return QueueBuilder
                .durable(properties.queues().documentosDlq())
                .quorum()
                .build();
    }

    @Bean
    public Binding notificacionesCreadaBinding(
            Queue notificacionesQueue,
            TopicExchange solicitudesExchange) {

        return BindingBuilder
                .bind(notificacionesQueue)
                .to(solicitudesExchange)
                .with(properties.routingKeys().solicitudCreada());
    }

    @Bean
    public Binding notificacionesAprobadaBinding(
            Queue notificacionesQueue,
            TopicExchange solicitudesExchange) {

        return BindingBuilder
                .bind(notificacionesQueue)
                .to(solicitudesExchange)
                .with(properties.routingKeys().solicitudAprobada());
    }

    @Bean
    public Binding notificacionesRechazadaBinding(
            Queue notificacionesQueue,
            TopicExchange solicitudesExchange) {

        return BindingBuilder
                .bind(notificacionesQueue)
                .to(solicitudesExchange)
                .with(properties.routingKeys().solicitudRechazada());
    }

    @Bean
    public Binding documentosAprobadaBinding(
            Queue documentosQueue,
            TopicExchange solicitudesExchange) {

        return BindingBuilder
                .bind(documentosQueue)
                .to(solicitudesExchange)
                .with(properties.routingKeys().solicitudAprobada());
    }

    @Bean
    public Binding notificacionesDlqBinding(
            Queue notificacionesDlq,
            TopicExchange deadLetterExchange) {

        return BindingBuilder
                .bind(notificacionesDlq)
                .to(deadLetterExchange)
                .with(properties.routingKeys().notificacionesDead());
    }

    @Bean
    public Binding documentosDlqBinding(
            Queue documentosDlq,
            TopicExchange deadLetterExchange) {

        return BindingBuilder
                .bind(documentosDlq)
                .to(deadLetterExchange)
                .with(properties.routingKeys().documentosDead());
    }
}
