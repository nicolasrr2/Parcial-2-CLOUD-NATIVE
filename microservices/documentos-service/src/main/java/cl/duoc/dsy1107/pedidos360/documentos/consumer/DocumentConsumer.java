package cl.duoc.dsy1107.pedidos360.documentos.consumer;

import cl.duoc.dsy1107.pedidos360.documentos.config.RabbitMessagingProperties;
import cl.duoc.dsy1107.pedidos360.documentos.event.SolicitudEvent;
import cl.duoc.dsy1107.pedidos360.documentos.exception.NonRecoverableDocumentException;
import cl.duoc.dsy1107.pedidos360.documentos.exception.RecoverableDocumentException;
import cl.duoc.dsy1107.pedidos360.documentos.service.DocumentProcessor;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class DocumentConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(DocumentConsumer.class);

    private final DocumentProcessor processor;
    private final RabbitMessagingProperties properties;

    public DocumentConsumer(
            DocumentProcessor processor,
            RabbitMessagingProperties properties) {

        this.processor = processor;
        this.properties = properties;
    }

    @RabbitListener(
            queues = "${pedidos360.messaging.queues.documentos}"
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
                        "ACK documento enviado: solicitudId={}, intento={}",
                        event.solicitudId(),
                        attempt
                );

                return;
            }
            catch (RecoverableDocumentException ex) {

                log.warn(
                        "Error recuperable documento: solicitudId={}, intento={}/{}, causa={}",
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
            catch (NonRecoverableDocumentException ex) {

                log.error(
                        "Error no recuperable documento: solicitudId={}, causa={}",
                        event == null ? null : event.solicitudId(),
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
                        "Error inesperado documento: solicitudId={}, causa={}",
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
                "NACK documento con requeue=false: solicitudId={}, motivo={}. RabbitMQ enviara el mensaje al DLX/DLQ.",
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

            throw new NonRecoverableDocumentException(
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