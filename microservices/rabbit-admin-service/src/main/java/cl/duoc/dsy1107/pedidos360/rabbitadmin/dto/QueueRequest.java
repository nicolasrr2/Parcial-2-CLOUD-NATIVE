package cl.duoc.dsy1107.pedidos360.rabbitadmin.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record QueueRequest(

        @NotBlank(
                message = "name es obligatorio"
        )
        @Pattern(
                regexp = "^[a-zA-Z0-9._-]+$",
                message = "name solo puede contener letras, numeros, punto, guion y guion bajo"
        )
        @Size(
                min = 3,
                max = 120,
                message = "name debe tener entre 3 y 120 caracteres"
        )
        String name,

        @Pattern(
                regexp = "^$|^[a-zA-Z0-9._-]+$",
                message = "deadLetterExchange tiene un formato invalido"
        )
        String deadLetterExchange,

        @Pattern(
                regexp = "^$|^[a-zA-Z0-9._-]+$",
                message = "deadLetterRoutingKey tiene un formato invalido"
        )
        String deadLetterRoutingKey
) {

    @AssertTrue(
            message = "deadLetterExchange y deadLetterRoutingKey deben informarse juntos"
    )
    public boolean isDeadLetterConfigurationValid() {

        boolean exchangeBlank =
                deadLetterExchange == null
                        || deadLetterExchange.isBlank();

        boolean routingBlank =
                deadLetterRoutingKey == null
                        || deadLetterRoutingKey.isBlank();

        return exchangeBlank == routingBlank;
    }
}