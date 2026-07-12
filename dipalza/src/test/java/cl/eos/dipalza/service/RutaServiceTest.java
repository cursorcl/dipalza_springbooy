package cl.eos.dipalza.service;

import cl.eos.dipalza.entity.Conduccion;
import cl.eos.dipalza.entity.Ruta;
import cl.eos.dipalza.mapper.RutaMapper;
import cl.eos.dipalza.model.RutaDTO;
import cl.eos.dipalza.repository.ConduccionRepository;
import cl.eos.dipalza.repository.RutaRepository;
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
class RutaServiceTest {

    @Mock RutaRepository rutaRepo;
    @Mock RutaMapper rutaMapper;
    @Mock ConduccionRepository conduccionRepo;
    @InjectMocks RutaService service;

    private Conduccion conduccion(String codigo) {
        Conduccion c = new Conduccion();
        c.setCodigo(codigo);
        c.setDescripcion("Camión " + codigo);
        return c;
    }

    private RutaDTO dto(String codigo, String codigoConduccion) {
        RutaDTO d = new RutaDTO();
        d.setCodigo(codigo);
        d.setDescripcion("Ruta " + codigo);
        d.setCodigoConduccion(codigoConduccion);
        return d;
    }

    @Test
    void getAllRutas_retornaListaMapeada() {
        Ruta r = new Ruta();
        when(rutaRepo.findAll()).thenReturn(List.of(r));
        when(rutaMapper.toDTO(r)).thenReturn(dto("R01", "C1"));

        assertThat(service.getAllRutas()).hasSize(1);
    }

    @Test
    void getRutaById_existente_retornaDTO() {
        Ruta r = new Ruta();
        when(rutaRepo.findById("R01")).thenReturn(Optional.of(r));
        when(rutaMapper.toDTO(r)).thenReturn(dto("R01", "C1"));

        assertThat(service.getRutaById("R01")).isPresent();
    }

    @Test
    void getRutaById_noExiste_retornaEmpty() {
        when(rutaRepo.findById("XX")).thenReturn(Optional.empty());
        assertThat(service.getRutaById("XX")).isEmpty();
    }

    @Test
    void createOrUpdateRuta_conduccionCoincide_usaLaCorrecta() {
        Conduccion c1 = conduccion("C1");
        Conduccion c2 = conduccion("C2");
        when(conduccionRepo.findAll()).thenReturn(List.of(c1, c2));

        Ruta saved = new Ruta();
        when(rutaMapper.toEntity(any(), eq(c2))).thenReturn(saved);
        when(rutaRepo.save(saved)).thenReturn(saved);
        when(rutaMapper.toDTO(saved)).thenReturn(dto("R01", "C2"));

        RutaDTO result = service.createOrUpdateRuta(dto("R01", "C2"));
        assertThat(result.getCodigoConduccion()).isEqualTo("C2");
        verify(rutaMapper).toEntity(any(), eq(c2));
    }

    @Test
    void createOrUpdateRuta_conduccionNoCoincide_usaPrimera() {
        Conduccion c1 = conduccion("C1");
        when(conduccionRepo.findAll()).thenReturn(List.of(c1));

        Ruta saved = new Ruta();
        when(rutaMapper.toEntity(any(), eq(c1))).thenReturn(saved);
        when(rutaRepo.save(saved)).thenReturn(saved);
        when(rutaMapper.toDTO(saved)).thenReturn(dto("R01", "C1"));

        service.createOrUpdateRuta(dto("R01", "NO_EXISTE"));
        verify(rutaMapper).toEntity(any(), eq(c1));
    }
}
