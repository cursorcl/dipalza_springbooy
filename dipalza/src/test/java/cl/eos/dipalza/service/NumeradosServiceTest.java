package cl.eos.dipalza.service;

import cl.eos.dipalza.entity.Numerado;
import cl.eos.dipalza.entity.Producto;
import cl.eos.dipalza.mapper.NumeradoMapper;
import cl.eos.dipalza.model.NumeradoDTO;
import cl.eos.dipalza.repository.NumeradoRepository;
import cl.eos.dipalza.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NumeradosServiceTest {

    @Mock NumeradoRepository numeradoRepo;
    @Mock ProductoRepository productoRepo;
    @Mock NumeradoMapper mapper;
    @InjectMocks NumeradosService service;

    private Numerado numerado(Long id, String productoId, BigDecimal peso) {
        Numerado n = new Numerado();
        n.setId(id);
        n.setEstado("D");
        n.setNumero(1);
        n.setPeso(peso);
        Producto p = new Producto();
        p.setArticulo(productoId);
        n.setProducto(p);
        return n;
    }

    private NumeradoDTO dto(Long id) {
        NumeradoDTO d = new NumeradoDTO();
        d.setId(id);
        d.setCodigoProducto("ART001");
        d.setNumero(1);
        d.setPeso(BigDecimal.valueOf(10));
        return d;
    }

    @Test
    void findAll_listaVacia_retornaEmpty() {
        when(numeradoRepo.findAll()).thenReturn(List.of());
        assertThat(service.findAll()).isEmpty();
    }

    @Test
    void findAll_conElementos_retornaDTOs() {
        when(numeradoRepo.findAll()).thenReturn(List.of(numerado(1L, "ART001", BigDecimal.TEN)));
        when(mapper.toDTO(any())).thenReturn(dto(1L));
        assertThat(service.findAll()).hasSize(1);
    }

    @Test
    void findByProducto_conElementos_retornaDTOs() {
        when(numeradoRepo.findByProductoId("ART001")).thenReturn(List.of(numerado(1L, "ART001", BigDecimal.TEN)));
        when(mapper.toDTO(any())).thenReturn(dto(1L));
        assertThat(service.findByProducto("ART001")).hasSize(1);
    }

    @Test
    void findById_existente_retornaDTO() {
        when(numeradoRepo.findById(1L)).thenReturn(Optional.of(numerado(1L, "ART001", BigDecimal.TEN)));
        when(mapper.toDTO(any())).thenReturn(dto(1L));
        assertThat(service.findById(1L)).isNotNull().extracting(NumeradoDTO::getId).isEqualTo(1L);
    }

    @Test
    void save_productoNoExiste_retornaNull() {
        when(productoRepo.findByArticulo("NOEXISTE")).thenReturn(null);
        NumeradoDTO d = dto(null);
        d.setCodigoProducto("NOEXISTE");
        assertThat(service.save(d)).isNull();
    }

    @Test
    void save_productoExiste_guardaYRetornaDTO() {
        Producto prod = new Producto();
        prod.setArticulo("ART001");
        when(productoRepo.findByArticulo("ART001")).thenReturn(prod);
        when(numeradoRepo.findById(any())).thenReturn(Optional.empty());
        Numerado saved = numerado(1L, "ART001", BigDecimal.TEN);
        when(numeradoRepo.save(any())).thenReturn(saved);
        when(mapper.toDTO(any(Numerado.class))).thenReturn(dto(1L));

        NumeradoDTO result = service.save(dto(null));
        assertThat(result).isNotNull();
    }

    @Test
    void deleteById_llamaAlRepo() {
        service.deleteById(5L);
        verify(numeradoRepo).deleteById(5L);
    }

    @Test
    void findPrecioPromedio_listaVacia_retornaCero() {
        when(numeradoRepo.findByProductoId("ART001")).thenReturn(List.of());
        assertThat(service.findPrecioPromedioArticulo("ART001")).isEqualTo(0f);
    }

    @Test
    void findPrecioPromedio_conElementos_retornaPromedio() {
        List<Numerado> lista = List.of(
                numerado(1L, "ART001", BigDecimal.valueOf(10)),
                numerado(2L, "ART001", BigDecimal.valueOf(20))
        );
        when(numeradoRepo.findByProductoId("ART001")).thenReturn(lista);
        assertThat(service.findPrecioPromedioArticulo("ART001")).isEqualTo(15f);
    }
}
