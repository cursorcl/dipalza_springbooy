package cl.eos.dipalza.controller;

import cl.eos.dipalza.model.venta.VentaDetalleDTO;
import cl.eos.dipalza.service.VentaDetalleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = VentaDetalleController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class VentaDetalleControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean VentaDetalleService service;

    @Test
    void listarDetalles_conResultados_retorna200() throws Exception {
        VentaDetalleDTO dto = new VentaDetalleDTO();
        dto.setId(1L);
        when(service.listarDetallesOptimized(10L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/ventadetalle/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void listarDetalles_sinResultados_retornaArrayVacio() throws Exception {
        when(service.listarDetallesOptimized(99L)).thenReturn(List.of());

        mockMvc.perform(get("/api/ventadetalle/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
