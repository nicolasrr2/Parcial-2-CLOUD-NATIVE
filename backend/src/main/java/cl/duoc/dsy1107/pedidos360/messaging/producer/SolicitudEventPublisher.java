package cl.duoc.dsy1107.pedidos360.messaging.producer;

import cl.duoc.dsy1107.pedidos360.entity.Solicitud;

public interface SolicitudEventPublisher {

    void publicarCreada(Solicitud solicitud);

    void publicarAprobada(Solicitud solicitud);

    void publicarRechazada(Solicitud solicitud);
}