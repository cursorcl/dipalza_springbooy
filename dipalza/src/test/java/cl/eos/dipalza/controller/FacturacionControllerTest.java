package cl.eos.dipalza.controller;

import cl.eos.dipalza.service.FacturacionService;
import cl.eos.dipalza.service.resultados.VentaFacturaResultado;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = FacturacionController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class FacturacionControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean FacturacionService service;

    @Test
    void facturarVentas_conResultados_retorna200() throws Exception {
        VentaFacturaResultado resultado = new VentaFacturaResultado(
                "FAC001", LocalDateTime.now(), BigDecimal.valueOf(1190), List.of(), "OK");
        when(service.facturar()).thenReturn(List.of(resultado));

        mockMvc.perform(post("/api/facturacion").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void facturarVentas_sinResultados_retorna204() throws Exception {
        when(service.facturar()).thenReturn(List.of());

        mockMvc.perform(post("/api/facturacion").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }
}
