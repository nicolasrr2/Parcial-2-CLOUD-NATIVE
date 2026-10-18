package cl.duoc.dsy1107.pedidos360.rabbitadmin.service;

import cl.duoc.dsy1107.pedidos360.rabbitadmin.dto.BindingRequest;
import cl.duoc.dsy1107.pedidos360.rabbitadmin.dto.ExchangeRequest;
import cl.duoc.dsy1107.pedidos360.rabbitadmin.dto.QueueRequest;
import cl.duoc.dsy1107.pedidos360.rabbitadmin.exception.RabbitAdminOperationException;
import cl.duoc.dsy1107.pedidos360.rabbitadmin.exception.RabbitResourceBusyException;
import cl.duoc.dsy1107.pedidos360.rabbitadmin.exception.RabbitResourceNotFoundException;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

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

            rabbitAdmin.declareQueue(
                    buildQueue(request)
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

    public void updateQueue(
            String name,
            QueueRequest request) {

        validateSameName(
                name,
                request.name()
        );

        QueueInformation info =
                Optional.ofNullable(
                        rabbitAdmin.getQueueInfo(name)
                ).orElseThrow(
                        () -> new RabbitResourceNotFoundException(
                                "La cola " + name + " no existe"
                        )
                );

        if (
                info.getMessageCount() > 0
                        || info.getConsumerCount() > 0
        ) {

            throw new RabbitResourceBusyException(
                    "La cola " + name
                            + " debe estar vacia y sin consumidores para actualizarse"
            );
        }

        try {

            boolean deleted =
                    rabbitAdmin.deleteQueue(name);

            if (!deleted) {

                throw new RabbitResourceNotFoundException(
                        "La cola " + name + " no existe"
                );
            }

            rabbitAdmin.declareQueue(
                    buildQueue(request)
            );
        }
        catch (
                RabbitResourceNotFoundException
                        | RabbitResourceBusyException ex
        ) {

            throw ex;
        }
        catch (AmqpException ex) {

            throw new RabbitAdminOperationException(
                    "No fue posible actualizar la cola "
                            + name,
                    ex
            );
        }
    }

    public void deleteQueue(
            String name) {

        try {

            boolean deleted =
                    rabbitAdmin.deleteQueue(name);

            if (!deleted) {

                throw new RabbitResourceNotFoundException(
                        "La cola " + name + " no existe"
                );
            }
        }
        catch (RabbitResourceNotFoundException ex) {

            throw ex;
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

            rabbitAdmin.declareExchange(
                    buildExchange(request)
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

    public void updateExchange(
            String name,
            ExchangeRequest request) {

        validateSameName(
                name,
                request.name()
        );

        try {

            boolean deleted =
                    rabbitAdmin.deleteExchange(name);

            if (!deleted) {

                throw new RabbitResourceNotFoundException(
                        "El exchange " + name + " no existe"
                );
            }

            rabbitAdmin.declareExchange(
                    buildExchange(request)
            );
        }
        catch (RabbitResourceNotFoundException ex) {

            throw ex;
        }
        catch (AmqpException ex) {

            throw new RabbitAdminOperationException(
                    "No fue posible actualizar el exchange "
                            + name,
                    ex
            );
        }
    }

    public void deleteExchange(
            String name) {

        try {

            boolean deleted =
                    rabbitAdmin.deleteExchange(name);

            if (!deleted) {

                throw new RabbitResourceNotFoundException(
                        "El exchange " + name + " no existe"
                );
            }
        }
        catch (RabbitResourceNotFoundException ex) {

            throw ex;
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

    private Queue buildQueue(
            QueueRequest request) {

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

        return builder.build();
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

    private void validateSameName(
            String pathName,
            String bodyName) {

        if (!pathName.equals(bodyName)) {

            throw new IllegalArgumentException(
                    "El nombre de la URL debe coincidir con el nombre del recurso"
            );
        }
    }

    private boolean hasText(
            String value) {

        return value != null
                && !value.isBlank();
    }
}