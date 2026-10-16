package cl.duoc.dsy1107.pedidos360.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ExchangeRequest(

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

        @NotBlank(
                message = "type es obligatorio"
        )
        @Pattern(
                regexp = "^(direct|topic|fanout)$",
                message = "type debe ser direct, topic o fanout"
        )
        String type
) {}