package cl.eos.dipalza.controller;

import cl.eos.dipalza.model.RutaDTO;
import cl.eos.dipalza.service.VendedorRutaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = VendedorRutaController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class VendedorRutaControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean VendedorRutaService service;

    private RutaDTO dto(String codigo) {
        RutaDTO d = new RutaDTO();
        d.setCodigo(codigo);
        d.setDescripcion("Ruta " + codigo);
        return d;
    }

    @Test
    void getRutas_retornaListaDelVendedor() throws Exception {
        when(service.getRutasByVendedor("001", "V")).thenReturn(List.of(dto("R01")));

        mockMvc.perform(get("/api/vendedores/001/V/rutas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].codigo", is("R01")));
    }

    @Test
    void setRutas_datosValidos_retornaListaActualizada() throws Exception {
        when(service.asignarRutas("001", "V", List.of("R01", "R02")))
                .thenReturn(List.of(dto("R01"), dto("R02")));

        mockMvc.perform(put("/api/vendedores/001/V/rutas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of("R01", "R02"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void setRutas_vendedorNoExiste_retorna404() throws Exception {
        when(service.asignarRutas("999", "V", List.of("R01")))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendedor no encontrado"));

        mockMvc.perform(put("/api/vendedores/999/V/rutas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of("R01"))))
                .andExpect(status().isNotFound());
    }
}
