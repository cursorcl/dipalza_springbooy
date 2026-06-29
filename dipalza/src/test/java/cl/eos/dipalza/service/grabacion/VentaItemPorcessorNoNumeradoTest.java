package cl.eos.dipalza.service.grabacion;

import cl.eos.dipalza.entity.Producto;
import cl.eos.dipalza.model.venta.VentaDetalleDTO;
import cl.eos.dipalza.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link VentaItemPorcessorNoNumerado}.
 *
 * <p>Tras el fix del descuento duplicado de stock (ver {@code FacturacionServiceTest}),
 * este processor es la ÚNICA fuente de verdad para descontar {@code stockVentas}/
 * {@code piezasVentas} de un producto no numerado al facturar una línea.</p>
 */
@ExtendWith(MockitoExtension.class)
class VentaItemPorcessorNoNumeradoTest {

    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private JdbcTemplate jdbcFacturacionTemplate;
    @Mock
    private ProductoRepository productoRepository;

    private VentaItemPorcessorNoNumerado processor;

    private Producto producto(String articulo, BigDecimal stockErp, BigDecimal stockVentas, BigDecimal piezasVentas) {
        Producto p = new Producto();
        p.setArticulo(articulo);
        p.setDescripcion("Producto " + articulo);
        p.setVentaNeto(new BigDecimal("100"));
        p.setCosto(BigDecimal.ZERO);
        p.setStock(stockErp);
        p.setStockVentas(stockVentas);
        p.setPiezasVentas(piezasVentas);
        return p;
    }

    private VentaDetalleDTO detalle(String idProducto, BigDecimal cantidad) {
        VentaDetalleDTO d = new VentaDetalleDTO();
        d.setIdProducto(idProducto);
        d.setCantidad(cantidad);
        d.setPrecioUnitario(new BigDecimal("100"));
        d.setPorcentajeDescuento(BigDecimal.ZERO);
        d.setPorcentajeIva(new BigDecimal("19"));
        d.setPorcentajeIla(BigDecimal.ZERO);
        d.setPiezas(BigDecimal.ZERO);
        return d;
    }

    @Test
    void procesar_descuentaStockVentasEnLaCantidadAsignada() {
        processor = new VentaItemPorcessorNoNumerado(transactionManager, jdbcFacturacionTemplate, productoRepository);
        Producto p = producto("ART1", new BigDecimal("100"), new BigDecimal("50"), new BigDecimal("50"));
        when(productoRepository.findById("ART1")).thenReturn(Optional.of(p));
        when(jdbcFacturacionTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

        VentaItemContext context = new VentaItemContext(detalle("ART1", new BigDecimal("5")), "0000001", "ID1", 1);
        processor.procesar(context);

        ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
        verify(productoRepository).save(captor.capture());
        assertThat(captor.getValue().getStockVentas()).isEqualByComparingTo("45"); // 50 - 5
    }

    @Test
    void procesar_cantidadPedidaMayorQueStockVentasDejaStockVentasEnCeroNoNegativo() {
        processor = new VentaItemPorcessorNoNumerado(transactionManager, jdbcFacturacionTemplate, productoRepository);
        // stock ERP alto (no limita cantidadAsignada) pero stockVentas (acumulado pendiente) es bajo.
        Producto p = producto("ART1", new BigDecimal("999"), new BigDecimal("2"), new BigDecimal("2"));
        when(productoRepository.findById("ART1")).thenReturn(Optional.of(p));
        when(jdbcFacturacionTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

        VentaItemContext context = new VentaItemContext(detalle("ART1", new BigDecimal("5")), "0000001", "ID1", 1);
        processor.procesar(context);

        ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
        verify(productoRepository).save(captor.capture());
        assertThat(captor.getValue().getStockVentas()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void procesar_productoNoExiste_noLanzaExcepcionYRetornaError() {
        processor = new VentaItemPorcessorNoNumerado(transactionManager, jdbcFacturacionTemplate, productoRepository);
        when(productoRepository.findById("FANTASMA")).thenReturn(Optional.empty());

        VentaItemContext context = new VentaItemContext(detalle("FANTASMA", BigDecimal.ONE), "0000001", "ID1", 1);
        var resultado = processor.procesar(context);

        assertThat(resultado.error()).contains("No existe el código de producto");
        verify(productoRepository, org.mockito.Mockito.never()).save(any());
    }
}
