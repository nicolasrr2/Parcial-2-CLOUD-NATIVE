package cl.duoc.dsy1107.pedidos360.rabbitadmin.exception;

public class RabbitResourceNotFoundException extends RuntimeException {

    public RabbitResourceNotFoundException(String message) {
        super(message);
    }
}