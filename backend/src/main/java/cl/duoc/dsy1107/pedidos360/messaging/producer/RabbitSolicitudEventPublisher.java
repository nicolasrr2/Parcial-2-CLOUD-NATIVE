package cl.duoc.dsy1107.pedidos360.messaging.producer;

import cl.duoc.dsy1107.pedidos360.entity.Solicitud;
import cl.duoc.dsy1107.pedidos360.messaging.config.RabbitMessagingProperties;
import cl.duoc.dsy1107.pedidos360.messaging.event.SolicitudEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class RabbitSolicitudEventPublisher implements SolicitudEventPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(RabbitSolicitudEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMessagingProperties properties;

    public RabbitSolicitudEventPublisher(
            RabbitTemplate rabbitTemplate,
            RabbitMessagingProperties properties) {

        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    @Override
    public void publicarCreada(Solicitud solicitud) {
        publicar(
                solicitud,
                properties.routingKeys().solicitudCreada()
        );
    }

    @Override
    public void publicarAprobada(Solicitud solicitud) {
        publicar(
                solicitud,
                properties.routingKeys().solicitudAprobada()
        );
    }

    @Override
    public void publicarRechazada(Solicitud solicitud) {
        publicar(
                solicitud,
                properties.routingKeys().solicitudRechazada()
        );
    }

    private void publicar(Solicitud solicitud, String routingKey) {

        SolicitudEvent evento = SolicitudEvent.desde(solicitud);

        rabbitTemplate.convertAndSend(
                properties.exchanges().solicitudes(),
                routingKey,
                evento
        );

        log.info(
                "Evento RabbitMQ publicado: solicitudId={}, routingKey={}",
                solicitud.getId(),
                routingKey
        );
    }
}