package cl.duoc.dsy1107.pedidos360.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BindingRequest(

        @NotBlank(
                message = "exchange es obligatorio"
        )
        @Pattern(
                regexp = "^[a-zA-Z0-9._-]+$",
                message = "exchange tiene un formato invalido"
        )
        @Size(
                max = 120,
                message = "exchange no puede superar 120 caracteres"
        )
        String exchange,

        @NotBlank(
                message = "queue es obligatorio"
        )
        @Pattern(
                regexp = "^[a-zA-Z0-9._-]+$",
                message = "queue tiene un formato invalido"
        )
        @Size(
                max = 120,
                message = "queue no puede superar 120 caracteres"
        )
        String queue,

        @Pattern(
                regexp = "^$|^[a-zA-Z0-9.*#_-]+$",
                message = "routingKey tiene un formato invalido"
        )
        @Size(
                max = 120,
                message = "routingKey no puede superar 120 caracteres"
        )
        String routingKey
) {}