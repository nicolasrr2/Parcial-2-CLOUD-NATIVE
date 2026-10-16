package cl.duoc.dsy1107.pedidos360.notificaciones.consumer;

import cl.duoc.dsy1107.pedidos360.notificaciones.config.RabbitMessagingProperties;
import cl.duoc.dsy1107.pedidos360.notificaciones.event.SolicitudEvent;
import cl.duoc.dsy1107.pedidos360.notificaciones.exception.NonRecoverableNotificationException;
import cl.duoc.dsy1107.pedidos360.notificaciones.exception.RecoverableNotificationException;
import cl.duoc.dsy1107.pedidos360.notificaciones.service.NotificationProcessor;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class NotificationConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(NotificationConsumer.class);

    private final NotificationProcessor processor;
    private final RabbitMessagingProperties properties;

    public NotificationConsumer(
            NotificationProcessor processor,
            RabbitMessagingProperties properties) {

        this.processor = processor;
        this.properties = properties;
    }

    @RabbitListener(
            queues = "${pedidos360.messaging.queues.notificaciones}"
    )
    public void consume(
            SolicitudEvent event,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag)
            throws IOException {

        int maxAttempts =
                Math.max(
                        1,
                        properties.retry().maxAttempts()
                );

        long delay =
                Math.max(
                        0,
                        properties.retry().initialIntervalMs()
                );

        for (int attempt = 1;
             attempt <= maxAttempts;
             attempt++) {

            try {

                processor.process(event);

                channel.basicAck(
                        deliveryTag,
                        false
                );

                log.info(
                        "ACK enviado: solicitudId={}, intento={}",
                        event.solicitudId(),
                        attempt
                );

                return;
            }
            catch (RecoverableNotificationException ex) {

                log.warn(
                        "Error recuperable: solicitudId={}, intento={}/{}, causa={}",
                        event.solicitudId(),
                        attempt,
                        maxAttempts,
                        ex.getMessage()
                );

                if (attempt == maxAttempts) {

                    rejectToDlq(
                            event,
                            channel,
                            deliveryTag,
                            "reintentos agotados"
                    );

                    return;
                }

                waitBeforeRetry(delay);

                delay = nextDelay(delay);
            }
            catch (NonRecoverableNotificationException ex) {

                log.error(
                        "Error no recuperable: solicitudId={}, causa={}",
                        event.solicitudId(),
                        ex.getMessage()
                );

                rejectToDlq(
                        event,
                        channel,
                        deliveryTag,
                        "error no recuperable"
                );

                return;
            }
            catch (Exception ex) {

                log.error(
                        "Error inesperado: solicitudId={}, causa={}",
                        event == null ? null : event.solicitudId(),
                        ex.getMessage(),
                        ex
                );

                rejectToDlq(
                        event,
                        channel,
                        deliveryTag,
                        "error inesperado"
                );

                return;
            }
        }
    }

    private void rejectToDlq(
            SolicitudEvent event,
            Channel channel,
            long deliveryTag,
            String reason)
            throws IOException {

        channel.basicNack(
                deliveryTag,
                false,
                false
        );

        log.warn(
                "NACK enviado con requeue=false: solicitudId={}, motivo={}. RabbitMQ enviara el mensaje al DLX/DLQ.",
                event == null ? null : event.solicitudId(),
                reason
        );
    }

    private void waitBeforeRetry(long delay) {

        if (delay <= 0) {
            return;
        }

        try {
            Thread.sleep(delay);
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();

            throw new NonRecoverableNotificationException(
                    "El proceso fue interrumpido durante el reintento"
            );
        }
    }

    private long nextDelay(long currentDelay) {

        double multiplier =
                Math.max(
                        1.0,
                        properties.retry().multiplier()
                );

        return Math.round(
                currentDelay * multiplier
        );
    }
}