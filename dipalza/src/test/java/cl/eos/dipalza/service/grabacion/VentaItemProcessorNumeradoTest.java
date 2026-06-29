package cl.eos.dipalza.service.grabacion;

import cl.eos.dipalza.entity.Numerado;
import cl.eos.dipalza.entity.Producto;
import cl.eos.dipalza.model.venta.VentaDetalleDTO;
import cl.eos.dipalza.repository.NumeradoRepository;
import cl.eos.dipalza.repository.ProductoRepository;
import cl.eos.dipalza.repository.VentaDetallePiezaRepository;
import cl.eos.dipalza.repository.VentaDetalleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link VentaItemProcessorNumerado}.
 *
 * <p>Tras el fix del descuento duplicado de stock, este processor es la ÚNICA fuente
 * de verdad para descontar {@code stockVentas}/{@code piezasVentas}/{@code pieces} de
 * un producto numerado al facturar una línea.</p>
 */
@ExtendWith(MockitoExtension.class)
class VentaItemProcessorNumeradoTest {

    @Mock
    private JdbcTemplate jdbcFacturacionTemplate;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private VentaDetalleRepository ventaDetalleRepository;
    @Mock
    private VentaDetallePiezaRepository ventaDetallePiezaRepository;
    @Mock
    private NumeradoRepository numeradoRepository;

    private VentaItemProcessorNumerado processor;

    private Producto producto(BigDecimal pieces, BigDecimal stockVentas, BigDecimal piezasVentas) {
        Producto p = new Producto();
        p.setArticulo("NUM1");
        p.setDescripcion("Producto numerado");
        p.setVentaNeto(new BigDecimal("100"));
        p.setPieces(pieces);
        p.setStockVentas(stockVentas);
        p.setPiezasVentas(piezasVentas);
        return p;
    }

    private Numerado numerado(long id, int numero, BigDecimal peso) {
        Numerado n = new Numerado();
        n.setId(id);
        n.setNumero(numero);
        n.setPeso(peso);
        n.setEstado("D");
        return n;
    }

    private VentaDetalleDTO detalle(BigDecimal piezas) {
        VentaDetalleDTO d = new VentaDetalleDTO();
        d.setId(1L);
        d.setIdProducto("NUM1");
        d.setCantidad(piezas);
        d.setPiezas(piezas);
        d.setPrecioUnitario(new BigDecimal("100"));
        d.setPorcentajeDescuento(BigDecimal.ZERO);
        d.setPorcentajeIva(new BigDecimal("19"));
        d.setPorcentajeIla(BigDecimal.ZERO);
        return d;
    }

    @Test
    void procesar_cantidadPiezasSolicitadaMayorQuePiezasVentasDejaPiezasVentasEnCeroNoNegativo() {
        processor = new VentaItemProcessorNumerado(
                jdbcFacturacionTemplate, productoRepository, ventaDetalleRepository,
                ventaDetallePiezaRepository, numeradoRepository);

        // 5 piezas disponibles en inventario numerado, pero piezasVentas (acumulado pendiente) es solo 2.
        Producto p = producto(new BigDecimal("5"), new BigDecimal("2"), new BigDecimal("2"));
        when(productoRepository.findById("NUM1")).thenReturn(Optional.of(p));
        when(numeradoRepository.findByProductoIdAndEstadoOrderById("NUM1", "D"))
                .thenReturn(List.of(
                        numerado(1L, 101, new BigDecimal("1")),
                        numerado(2L, 102, new BigDecimal("1")),
                        numerado(3L, 103, new BigDecimal("1"))
                ));

        VentaItemContext context = new VentaItemContext(detalle(new BigDecimal("3")), "0000001", "ID1", 1);
        processor.procesar(context);

        ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
        verify(productoRepository).save(captor.capture());
        assertThat(captor.getValue().getPiezasVentas()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void procesar_descuentaStockVentasYPiezasEnLaCantidadAsignada() {
        processor = new VentaItemProcessorNumerado(
                jdbcFacturacionTemplate, productoRepository, ventaDetalleRepository,
                ventaDetallePiezaRepository, numeradoRepository);

        Producto p = producto(new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("10"));
        when(productoRepository.findById("NUM1")).thenReturn(Optional.of(p));
        when(numeradoRepository.findByProductoIdAndEstadoOrderById("NUM1", "D"))
                .thenReturn(List.of(numerado(1L, 101, new BigDecimal("1.5"))));

        VentaItemContext context = new VentaItemContext(detalle(new BigDecimal("1")), "0000001", "ID1", 1);
        processor.procesar(context);

        ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
        verify(productoRepository).save(captor.capture());
        // cantidadAsignada (stockVentas) se descuenta en pesoRealAsignado (1.5, el peso real del numerado usado).
        assertThat(captor.getValue().getStockVentas()).isEqualByComparingTo("8.5");
        assertThat(captor.getValue().getPiezasVentas()).isEqualByComparingTo("9");
        assertThat(captor.getValue().getPieces()).isEqualByComparingTo("9");
    }
}
