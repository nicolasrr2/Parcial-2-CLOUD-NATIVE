package cl.duoc.dsy1107.pedidos360.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.lang.NonNull;
import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException exception) {
        String detalle = exception.getBindingResult().getFieldErrors().stream()
                .findFirst().map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("La solicitud no es valida");
        return respuesta(HttpStatus.BAD_REQUEST, detalle);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> jsonInvalido(HttpMessageNotReadableException exception) {
        return respuesta(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no es valido");
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<Map<String, Object>> regla(ReglaNegocioException exception) {
        return respuesta(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(AccesoSolicitudException.class)
    public ResponseEntity<Map<String, Object>> acceso(AccesoSolicitudException exception) {
        return respuesta(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(SolicitudNoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> noEncontrada(SolicitudNoEncontradaException exception) {
        return respuesta(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    private ResponseEntity<Map<String, Object>> respuesta(@NonNull HttpStatus status, String detail) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", Instant.now(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", detail));
    }
}
