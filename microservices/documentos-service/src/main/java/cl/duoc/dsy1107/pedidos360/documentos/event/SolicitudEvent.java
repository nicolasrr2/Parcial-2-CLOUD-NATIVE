package cl.duoc.dsy1107.pedidos360.documentos.event;

import java.time.Instant;

public record SolicitudEvent(
        Long solicitudId,
        String tipo,
        String estado,
        String creadoPor,
        String creadoPorEmail,
        String comentarioAprobador,
        Instant ocurridoEn
) {}