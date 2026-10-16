package cl.duoc.dsy1107.pedidos360.notificaciones.consumer;

import cl.duoc.dsy1107.pedidos360.notificaciones.config.RabbitMessagingProperties;
import cl.duoc.dsy1107.pedidos360.notificaciones.event.SolicitudEvent;
import cl.duoc.dsy1107.pedidos360.notificaciones.exception.NonRecoverableNotificationException;
import cl.duoc.dsy1107.pedidos360.notificaciones.exception.RecoverableNotificationException;
import cl.duoc.dsy1107.pedidos360.notificaciones.service.NotificationProcessor;
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
class NotificationConsumerTest {

    @Mock
    private NotificationProcessor processor;

    @Mock
    private Channel channel;

    private NotificationConsumer consumer;
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
                                "pedidos360.notificaciones.queue",
                                "pedidos360.notificaciones.dlq"
                        ),
                        new RabbitMessagingProperties.RoutingKeys(
                                "solicitud.creada",
                                "solicitud.aprobada",
                                "solicitud.rechazada",
                                "notificaciones.dead"
                        ),
                        new RabbitMessagingProperties.Retry(
                                3,
                                0,
                                2.0
                        )
                );

        consumer = new NotificationConsumer(
                processor,
                properties
        );

        event = new SolicitudEvent(
                10L,
                "Vacaciones",
                "APROBADA",
                "user-123",
                "usuario@test.com",
                "Aprobada",
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
                100L
        );

        verify(processor, times(1))
                .process(event);

        verify(channel)
                .basicAck(
                        100L,
                        false
                );

        verify(channel, never())
                .basicNack(
                        100L,
                        false,
                        false
                );
    }

    @Test
    void errorRecuperableReintentaYLuegoEnviaAck()
            throws IOException {

        doThrow(
                new RecoverableNotificationException(
                        "Servicio temporalmente no disponible"
                )
        )
                .doNothing()
                .when(processor)
                .process(event);

        consumer.consume(
                event,
                channel,
                101L
        );

        verify(processor, times(2))
                .process(event);

        verify(channel)
                .basicAck(
                        101L,
                        false
                );

        verify(channel, never())
                .basicNack(
                        101L,
                        false,
                        false
                );
    }

    @Test
    void errorRecuperableAgotaReintentosYEnviaNack()
            throws IOException {

        doThrow(
                new RecoverableNotificationException(
                        "Servicio sigue sin responder"
                )
        )
                .when(processor)
                .process(event);

        consumer.consume(
                event,
                channel,
                102L
        );

        verify(processor, times(3))
                .process(event);

        verify(channel, never())
                .basicAck(
                        102L,
                        false
                );

        verify(channel)
                .basicNack(
                        102L,
                        false,
                        false
                );
    }

    @Test
    void errorNoRecuperableEnviaNackSinReintentar()
            throws IOException {

        doThrow(
                new NonRecoverableNotificationException(
                        "Datos invalidos"
                )
        )
                .when(processor)
                .process(event);

        consumer.consume(
                event,
                channel,
                103L
        );

        verify(processor, times(1))
                .process(event);

        verify(channel, never())
                .basicAck(
                        103L,
                        false
                );

        verify(channel)
                .basicNack(
                        103L,
                        false,
                        false
                );
    }
}