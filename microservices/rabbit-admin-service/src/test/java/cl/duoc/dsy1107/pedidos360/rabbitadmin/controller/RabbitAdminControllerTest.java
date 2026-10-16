package cl.duoc.dsy1107.pedidos360.rabbitadmin.controller;

import cl.duoc.dsy1107.pedidos360.rabbitadmin.dto.BindingRequest;
import cl.duoc.dsy1107.pedidos360.rabbitadmin.dto.ExchangeRequest;
import cl.duoc.dsy1107.pedidos360.rabbitadmin.dto.QueueRequest;
import cl.duoc.dsy1107.pedidos360.rabbitadmin.service.RabbitAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RabbitAdminController.class)
@Import(GlobalExceptionHandler.class)
class RabbitAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RabbitAdminService service;

    @Test
    void colaValidaRetornaCreated()
            throws Exception {

        mockMvc.perform(
                post("/admin/queues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "pedidos360.demo.queue",
                                  "deadLetterExchange": "pedidos360.dlx",
                                  "deadLetterRoutingKey": "demo.dead"
                                }
                                """)
        )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.resource")
                                .value("queue")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("created")
                );

        verify(service)
                .createQueue(
                        any(QueueRequest.class)
                );
    }

    @Test
    void colaSinNombreRetornaBadRequest()
            throws Exception {

        mockMvc.perform(
                post("/admin/queues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "deadLetterExchange": "",
                                  "deadLetterRoutingKey": ""
                                }
                                """)
        )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.details.name")
                                .exists()
                );

        verify(service, never())
                .createQueue(
                        any(QueueRequest.class)
                );
    }

    @Test
    void configuracionDlqIncompletaRetornaBadRequest()
            throws Exception {

        mockMvc.perform(
                post("/admin/queues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "pedidos360.demo.queue",
                                  "deadLetterExchange": "pedidos360.dlx",
                                  "deadLetterRoutingKey": ""
                                }
                                """)
        )
                .andExpect(
                        status().isBadRequest()
                );

        verify(service, never())
                .createQueue(
                        any(QueueRequest.class)
                );
    }

    @Test
    void exchangeConTipoInvalidoRetornaBadRequest()
            throws Exception {

        mockMvc.perform(
                post("/admin/exchanges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "pedidos360.demo.exchange",
                                  "type": "incorrecto"
                                }
                                """)
        )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.details.type")
                                .exists()
                );

        verify(service, never())
                .createExchange(
                        any(ExchangeRequest.class)
                );
    }

    @Test
    void bindingSinQueueRetornaBadRequest()
            throws Exception {

        mockMvc.perform(
                post("/admin/bindings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "exchange": "pedidos360.demo.exchange",
                                  "queue": "",
                                  "routingKey": "demo.created"
                                }
                                """)
        )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.details.queue")
                                .exists()
                );

        verify(service, never())
                .createBinding(
                        any(BindingRequest.class)
                );
    }
}