package cl.eos.dipalza.service;

import cl.eos.dipalza.entity.Cliente;
import cl.eos.dipalza.entity.CondicionVenta;
import cl.eos.dipalza.entity.EstadoVenta;
import cl.eos.dipalza.entity.Producto;
import cl.eos.dipalza.entity.Ruta;
import cl.eos.dipalza.entity.Vendedor;
import cl.eos.dipalza.entity.Venta;
import cl.eos.dipalza.entity.VentaDetalle;
import cl.eos.dipalza.entity.ids.ClienteId;
import cl.eos.dipalza.entity.ids.VendedorId;
import cl.eos.dipalza.model.venta.VentaDTO;
import cl.eos.dipalza.repository.ClienteRepository;
import cl.eos.dipalza.repository.CondicionVentaRepository;
import cl.eos.dipalza.repository.ProductoRepository;
import cl.eos.dipalza.repository.RutaRepository;
import cl.eos.dipalza.repository.VendedorRepository;
import cl.eos.dipalza.repository.VentaRepository;
import cl.eos.dipalza.service.resultados.VentaFacturaResultado;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de integración de {@link FacturacionService} contra la base de datos de
 * prueba real (192.168.100.102), con los procesadores reales
 * ({@code VentaItemPorcessorNoNumerado} / {@code VentaItemProcessorNumerado}) en
 * lugar de mocks.
 *
 * <p>Crea una venta FINISHED real con un solo detalle (producto no numerado),
 * factura SOLO esa venta (nunca el {@code facturar()} sin argumentos, que
 * procesaría cualquier otra venta FINISHED real que exista en el ambiente
 * compartido) y limpia todo lo que crea al finalizar.</p>
 */
@SpringBootTest
@ActiveProfiles({"dev-nosec", "it"})
class FacturacionServiceIT {

    private static final String ARTICULO_TEST = "012";
    private static final BigDecimal STOCK_VENTAS_BASE_DE_PRUEBA = new BigDecimal("50");

    @Autowired
    private FacturacionService facturacionService;
    @Autowired
    private VentaRepository ventaRepository;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private VendedorRepository vendedorRepository;
    @Autowired
    private RutaRepository rutaRepository;
    @Autowired
    private CondicionVentaRepository condicionVentaRepository;
    @Autowired
    @Qualifier("facturacionJdbcTemplate")
    private JdbcTemplate jdbcMastersoft;

    private BigDecimal stockVentasOriginal;
    private Long ventaIdCreada;
    private String identificadorCreado;
    private String nroFacturaCreado;

    @BeforeEach
    void prepararBaseline() {
        Producto producto = productoRepository.findById(ARTICULO_TEST).orElseThrow();
        stockVentasOriginal = producto.getStockVentas();
        // Se fija un valor base alto para que el descuento (correcto o duplicado) no choque con el piso en cero.
        producto.setStockVentas(STOCK_VENTAS_BASE_DE_PRUEBA);
        productoRepository.save(producto);
    }

    @AfterEach
    void limpiar() {
        if (identificadorCreado != null) {
            jdbcMastersoft.update("DELETE FROM detalledocumento WHERE Id = ?", identificadorCreado);
            jdbcMastersoft.update("DELETE FROM totaldocumento WHERE Id = ?", identificadorCreado);
            jdbcMastersoft.update("DELETE FROM MSOSTVENTASILA WHERE numero = ?", nroFacturaCreado);
            jdbcMastersoft.update("DELETE FROM ctadocto WHERE numero = ?", nroFacturaCreado);
            jdbcMastersoft.update("DELETE FROM folios WHERE numero = ? AND tipo = '06'", nroFacturaCreado);
            jdbcMastersoft.update("DELETE FROM encabezadocumento WHERE Id = ?", identificadorCreado);
        }
        if (ventaIdCreada != null) {
            ventaRepository.deleteById(ventaIdCreada);
        }
        if (stockVentasOriginal != null) {
            Producto producto = productoRepository.findById(ARTICULO_TEST).orElseThrow();
            producto.setStockVentas(stockVentasOriginal);
            productoRepository.save(producto);
        }
    }

