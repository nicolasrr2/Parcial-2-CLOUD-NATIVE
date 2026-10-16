package cl.duoc.dsy1107.pedidos360.notificaciones.service;

import cl.duoc.dsy1107.pedidos360.notificaciones.event.SolicitudEvent;
import cl.duoc.dsy1107.pedidos360.notificaciones.exception.NonRecoverableNotificationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationProcessor {

    private static final Logger log =
            LoggerFactory.getLogger(NotificationProcessor.class);

    public void process(SolicitudEvent event) {

        if (event == null) {
            throw new NonRecoverableNotificationException(
                    "El evento de solicitud no puede ser null"
            );
        }

        if (event.solicitudId() == null) {
            throw new NonRecoverableNotificationException(
                    "El evento debe contener solicitudId"
            );
        }

        if (event.estado() == null || event.estado().isBlank()) {
            throw new NonRecoverableNotificationException(
                    "El evento debe contener estado"
            );
        }

        log.info(
                "Notificacion procesada: solicitudId={}, estado={}, email={}",
                event.solicitudId(),
                event.estado(),
                event.creadoPorEmail()
        );
    }
}