package cl.duoc.dsy1107.pedidos360.rabbitadmin.exception;

public class RabbitAdminOperationException extends RuntimeException {

    public RabbitAdminOperationException(
            String message,
            Throwable cause) {

        super(message, cause);
    }
}