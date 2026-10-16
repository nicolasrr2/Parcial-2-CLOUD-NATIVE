package cl.duoc.dsy1107.pedidos360.rabbitadmin.controller;

import cl.duoc.dsy1107.pedidos360.rabbitadmin.exception.RabbitAdminOperationException;
import cl.duoc.dsy1107.pedidos360.rabbitadmin.exception.RabbitResourceBusyException;
import cl.duoc.dsy1107.pedidos360.rabbitadmin.exception.RabbitResourceNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> validationError(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors =
                new LinkedHashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(
                        error ->
                                errors.put(
                                        error.getField(),
                                        error.getDefaultMessage()
                                )
                );

        ex.getBindingResult()
                .getGlobalErrors()
                .forEach(
                        error ->
                                errors.put(
                                        "configuration",
                                        error.getDefaultMessage()
                                )
                );

        return Map.of(
                "status", 400,
                "error", "Bad Request",
                "details", errors
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> constraintError(
            ConstraintViolationException ex) {

        return Map.of(
                "status", 400,
                "error", "Bad Request",
                "message", ex.getMessage()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> illegalArgument(
            IllegalArgumentException ex) {

        return Map.of(
                "status", 400,
                "error", "Bad Request",
                "message", ex.getMessage()
        );
    }

    @ExceptionHandler(RabbitResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> resourceNotFound(
            RabbitResourceNotFoundException ex) {

        return Map.of(
                "status", 404,
                "error", "Not Found",
                "message", ex.getMessage()
        );
    }

    @ExceptionHandler(RabbitResourceBusyException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> resourceBusy(
            RabbitResourceBusyException ex) {

        return Map.of(
                "status", 409,
                "error", "Conflict",
                "message", ex.getMessage()
        );
    }

    @ExceptionHandler(RabbitAdminOperationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> rabbitOperationError(
            RabbitAdminOperationException ex) {

        return Map.of(
                "status", 409,
                "error", "RabbitMQ operation failed",
                "message", ex.getMessage()
        );
    }
}