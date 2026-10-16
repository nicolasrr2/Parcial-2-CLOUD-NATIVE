package cl.duoc.dsy1107.pedidos360.notificaciones.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RabbitMessagingProperties.class)
public class RabbitNotificationConfig {

    private final RabbitMessagingProperties properties;

    public RabbitNotificationConfig(
            RabbitMessagingProperties properties) {

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
                .deadLetterExchange(
                        properties.exchanges().deadLetter()
                )
                .deadLetterRoutingKey(
                        properties.routingKeys().notificacionesDead()
                )
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
    public Binding creadaBinding(
            Queue notificacionesQueue,
            TopicExchange solicitudesExchange) {

        return BindingBuilder
                .bind(notificacionesQueue)
                .to(solicitudesExchange)
                .with(properties.routingKeys().solicitudCreada());
    }

    @Bean
    public Binding aprobadaBinding(
            Queue notificacionesQueue,
            TopicExchange solicitudesExchange) {

        return BindingBuilder
                .bind(notificacionesQueue)
                .to(solicitudesExchange)
                .with(properties.routingKeys().solicitudAprobada());
    }

    @Bean
    public Binding rechazadaBinding(
            Queue notificacionesQueue,
            TopicExchange solicitudesExchange) {

        return BindingBuilder
                .bind(notificacionesQueue)
                .to(solicitudesExchange)
                .with(properties.routingKeys().solicitudRechazada());
    }

    @Bean
    public Binding dlqBinding(
            Queue notificacionesDlq,
            TopicExchange deadLetterExchange) {

        return BindingBuilder
                .bind(notificacionesDlq)
                .to(deadLetterExchange)
                .with(properties.routingKeys().notificacionesDead());
    }

    @Bean
    public MessageConverter rabbitMessageConverter(
            ObjectMapper objectMapper) {

        return new Jackson2JsonMessageConverter(objectMapper);
    }
}