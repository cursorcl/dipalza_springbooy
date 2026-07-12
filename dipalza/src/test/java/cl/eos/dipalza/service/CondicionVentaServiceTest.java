package cl.eos.dipalza.service;

import cl.eos.dipalza.entity.CondicionVenta;
import cl.eos.dipalza.mapper.CondicionVentaMapper;
import cl.eos.dipalza.model.CondicionVentaDTO;
import cl.eos.dipalza.repository.CondicionVentaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CondicionVentaServiceTest {

    @Mock CondicionVentaRepository repo;
    @Mock CondicionVentaMapper mapper;
    @InjectMocks CondicionVentaService service;

    private CondicionVenta entidad(String codigo) {
        CondicionVenta e = new CondicionVenta();
        e.setCodigo(codigo);
        e.setDescripcion("Contado");
        return e;
    }

    private CondicionVentaDTO dto(String codigo) {
        CondicionVentaDTO d = new CondicionVentaDTO();
        d.setCodigo(codigo);
        d.setDescripcion("Contado");
        return d;
    }

    @Test
    void getAllCondicionVentas_retornaListaMapeada() {
        when(repo.findAll()).thenReturn(List.of(entidad("01")));
        when(mapper.toDTO(any())).thenReturn(dto("01"));

        List<CondicionVentaDTO> result = service.getAllCondicionVentas();
        assertThat(result).hasSize(1).first().extracting(CondicionVentaDTO::getCodigo).isEqualTo("01");
    }

    @Test
    void getCondicionVentaById_existente_retornaDTO() {
        when(repo.findById("01")).thenReturn(Optional.of(entidad("01")));
        when(mapper.toDTO(any())).thenReturn(dto("01"));

        assertThat(service.getCondicionVentaById("01")).isPresent()
                .get().extracting(CondicionVentaDTO::getCodigo).isEqualTo("01");
    }

    @Test
    void getCondicionVentaById_noExiste_retornaEmpty() {
        when(repo.findById("99")).thenReturn(Optional.empty());
        assertThat(service.getCondicionVentaById("99")).isEmpty();
    }

    @Test
    void deleteCondicionVenta_existente_retornaTrue() {
        when(repo.existsById("01")).thenReturn(true);
        assertThat(service.deleteCondicionVenta("01")).isTrue();
        verify(repo).deleteById("01");
    }
}