    @Test
    void facturar_ventaReal_creaDocumentosEnMastersoftYDescuentaStockVentasUnaSolaVez() {
        Cliente cliente = clienteRepository.findById(new ClienteId("0762235587", "001")).orElseThrow();
        Vendedor vendedor = vendedorRepository.findById(new VendedorId("002", "0")).orElseThrow();
        Ruta ruta = rutaRepository.findById("004").orElseThrow();
        CondicionVenta condicionVenta = condicionVentaRepository.findById("001").orElseThrow();
        Producto producto = productoRepository.findById(ARTICULO_TEST).orElseThrow();

        Venta venta = new Venta();
        venta.setCliente(cliente);
        venta.setVendedor(vendedor);
        venta.setRuta(ruta);
        venta.setCondicionVenta(condicionVenta);
        venta.setFecha(LocalDate.now());
        venta.setEstado(EstadoVenta.FINISHED);
        venta.setTotalNeto(BigDecimal.ZERO);
        venta.setTotal(BigDecimal.ZERO);
        venta.setTotalDescuento(BigDecimal.ZERO);
        venta.setTotalIla(BigDecimal.ZERO);
        venta.setTotalIva(BigDecimal.ZERO);

        BigDecimal cantidad = new BigDecimal("3");
        VentaDetalle detalle = new VentaDetalle();
        detalle.setVenta(venta);
        detalle.setProducto(producto);
        detalle.setCantidad(cantidad);
        detalle.setPrecioUnitario(producto.getVentaNeto());
        detalle.setPorcentajeDescuento(BigDecimal.ZERO);
        detalle.setPorcentajeIva(new BigDecimal("19"));
        detalle.setPorcentajeIla(BigDecimal.ZERO);
        detalle.setTotalDescuento(BigDecimal.ZERO);
        detalle.setTotalIla(BigDecimal.ZERO);
        detalle.setTotalIva(BigDecimal.ZERO);
        detalle.setTotalLinea(BigDecimal.ZERO);
        detalle.setUnidad(producto.getUnidad());
        detalle.setPiezas(BigDecimal.ZERO); // no numerado -> VentaItemPorcessorNoNumerado
        venta.setDetalles(List.of(detalle));

        Venta ventaGuardada = ventaRepository.saveAndFlush(venta);
        ventaIdCreada = ventaGuardada.getId();

        // DTO mínimo: procesarVenta() vuelve a leer los detalles reales desde la BD por id de venta.
        VentaDTO ventaDTO = new VentaDTO();
        ventaDTO.setId(ventaGuardada.getId());
        ventaDTO.setEstadoVenta(EstadoVenta.FINISHED.name());
        ventaDTO.setFecha(venta.getFecha());
        ventaDTO.setRutCliente("0762235587");
        ventaDTO.setCodigoCliente("001");
        ventaDTO.setCodigoVendedor("002");
        ventaDTO.setCodigoCondicionVenta("001");

        List<VentaFacturaResultado> resultados = facturacionService.facturar(List.of(ventaDTO));

        assertThat(resultados).hasSize(1);
        VentaFacturaResultado resultado = resultados.get(0);
        nroFacturaCreado = resultado.factura();
        assertThat(nroFacturaCreado).isNotBlank();

        identificadorCreado = jdbcMastersoft.queryForObject(
                "SELECT Id FROM encabezadocumento WHERE Tipo = '06' AND Numero = ?", String.class, nroFacturaCreado);
        assertThat(identificadorCreado).isNotNull();

        Integer filasDetalle = jdbcMastersoft.queryForObject(
                "SELECT COUNT(*) FROM detalledocumento WHERE Id = ?", Integer.class, identificadorCreado);
        assertThat(filasDetalle).isEqualTo(1);

        Integer filasFolio = jdbcMastersoft.queryForObject(
                "SELECT COUNT(*) FROM folios WHERE numero = ? AND tipo = '06'", Integer.class, nroFacturaCreado);
        assertThat(filasFolio).isEqualTo(1);

        Venta ventaRecargada = ventaRepository.findById(ventaIdCreada).orElseThrow();
        assertThat(ventaRecargada.getEstado()).isEqualTo(EstadoVenta.CLOSED);

        // stockVentas tiene DOS movimientos en este flujo, ninguno duplicado:
        // 1) el trigger de BD `tgr_ventadetalle_producto` (sobre venta_detalle) SUMA la
        //    cantidad al crear la venta (mientras no esté CERRADA) — reserva el stock.
        // 2) VentaItemPorcessorNoNumerado.actualizarStockProducto() RESTA esa misma
        //    cantidad al facturar — libera la reserva. FacturacionService ya NO descuenta
        //    una segunda vez (ver fix del descuento duplicado), así que el neto de
        //    crear + facturar una venta vuelve al valor base.
        Producto productoRecargado = productoRepository.findById(ARTICULO_TEST).orElseThrow();

        assertThat(productoRecargado.getStockVentas()).isEqualByComparingTo(STOCK_VENTAS_BASE_DE_PRUEBA);
    }
}
