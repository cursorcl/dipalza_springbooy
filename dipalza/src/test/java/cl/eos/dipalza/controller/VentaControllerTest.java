package cl.eos.dipalza.controller;

import cl.eos.dipalza.entity.EstadoVenta;
import cl.eos.dipalza.entity.Venta;
import cl.eos.dipalza.model.EstadoVentaDTO;
import cl.eos.dipalza.model.venta.VentaDTO;
import cl.eos.dipalza.model.venta.VentaDetalleDTO;
import cl.eos.dipalza.repository.NumeradoRepository;
import cl.eos.dipalza.service.VentaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = VentaController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class VentaControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean VentaService ventaService;
    @MockBean NumeradoRepository numeradoRepository;

    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    private Venta venta(Long id) {
        Venta v = new Venta();
        v.setId(id);
        v.setFecha(LocalDate.now());
        v.setEstado(EstadoVenta.OPENED);
        return v;
    }

    private VentaDTO ventaDTO(Long id) {
        VentaDTO d = new VentaDTO();
        d.setId(id);
        d.setFecha(LocalDate.now());
        d.setEstadoVenta("OPENED");
        return d;
    }

    @Test
    void listarVentas_retornaLista() throws Exception {
        when(ventaService.listarVentas(any())).thenReturn(List.of(venta(1L)));

        mockMvc.perform(get("/api/ventas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)));
    }

    @Test
    void listarVentasPendingByVendedor_retornaLista() throws Exception {
        when(ventaService.listarVentas(any())).thenReturn(List.of(venta(2L)));

        mockMvc.perform(get("/api/ventas/pending/V01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void grabarVenta_nueva_llamaCrearVenta() throws Exception {
        VentaDTO input = ventaDTO(null);
        when(ventaService.crearVenta(any())).thenReturn(ventaDTO(10L));

        mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(10)));
    }

    @Test
    void grabarVenta_existente_llamaActualizarVenta() throws Exception {
        VentaDTO input = ventaDTO(5L);
        when(ventaService.actualizarVenta(eq(5L), any())).thenReturn(ventaDTO(5L));

        mockMvc.perform(post("/api/ventas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(5)));
    }

    @Test
    void eliminarVenta_existente_retorna204() throws Exception {
        when(ventaService.eliminarVenta(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/ventas/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void eliminarVenta_noExiste_retorna404() throws Exception {
        when(ventaService.eliminarVenta(99L)).thenReturn(false);

        mockMvc.perform(delete("/api/ventas/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminarVenta_conflicto_retorna409() throws Exception {
        when(ventaService.eliminarVenta(anyLong())).thenThrow(new IllegalStateException("no se puede eliminar"));

        mockMvc.perform(delete("/api/ventas/1"))
                .andExpect(status().isConflict());
    }

    @Test
    void obtenerDetalleVenta_retornaLista() throws Exception {
        VentaDetalleDTO det = new VentaDetalleDTO();
        det.setId(1L);
        when(ventaService.obtenerDetallePorVenta(5L)).thenReturn(List.of(det));

        mockMvc.perform(get("/api/ventas/5/detalles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void updateEstadoVenta_estadoValido_retorna200() throws Exception {
        EstadoVentaDTO body = new EstadoVentaDTO(1L, "FINISHED");
        Venta v = venta(1L);
        v.setEstado(EstadoVenta.FINISHED);
        when(ventaService.actualizaEstadoVenta(eq(1L), eq(EstadoVenta.FINISHED))).thenReturn(v);

        mockMvc.perform(post("/api/ventas/updateEstadoVenta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));
    }

    @Test
    void updateEstadoVenta_estadoInvalido_retorna400() throws Exception {
        EstadoVentaDTO body = new EstadoVentaDTO(1L, "INVALID_STATE");

        mockMvc.perform(post("/api/ventas/updateEstadoVenta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }
}
