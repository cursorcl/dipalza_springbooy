package cl.eos.dipalza.service;

import cl.eos.dipalza.entity.Ila;
import cl.eos.dipalza.mapper.IlaMapper;
import cl.eos.dipalza.model.IlaDTO;
import cl.eos.dipalza.repository.IlaRepository;
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
class IlaServiceTest {

    @Mock IlaRepository repo;
    @Mock IlaMapper mapper;
    @InjectMocks IlaService service;

    private Ila entidad(String codigo) {
        Ila e = new Ila();
        e.setCodigo(codigo);
        e.setDescripcion("Bebidas alcohólicas");
        return e;
    }

    private IlaDTO dto(String codigo) {
        IlaDTO d = new IlaDTO();
        d.setCodigo(codigo);
        d.setDescripcion("Bebidas alcohólicas");
        d.setValor(BigDecimal.valueOf(27));
        return d;
    }

    @Test
    void findAllByOrderByDescripcionAsc_retornaListaMapeada() {
        when(repo.findAllByOrderByDescripcionAsc()).thenReturn(List.of(entidad("I1")));
        when(mapper.toDTO(any())).thenReturn(dto("I1"));

        List<IlaDTO> result = service.findAllByOrderByDescripcionAsc();
        assertThat(result).hasSize(1).first().extracting(IlaDTO::getCodigo).isEqualTo("I1");
    }

    @Test
    void getIlaById_existente_retornaDTO() {
        when(repo.findById("I1")).thenReturn(Optional.of(entidad("I1")));
        when(mapper.toDTO(any())).thenReturn(dto("I1"));

        assertThat(service.getIlaById("I1")).isPresent();
    }

    @Test
    void getIlaById_noExiste_retornaEmpty() {
        when(repo.findById("XX")).thenReturn(Optional.empty());
        assertThat(service.getIlaById("XX")).isEmpty();
    }

    @Test
    void deleteIla_existente_retornaTrue() {
        when(repo.existsById("I1")).thenReturn(true);
        assertThat(service.deleteIla("I1")).isTrue();
        verify(repo).deleteById("I1");
    }
}
