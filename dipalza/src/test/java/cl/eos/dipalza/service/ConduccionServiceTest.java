package cl.eos.dipalza.service;

import cl.eos.dipalza.entity.Conduccion;
import cl.eos.dipalza.mapper.ConduccionMapper;
import cl.eos.dipalza.model.ConduccionDTO;
import cl.eos.dipalza.repository.ConduccionRepository;
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
class ConduccionServiceTest {

    @Mock ConduccionRepository repo;
    @Mock ConduccionMapper mapper;
    @InjectMocks ConduccionService service;

    private Conduccion entidad(String codigo) {
        Conduccion e = new Conduccion();
        e.setCodigo(codigo);
        e.setDescripcion("Camión");
        return e;
    }

    private ConduccionDTO dto(String codigo) {
        ConduccionDTO d = new ConduccionDTO();
        d.setCodigo(codigo);
        d.setDescripcion("Camión");
        d.setValor(BigDecimal.valueOf(1500));
        return d;
    }

    @Test
    void getAllConduccions_retornaListaMapeada() {
        when(repo.findAll()).thenReturn(List.of(entidad("C1")));
        when(mapper.toDTO(any())).thenReturn(dto("C1"));

        List<ConduccionDTO> result = service.getAllConduccions();
        assertThat(result).hasSize(1);
    }

    @Test
    void getConduccionById_existente_retornaDTO() {
        when(repo.findById("C1")).thenReturn(Optional.of(entidad("C1")));
        when(mapper.toDTO(any())).thenReturn(dto("C1"));

        assertThat(service.getConduccionById("C1")).isPresent();
    }

    @Test
    void getConduccionById_noExiste_retornaEmpty() {
        when(repo.findById("XX")).thenReturn(Optional.empty());
        assertThat(service.getConduccionById("XX")).isEmpty();
    }

    @Test
    void deleteConduccion_noExiste_retornaFalse() {
        when(repo.existsById("XX")).thenReturn(false);
        assertThat(service.deleteConduccion("XX")).isFalse();
        verify(repo, never()).deleteById(any());
    }
}
