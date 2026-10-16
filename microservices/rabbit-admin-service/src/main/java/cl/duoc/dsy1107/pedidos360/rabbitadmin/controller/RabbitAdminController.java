package cl.duoc.dsy1107.pedidos360.rabbitadmin.controller;

import cl.duoc.dsy1107.pedidos360.rabbitadmin.dto.BindingRequest;
import cl.duoc.dsy1107.pedidos360.rabbitadmin.dto.ExchangeRequest;
import cl.duoc.dsy1107.pedidos360.rabbitadmin.dto.QueueRequest;
import cl.duoc.dsy1107.pedidos360.rabbitadmin.service.RabbitAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin")
@Validated
public class RabbitAdminController {

    private final RabbitAdminService service;

    public RabbitAdminController(
            RabbitAdminService service) {

        this.service = service;
    }

    @PostMapping("/queues")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> createQueue(
            @Valid @RequestBody QueueRequest request) {

        service.createQueue(request);

        return response(
                "queue",
                request.name(),
                "created"
        );
    }

    @PutMapping("/queues/{name}")
    public Map<String, String> updateQueue(
            @PathVariable String name,
            @Valid @RequestBody QueueRequest request) {

        service.updateQueue(
                name,
                request
        );

        return response(
                "queue",
                name,
                "updated"
        );
    }

    @DeleteMapping("/queues/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQueue(

            @PathVariable
            @Pattern(
                    regexp = "^[a-zA-Z0-9._-]+$",
                    message = "name tiene un formato invalido"
            )
            @Size(
                    min = 3,
                    max = 120,
                    message = "name debe tener entre 3 y 120 caracteres"
            )
            String name) {

        service.deleteQueue(name);
    }

    @PostMapping("/exchanges")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> createExchange(
            @Valid @RequestBody ExchangeRequest request) {

        service.createExchange(request);

        return response(
                "exchange",
                request.name(),
                "created"
        );
    }

    @PutMapping("/exchanges/{name}")
    public Map<String, String> updateExchange(
            @PathVariable String name,
            @Valid @RequestBody ExchangeRequest request) {

        service.updateExchange(
                name,
                request
        );

        return response(
                "exchange",
                name,
                "updated"
        );
    }

    @DeleteMapping("/exchanges/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExchange(

            @PathVariable
            @Pattern(
                    regexp = "^[a-zA-Z0-9._-]+$",
                    message = "name tiene un formato invalido"
            )
            @Size(
                    min = 3,
                    max = 120,
                    message = "name debe tener entre 3 y 120 caracteres"
            )
            String name) {

        service.deleteExchange(name);
    }

    @PostMapping("/bindings")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> createBinding(
            @Valid @RequestBody BindingRequest request) {

        service.createBinding(request);

        return response(
                "binding",
                request.exchange()
                        + " -> "
                        + request.queue(),
                "created"
        );
    }

    @DeleteMapping("/bindings")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBinding(
            @Valid @RequestBody BindingRequest request) {

        service.deleteBinding(request);
    }

    private Map<String, String> response(
            String resource,
            String name,
            String status) {

        return Map.of(
                "resource", resource,
                "name", name,
                "status", status
        );
    }
}