package cl.duoc.dsy1107.pedidos360.rabbitadmin.service;

import cl.duoc.dsy1107.pedidos360.rabbitadmin.dto.BindingRequest;
import cl.duoc.dsy1107.pedidos360.rabbitadmin.dto.ExchangeRequest;
import cl.duoc.dsy1107.pedidos360.rabbitadmin.dto.QueueRequest;
import cl.duoc.dsy1107.pedidos360.rabbitadmin.exception.RabbitAdminOperationException;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RabbitAdminService {

    private final RabbitAdmin rabbitAdmin;

    public RabbitAdminService(
            RabbitAdmin rabbitAdmin) {

        this.rabbitAdmin = rabbitAdmin;
    }

    public void createQueue(
            QueueRequest request) {

        try {

            QueueBuilder builder =
                    QueueBuilder
                            .durable(request.name())
                            .quorum();

            if (hasText(request.deadLetterExchange())) {

                builder.deadLetterExchange(
                        request.deadLetterExchange()
                );

                builder.deadLetterRoutingKey(
                        request.deadLetterRoutingKey()
                );
            }

            rabbitAdmin.declareQueue(
                    builder.build()
            );
        }
        catch (AmqpException ex) {

            throw new RabbitAdminOperationException(
                    "No fue posible crear la cola "
                            + request.name(),
                    ex
            );
        }
    }

    public void deleteQueue(
            String name) {

        try {
            rabbitAdmin.deleteQueue(name);
        }
        catch (AmqpException ex) {

            throw new RabbitAdminOperationException(
                    "No fue posible eliminar la cola "
                            + name,
                    ex
            );
        }
    }

    public void createExchange(
            ExchangeRequest request) {

        try {

            Exchange exchange =
                    buildExchange(request);

            rabbitAdmin.declareExchange(
                    exchange
            );
        }
        catch (AmqpException ex) {

            throw new RabbitAdminOperationException(
                    "No fue posible crear el exchange "
                            + request.name(),
                    ex
            );
        }
    }

    public void deleteExchange(
            String name) {

        try {
            rabbitAdmin.deleteExchange(name);
        }
        catch (AmqpException ex) {

            throw new RabbitAdminOperationException(
                    "No fue posible eliminar el exchange "
                            + name,
                    ex
            );
        }
    }

    public void createBinding(
            BindingRequest request) {

        try {

            rabbitAdmin.declareBinding(
                    buildBinding(request)
            );
        }
        catch (AmqpException ex) {

            throw new RabbitAdminOperationException(
                    "No fue posible crear el binding",
                    ex
            );
        }
    }

    public void deleteBinding(
            BindingRequest request) {

        try {

            rabbitAdmin.removeBinding(
                    buildBinding(request)
            );
        }
        catch (AmqpException ex) {

            throw new RabbitAdminOperationException(
                    "No fue posible eliminar el binding",
                    ex
            );
        }
    }

    private Exchange buildExchange(
            ExchangeRequest request) {

        return switch (request.type()) {

            case "direct" ->
                    ExchangeBuilder
                            .directExchange(request.name())
                            .durable(true)
                            .build();

            case "topic" ->
                    ExchangeBuilder
                            .topicExchange(request.name())
                            .durable(true)
                            .build();

            case "fanout" ->
                    ExchangeBuilder
                            .fanoutExchange(request.name())
                            .durable(true)
                            .build();

            default ->
                    throw new IllegalArgumentException(
                            "Tipo de exchange no soportado"
                    );
        };
    }

    private Binding buildBinding(
            BindingRequest request) {

        String routingKey =
                request.routingKey() == null
                        ? ""
                        : request.routingKey();

        return new Binding(
                request.queue(),
                Binding.DestinationType.QUEUE,
                request.exchange(),
                routingKey,
                Map.of()
        );
    }

    private boolean hasText(
            String value) {

        return value != null
                && !value.isBlank();
    }
}