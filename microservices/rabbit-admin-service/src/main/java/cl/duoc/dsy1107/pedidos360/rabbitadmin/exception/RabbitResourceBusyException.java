package cl.duoc.dsy1107.pedidos360.rabbitadmin.exception;

public class RabbitResourceBusyException extends RuntimeException {

    public RabbitResourceBusyException(String message) {
        super(message);
    }
}