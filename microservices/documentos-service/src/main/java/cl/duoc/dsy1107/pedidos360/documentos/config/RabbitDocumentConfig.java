package cl.duoc.dsy1107.pedidos360.documentos.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RabbitMessagingProperties.class)
public class RabbitDocumentConfig {

    private final RabbitMessagingProperties properties;

    public RabbitDocumentConfig(
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
    public Queue documentosQueue() {
        return QueueBuilder
                .durable(properties.queues().documentos())
                .quorum()
                .deadLetterExchange(
                        properties.exchanges().deadLetter()
                )
                .deadLetterRoutingKey(
                        properties.routingKeys().documentosDead()
                )
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
    public Binding aprobadaBinding(
            @Qualifier("documentosQueue") Queue queue,
            @Qualifier("solicitudesExchange") TopicExchange exchange) {

        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(
                        properties
                                .routingKeys()
                                .solicitudAprobada()
                );
    }

    @Bean
    public Binding documentosDlqBinding(
            @Qualifier("documentosDlq") Queue queue,
            @Qualifier("deadLetterExchange") TopicExchange exchange) {

        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(
                        properties
                                .routingKeys()
                                .documentosDead()
                );
    }

    @Bean
    public MessageConverter rabbitMessageConverter(
            ObjectMapper objectMapper) {

        return new Jackson2JsonMessageConverter(
                objectMapper
        );
    }
}