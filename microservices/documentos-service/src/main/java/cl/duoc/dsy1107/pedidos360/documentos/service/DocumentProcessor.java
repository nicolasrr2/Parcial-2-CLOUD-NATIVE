package cl.duoc.dsy1107.pedidos360.documentos.service;

import cl.duoc.dsy1107.pedidos360.documentos.event.SolicitudEvent;
import cl.duoc.dsy1107.pedidos360.documentos.exception.NonRecoverableDocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DocumentProcessor {

    private static final Logger log =
            LoggerFactory.getLogger(DocumentProcessor.class);

    public void process(SolicitudEvent event) {

        if (event == null) {
            throw new NonRecoverableDocumentException(
                    "El evento de solicitud no puede ser null"
            );
        }

        if (event.solicitudId() == null) {
            throw new NonRecoverableDocumentException(
                    "El evento debe contener solicitudId"
            );
        }

        if (event.estado() == null || event.estado().isBlank()) {
            throw new NonRecoverableDocumentException(
                    "El evento debe contener estado"
            );
        }

        if (!"APROBADA".equalsIgnoreCase(event.estado())) {
            throw new NonRecoverableDocumentException(
                    "Solo se generan documentos para solicitudes APROBADAS"
            );
        }

        log.info(
                "Documento generado: solicitudId={}, tipo={}, email={}",
                event.solicitudId(),
                event.tipo(),
                event.creadoPorEmail()
        );
    }
}