package cl.eos.dipalza.service;

import cl.eos.dipalza.entity.Cliente;
import cl.eos.dipalza.entity.ids.ClienteId;
import cl.eos.dipalza.mapper.ClienteMapper;
import cl.eos.dipalza.model.ClienteDTO;
import cl.eos.dipalza.repository.ClienteRepository;
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
class ClienteServiceTest {

    @Mock ClienteRepository repo;
    @Mock ClienteMapper mapper;
    @InjectMocks ClienteService service;

    private Cliente entidad(String rut, String codigo) {
        Cliente c = new Cliente();
        c.setId(new ClienteId(rut, codigo));
        c.setRazon("Test SA");
        return c;
    }

    private ClienteDTO dto(String rut, String codigo) {
        ClienteDTO d = new ClienteDTO();
        d.setRut(rut);
        d.setCodigo(codigo);
        d.setRazon("Test SA");
        return d;
    }

    @Test
    void getAllClientes_retornaListaMapeada() {
        when(repo.findAll()).thenReturn(List.of(entidad("11111111-1", "001")));
        when(mapper.toDTO(any())).thenReturn(dto("11111111-1", "001"));

        assertThat(service.getAllClientes()).hasSize(1);
    }

    @Test
    void getClientesByRuta_retornaClientesDeLaRuta() {
        when(repo.getClienteByCodigoRuta("R01")).thenReturn(List.of(entidad("11111111-1", "001")));
        when(mapper.toDTO(any())).thenReturn(dto("11111111-1", "001"));

        assertThat(service.getClientesByRuta("R01")).hasSize(1);
    }

    @Test
    void getClienteById_existente_retornaDTO() {
        when(repo.findById(new ClienteId("11111111-1", "001"))).thenReturn(Optional.of(entidad("11111111-1", "001")));
        when(mapper.toDTO(any())).thenReturn(dto("11111111-1", "001"));

        assertThat(service.getClienteById("11111111-1", "001")).isPresent();
    }

    @Test
    void getClienteById_noExiste_retornaEmpty() {
        when(repo.findById(new ClienteId("99999999-9", "001"))).thenReturn(Optional.empty());
        assertThat(service.getClienteById("99999999-9", "001")).isEmpty();
    }

    @Test
    void getClientesByVendedor_retornaListaFiltrada() {
        when(repo.findByCodigoVendedorOrderByRazonAsc("V01")).thenReturn(List.of(entidad("11111111-1", "001")));
        when(mapper.toDTO(any())).thenReturn(dto("11111111-1", "001"));

        assertThat(service.getClientesByVendedor("V01")).hasSize(1);
    }

    @Test
    void createOrUpdateCliente_guarda_yRetornaDTO() {
        Cliente saved = entidad("11111111-1", "001");
        when(mapper.toEntity(any())).thenReturn(saved);
        when(repo.save(saved)).thenReturn(saved);
        when(mapper.toDTO(saved)).thenReturn(dto("11111111-1", "001"));

        ClienteDTO result = service.createOrUpdateCliente(dto("11111111-1", "001"));
        assertThat(result.getRut()).isEqualTo("11111111-1");
    }

    @Test
    void deleteCliente_existente_retornaTrue() {
        when(repo.existsById(new ClienteId("11111111-1", "001"))).thenReturn(true);
        assertThat(service.deleteCliente("11111111-1", "001")).isTrue();
        verify(repo).deleteById(new ClienteId("11111111-1", "001"));
    }

    @Test
    void deleteCliente_noExiste_retornaFalse() {
        when(repo.existsById(new ClienteId("99999999-9", "001"))).thenReturn(false);
        assertThat(service.deleteCliente("99999999-9", "001")).isFalse();
    }
}
