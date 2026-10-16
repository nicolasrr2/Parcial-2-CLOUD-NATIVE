package cl.duoc.dsy1107.pedidos360.documentos.consumer;

import cl.duoc.dsy1107.pedidos360.documentos.config.RabbitMessagingProperties;
import cl.duoc.dsy1107.pedidos360.documentos.event.SolicitudEvent;
import cl.duoc.dsy1107.pedidos360.documentos.exception.NonRecoverableDocumentException;
import cl.duoc.dsy1107.pedidos360.documentos.exception.RecoverableDocumentException;
import cl.duoc.dsy1107.pedidos360.documentos.service.DocumentProcessor;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.Instant;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DocumentConsumerTest {

    @Mock
    private DocumentProcessor processor;

    @Mock
    private Channel channel;

    private DocumentConsumer consumer;
    private SolicitudEvent event;

    @BeforeEach
    void setUp() {

        RabbitMessagingProperties properties =
                new RabbitMessagingProperties(
                        new RabbitMessagingProperties.Exchanges(
                                "pedidos360.solicitudes.events",
                                "pedidos360.dlx"
                        ),
                        new RabbitMessagingProperties.Queues(
                                "pedidos360.documentos.queue",
                                "pedidos360.documentos.dlq"
                        ),
                        new RabbitMessagingProperties.RoutingKeys(
                                "solicitud.aprobada",
                                "documentos.dead"
                        ),
                        new RabbitMessagingProperties.Retry(
                                3,
                                0,
                                2.0
                        )
                );

        consumer = new DocumentConsumer(
                processor,
                properties
        );

        event = new SolicitudEvent(
                20L,
                "Vacaciones",
                "APROBADA",
                "user-123",
                "usuario@test.com",
                "Aprobada por jefatura",
                Instant.now()
        );
    }

    @Test
    void procesamientoExitosoEnviaAck() throws IOException {

        doNothing()
                .when(processor)
                .process(event);

        consumer.consume(
                event,
                channel,
                200L
        );

        verify(processor, times(1))
                .process(event);

        verify(channel)
                .basicAck(
                        200L,
                        false
                );

        verify(channel, never())
                .basicNack(
                        200L,
                        false,
                        false
                );
    }

    @Test
    void errorRecuperableReintentaYLuegoEnviaAck()
            throws IOException {

        doThrow(
                new RecoverableDocumentException(
                        "Generador temporalmente no disponible"
                )
        )
                .doNothing()
                .when(processor)
                .process(event);

        consumer.consume(
                event,
                channel,
                201L
        );

        verify(processor, times(2))
                .process(event);

        verify(channel)
                .basicAck(
                        201L,
                        false
                );

        verify(channel, never())
                .basicNack(
                        201L,
                        false,
                        false
                );
    }

    @Test
    void errorRecuperableAgotaReintentosYEnviaNack()
            throws IOException {

        doThrow(
                new RecoverableDocumentException(
                        "Generador sigue sin responder"
                )
        )
                .when(processor)
                .process(event);

        consumer.consume(
                event,
                channel,
                202L
        );

        verify(processor, times(3))
                .process(event);

        verify(channel, never())
                .basicAck(
                        202L,
                        false
                );

        verify(channel)
                .basicNack(
                        202L,
                        false,
                        false
                );
    }

    @Test
    void errorNoRecuperableEnviaNackSinReintentar()
            throws IOException {

        doThrow(
                new NonRecoverableDocumentException(
                        "Estado de solicitud invalido"
                )
        )
                .when(processor)
                .process(event);

        consumer.consume(
                event,
                channel,
                203L
        );

        verify(processor, times(1))
                .process(event);

        verify(channel, never())
                .basicAck(
                        203L,
                        false
                );

        verify(channel)
                .basicNack(
                        203L,
                        false,
                        false
                );
    }
}