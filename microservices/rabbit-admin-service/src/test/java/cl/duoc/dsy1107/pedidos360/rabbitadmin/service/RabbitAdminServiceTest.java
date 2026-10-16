package cl.duoc.dsy1107.pedidos360.rabbitadmin.service;

import cl.duoc.dsy1107.pedidos360.rabbitadmin.dto.ExchangeRequest;
import cl.duoc.dsy1107.pedidos360.rabbitadmin.dto.QueueRequest;
import cl.duoc.dsy1107.pedidos360.rabbitadmin.exception.RabbitResourceBusyException;
import cl.duoc.dsy1107.pedidos360.rabbitadmin.exception.RabbitResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RabbitAdminServiceTest {

    private RabbitAdmin rabbitAdmin;
    private RabbitAdminService service;

    @BeforeEach
    void setUp() {

        rabbitAdmin =
                mock(RabbitAdmin.class);

        service =
                new RabbitAdminService(rabbitAdmin);
    }

    @Test
    void actualizarColaVaciaEliminaYRecrea() {

        String name =
                "pedidos360.demo.queue";

        QueueInformation info =
                mock(QueueInformation.class);

        when(rabbitAdmin.getQueueInfo(name))
                .thenReturn(info);

        when(info.getMessageCount())
                .thenReturn(0);

        when(info.getConsumerCount())
                .thenReturn(0);

        when(rabbitAdmin.deleteQueue(name))
                .thenReturn(true);

        QueueRequest request =
                new QueueRequest(
                        name,
                        "",
                        ""
                );

        service.updateQueue(
                name,
                request
        );

        verify(rabbitAdmin)
                .deleteQueue(name);

        verify(rabbitAdmin)
                .declareQueue(
                        any(Queue.class)
                );
    }

    @Test
    void actualizarColaConMensajesEsRechazado() {

        String name =
                "pedidos360.demo.queue";

        QueueInformation info =
                mock(QueueInformation.class);

        when(rabbitAdmin.getQueueInfo(name))
                .thenReturn(info);

        when(info.getMessageCount())
                .thenReturn(1);

        when(info.getConsumerCount())
                .thenReturn(0);

        QueueRequest request =
                new QueueRequest(
                        name,
                        "",
                        ""
                );

        assertThrows(
                RabbitResourceBusyException.class,
                () ->
                        service.updateQueue(
                                name,
                                request
                        )
        );

        verify(rabbitAdmin, never())
                .deleteQueue(name);

        verify(rabbitAdmin, never())
                .declareQueue(
                        any(Queue.class)
                );
    }

    @Test
    void actualizarColaInexistenteEsRechazado() {

        String name =
                "pedidos360.no.existe.queue";

        when(rabbitAdmin.getQueueInfo(name))
                .thenReturn(null);

        QueueRequest request =
                new QueueRequest(
                        name,
                        "",
                        ""
                );

        assertThrows(
                RabbitResourceNotFoundException.class,
                () ->
                        service.updateQueue(
                                name,
                                request
                        )
        );

        verify(rabbitAdmin, never())
                .deleteQueue(name);

        verify(rabbitAdmin, never())
                .declareQueue(
                        any(Queue.class)
                );
    }

    @Test
    void actualizarExchangeEliminaYRecrea() {

        String name =
                "pedidos360.demo.exchange";

        when(rabbitAdmin.deleteExchange(name))
                .thenReturn(true);

        ExchangeRequest request =
                new ExchangeRequest(
                        name,
                        "direct"
                );

        service.updateExchange(
                name,
                request
        );

        verify(rabbitAdmin)
                .deleteExchange(name);

        verify(rabbitAdmin)
                .declareExchange(
                        any(Exchange.class)
                );
    }

    @Test
    void actualizarExchangeInexistenteEsRechazado() {

        String name =
                "pedidos360.no.existe.exchange";

        when(rabbitAdmin.deleteExchange(name))
                .thenReturn(false);

        ExchangeRequest request =
                new ExchangeRequest(
                        name,
                        "topic"
                );

        assertThrows(
                RabbitResourceNotFoundException.class,
                () ->
                        service.updateExchange(
                                name,
                                request
                        )
        );

        verify(rabbitAdmin, never())
                .declareExchange(
                        any(Exchange.class)
                );
    }
}