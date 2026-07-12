package cl.eos.dipalza.service;

import cl.eos.dipalza.entity.Producto;
import cl.eos.dipalza.mapper.ProductoMapper;
import cl.eos.dipalza.model.ProductoDTO;
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
class ProductoServiceTest {

    @Mock ProductoRepository repo;
    @Mock ProductoMapper mapper;
    @InjectMocks ProductoService service;

    private Producto entidad(String articulo) {
        Producto p = new Producto();
        p.setArticulo(articulo);
        p.setDescripcion("Queso Gouda");
        return p;
    }

    private ProductoDTO dto(String articulo) {
        ProductoDTO d = new ProductoDTO();
        d.setArticulo(articulo);
        d.setDescripcion("Queso Gouda");
        d.setVentaNeto(BigDecimal.valueOf(5000));
        return d;
    }

    @Test
    void getAllProductos_retornaListaMapeada() {
        when(repo.findAll()).thenReturn(List.of(entidad("ART001")));
        when(mapper.toDTO(any(Producto.class))).thenReturn(dto("ART001"));

        assertThat(service.getAllProductos()).hasSize(1);
    }

    @Test
    void findProductoById_existente_retornaDTO() {
        when(repo.findById("ART001")).thenReturn(Optional.of(entidad("ART001")));
        when(mapper.toDTO(any(Producto.class))).thenReturn(dto("ART001"));

        assertThat(service.findProductoById("ART001")).isPresent();
    }

    @Test
    void findProductoById_noExiste_retornaEmpty() {
        when(repo.findById("XX")).thenReturn(Optional.empty());
        assertThat(service.findProductoById("XX")).isEmpty();
    }

    @Test
    void createOrUpdateProducto_guarda_yRetornaDTO() {
        Producto saved = entidad("ART001");
        when(mapper.toEntity(any())).thenReturn(saved);
        when(repo.save(saved)).thenReturn(saved);
        when(mapper.toDTO(any(Producto.class))).thenReturn(dto("ART001"));

        ProductoDTO result = service.createOrUpdateProducto(dto("ART001"));
        assertThat(result.getArticulo()).isEqualTo("ART001");
    }

    @Test
    void deleteProducto_existente_retornaTrue() {
        when(repo.existsById("ART001")).thenReturn(true);
        assertThat(service.deleteProducto("ART001")).isTrue();
    }

    @Test
    void deleteProducto_noExiste_retornaFalse() {
        when(repo.existsById("XX")).thenReturn(false);
        assertThat(service.deleteProducto("XX")).isFalse();
    }
}
