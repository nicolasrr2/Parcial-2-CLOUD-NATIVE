package cl.duoc.dsy1107.pedidos360.messaging.event;

import cl.duoc.dsy1107.pedidos360.entity.Solicitud;
import java.time.Instant;

public record SolicitudEvent(
        Long solicitudId,
        String tipo,
        String estado,
        String creadoPor,
        String creadoPorEmail,
        String comentarioAprobador,
        Instant ocurridoEn
) {

    public static SolicitudEvent desde(Solicitud solicitud) {
        return new SolicitudEvent(
                solicitud.getId(),
                solicitud.getTipo(),
                solicitud.getEstado() == null ? null : solicitud.getEstado().name(),
                solicitud.getCreadoPor(),
                solicitud.getCreadoPorEmail(),
                solicitud.getComentarioAprobador(),
                Instant.now()
        );
    }
}