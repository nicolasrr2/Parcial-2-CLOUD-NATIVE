package cl.duoc.dsy1107.pedidos360.service;

import cl.duoc.dsy1107.pedidos360.dto.DecisionRequest;
import cl.duoc.dsy1107.pedidos360.dto.SolicitudRequest;
import cl.duoc.dsy1107.pedidos360.entity.EstadoSolicitud;
import cl.duoc.dsy1107.pedidos360.entity.Solicitud;
import cl.duoc.dsy1107.pedidos360.exception.AccesoSolicitudException;
import cl.duoc.dsy1107.pedidos360.exception.ReglaNegocioException;
import cl.duoc.dsy1107.pedidos360.messaging.producer.SolicitudEventPublisher;
import cl.duoc.dsy1107.pedidos360.repository.SolicitudRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitudServiceTest {

    @Mock
    private SolicitudRepository repository;

    @Mock
    private SolicitudEventPublisher eventPublisher;

    @InjectMocks
    private SolicitudService service;

    private SolicitudRequest request;

    @BeforeEach
    void setUp() {

        request = new SolicitudRequest();

        request.setTipo("Vacaciones");

        request.setFechaInicio(
                LocalDate.of(2026, 1, 10)
        );

        request.setFechaFin(
                LocalDate.of(2026, 1, 12)
        );

        request.setMotivo(
                "Descanso anual"
        );
    }

    @Test
    @SuppressWarnings("null")
    void crearAsignaEstadoPendienteYDatosDelToken() {

        when(repository.save(any(Solicitud.class)))
                .thenAnswer(
                        invocation ->
                                invocation.getArgument(0)
                );

        var result = service.crear(
                request,
                "user-123",
                "user@example.com"
        );

        assertThat(result.estado())
                .isEqualTo(
                        EstadoSolicitud.PENDIENTE
                );

        assertThat(result.creadoPor())
                .isEqualTo("user-123");

        assertThat(result.creadoPorEmail())
                .isEqualTo("user@example.com");

        verify(eventPublisher)
                .publicarCreada(
                        any(Solicitud.class)
                );
    }

    @Test
    void solicitanteNoPuedeEditarSolicitudDeOtroUsuario() {

        Solicitud solicitud = solicitud(
                1L,
                "owner",
                EstadoSolicitud.PENDIENTE
        );

        when(repository.findById(1L))
                .thenReturn(
                        Optional.of(solicitud)
                );

        assertThatThrownBy(
                () -> service.actualizar(
                        1L,
                        request,
                        "otro-usuario"
                )
        )
                .isInstanceOf(
                        AccesoSolicitudException.class
                );
    }

    @Test
    void solicitudPendientePuedeSerAprobada() {

        Solicitud solicitud = solicitud(
                1L,
                "owner",
                EstadoSolicitud.PENDIENTE
        );

        when(repository.findById(1L))
                .thenReturn(
                        Optional.of(solicitud)
                );

        DecisionRequest decision =
                new DecisionRequest();

        decision.setDecision(
                EstadoSolicitud.APROBADA
        );

        decision.setComentario(
                "Aprobada por jefatura"
        );

        var result = service.decidir(
                1L,
                decision
        );

        assertThat(result.estado())
                .isEqualTo(
                        EstadoSolicitud.APROBADA
                );

        assertThat(
                result.comentarioAprobador()
        )
                .isEqualTo(
                        "Aprobada por jefatura"
                );

        verify(eventPublisher)
                .publicarAprobada(solicitud);
    }

    @Test
    void solicitudYaDecididaNoPuedeDecidirseNuevamente() {

        Solicitud solicitud = solicitud(
                1L,
                "owner",
                EstadoSolicitud.RECHAZADA
        );

        when(repository.findById(1L))
                .thenReturn(
                        Optional.of(solicitud)
                );

        DecisionRequest decision =
                new DecisionRequest();

        decision.setDecision(
                EstadoSolicitud.APROBADA
        );

        decision.setComentario(
                "Segundo intento"
        );

        assertThatThrownBy(
                () -> service.decidir(
                        1L,
                        decision
                )
        )
                .isInstanceOf(
                        ReglaNegocioException.class
                )
                .hasMessageContaining(
                        "PENDIENTE"
                );
    }

    private Solicitud solicitud(
            Long id,
            String owner,
            EstadoSolicitud estado) {

        Solicitud solicitud =
                new Solicitud();

        solicitud.setId(id);
        solicitud.setCreadoPor(owner);
        solicitud.setEstado(estado);

        solicitud.setTipo(
                "Vacaciones"
        );

        solicitud.setFechaInicio(
                request.getFechaInicio()
        );

        solicitud.setFechaFin(
                request.getFechaFin()
        );

        solicitud.setMotivo(
                request.getMotivo()
        );

        return solicitud;
    }
}