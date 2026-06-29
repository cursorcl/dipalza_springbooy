package cl.eos.dipalza.controller;

import cl.eos.dipalza.model.ProductoDTO;
import cl.eos.dipalza.service.ProductoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = ProductoController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class ProductoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductoService service;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private ProductoDTO unProducto() {
        ProductoDTO dto = new ProductoDTO();
        dto.setArticulo("ART001");
        dto.setDescripcion("Queso Gouda");
        dto.setVentaNeto(new BigDecimal("5000.00"));
        dto.setPrecioLista2(new BigDecimal("5500.00"));
        dto.setUnidad("KG");
        dto.setStock(new BigDecimal("100.00"));
        dto.setNumbered(false);
        return dto;
    }

    // -------------------------------------------------------------------------
    // GET /api/productos
    // -------------------------------------------------------------------------

    @Test
    void getAllProductos_retornaListaConUnElemento() throws Exception {
        when(service.getAllProductos()).thenReturn(List.of(unProducto()));

        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].articulo", is("ART001")))
                .andExpect(jsonPath("$[0].descripcion", is("Queso Gouda")))
                .andExpect(jsonPath("$[0].ventaNeto", is(5000.00)))
                .andExpect(jsonPath("$[0].precioLista2", is(5500.00)));
    }

    @Test
    void getAllProductos_listaVacia_retorna200YArrayVacio() throws Exception {
        when(service.getAllProductos()).thenReturn(List.of());

        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getAllProductos_precioLista2Nulo_seSerializaComoNull() throws Exception {
        ProductoDTO dto = unProducto();
        dto.setPrecioLista2(null);
        when(service.getAllProductos()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].precioLista2", nullValue()));
    }

    // -------------------------------------------------------------------------
    // GET /api/productos/{articulo}
    // -------------------------------------------------------------------------

    @Test
    void findById_existente_retorna200ConProducto() throws Exception {
        when(service.findProductoById("ART001")).thenReturn(Optional.of(unProducto()));

        mockMvc.perform(get("/api/productos/ART001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.articulo", is("ART001")))
                .andExpect(jsonPath("$.ventaNeto", is(5000.00)))
                .andExpect(jsonPath("$.precioLista2", is(5500.00)));
    }

    @Test
    void findById_noExiste_retorna404() throws Exception {
        when(service.findProductoById("NOEXISTE")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/productos/NOEXISTE"))
                .andExpect(status().isNotFound());
    }

    @Test
    void findById_ambosPrecios_seDevuelvenEnLaRespuesta() throws Exception {
        ProductoDTO dto = unProducto();
        dto.setVentaNeto(new BigDecimal("4800.00"));
        dto.setPrecioLista2(new BigDecimal("5200.00"));
        when(service.findProductoById("ART001")).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/api/productos/ART001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ventaNeto", is(4800.00)))
                .andExpect(jsonPath("$.precioLista2", is(5200.00)));
    }
}
