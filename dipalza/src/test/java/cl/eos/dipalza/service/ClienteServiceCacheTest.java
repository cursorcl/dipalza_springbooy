package cl.eos.dipalza.service;

import cl.eos.dipalza.config.CacheConfig;
import cl.eos.dipalza.entity.Cliente;
import cl.eos.dipalza.entity.ids.ClienteId;
import cl.eos.dipalza.mapper.ClienteMapper;
import cl.eos.dipalza.model.ClienteDTO;
import cl.eos.dipalza.repository.ClienteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A diferencia de {@link ClienteServiceTest} (unitario puro, con {@code @InjectMocks}),
 * este test carga un contexto Spring real para que el proxy de {@code @Cacheable}/
 * {@code @CacheEvict} esté activo. Usa códigos distintos por test para que el estado
 * del caché (compartido entre tests, ya que el contexto Spring se reutiliza) no
 * contamine otros casos.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {ClienteService.class, ClienteMapper.class, CacheConfig.class})
class ClienteServiceCacheTest {

    @Autowired
    ClienteService clienteService;

    @MockitoBean
    ClienteRepository clienteRepository;

    private Cliente entidad(String rut, String codigo) {
        Cliente c = new Cliente();
        c.setId(new ClienteId(rut, codigo));
        c.setRazon("Test SA");
        return c;
    }

    @Test
    void getClientesByVendedor_segundaLlamada_noVuelveAGolpearElRepositorio() {
        String codigoVendedor = "CACHE-V01";
        when(clienteRepository.findByCodigoVendedorOrderByRazonAsc(codigoVendedor))
                .thenReturn(List.of(entidad("11111111-1", "001")));

        List<ClienteDTO> primera = clienteService.getClientesByVendedor(codigoVendedor);
        List<ClienteDTO> segunda = clienteService.getClientesByVendedor(codigoVendedor);

        assertThat(primera).hasSize(1);
        assertThat(segunda).hasSize(1);
        verify(clienteRepository, times(1)).findByCodigoVendedorOrderByRazonAsc(codigoVendedor);
    }
}
