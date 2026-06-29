package cl.eos.dipalza.service;

import cl.eos.dipalza.entity.EstadoVenta;
import cl.eos.dipalza.entity.Producto;
import cl.eos.dipalza.entity.Venta;
import cl.eos.dipalza.entity.VentaDetalle;
import cl.eos.dipalza.mapper.ProductoMapper;
import cl.eos.dipalza.model.venta.VentaDTO;
import cl.eos.dipalza.repository.IlaRepository;
import cl.eos.dipalza.repository.ProductoRepository;
import cl.eos.dipalza.repository.VentaDetalleRepository;
import cl.eos.dipalza.repository.VentaRepository;
import cl.eos.dipalza.service.grabacion.VentaItemContext;
import cl.eos.dipalza.service.grabacion.VentaItemProcessor;
import cl.eos.dipalza.service.grabacion.VentaItemProcessorResolver;
import cl.eos.dipalza.service.resultados.VentaFacturaResultado;
import cl.eos.dipalza.service.resultados.VentaItemResultado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de {@link FacturacionService}.
 *
 * <p>Todas las dependencias (JDBC hacia Mastersoft, repositorios JPA, configuración,
 * transaction manager) se mockean: no se toca ninguna base de datos real. Para
 * pruebas contra la base de datos de prueba real, ver {@code FacturacionServiceIT}.</p>
 */
@ExtendWith(MockitoExtension.class)
class FacturacionServiceTest {

    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private JdbcTemplate jdbcFacturacionTemplate;
    @Mock
    private VentaItemProcessorResolver resolver;
    @Mock
    private VentaRepository ventaRepository;
    @Mock
    private VentaDetalleRepository ventaDetalleRepository;
    @Mock
    private ConfiguracionService configuracion;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private IlaRepository ilaRepository;
    @Mock
    private ProductoMapper productoMapper;
    @Mock
    private VentaItemProcessor processor;

    private FacturacionService service;

    @BeforeEach
    void setUp() {
        // Por defecto: el valor real configurado en producción para NUMERO_LINEAS_FACTURA.
        service = crearService(25);
    }

    private FacturacionService crearService(int nroLineasPorFactura) {
        when(configuracion.getBoolean(FacturacionService.FACTURA_ELECTRONICA)).thenReturn(true);
        when(configuracion.getInt(FacturacionService.NUMERO_LINEAS_FACTURA)).thenReturn(nroLineasPorFactura);
        return new FacturacionService(
                transactionManager,
                jdbcFacturacionTemplate,
                resolver,
                ventaRepository,
                ventaDetalleRepository,
                configuracion,
                productoRepository,
                ilaRepository,
                productoMapper
        );
    }

    // -------------------------------------------------------------------------
    // Helpers de datos
    // -------------------------------------------------------------------------

    private VentaDTO ventaDTO(Long id, EstadoVenta estado) {
        VentaDTO dto = new VentaDTO();
        dto.setId(id);
        dto.setEstadoVenta(estado.name());
        dto.setFecha(LocalDate.of(2026, 1, 15));
        dto.setRutCliente("11111111-1");
        dto.setCodigoCliente("001");
        dto.setCodigoVendedor("V01");
        dto.setCodigoCondicionVenta("CTD");
        return dto;
    }

    private VentaDetalle detalleEntity(Long id, Venta venta, String articulo, BigDecimal cantidad, BigDecimal piezas) {
        Producto producto = new Producto();
        producto.setArticulo(articulo);
        producto.setDescripcion("Producto " + articulo);
        producto.setVentaNeto(new BigDecimal("100"));
        producto.setStockVentas(new BigDecimal("50"));
        producto.setPiezasVentas(new BigDecimal("50"));

        VentaDetalle detalle = new VentaDetalle();
        detalle.setId(id);
        detalle.setVenta(venta);
        detalle.setProducto(producto);
        detalle.setCantidad(cantidad);
        detalle.setPrecioUnitario(new BigDecimal("100"));
        detalle.setPorcentajeDescuento(BigDecimal.ZERO);
        detalle.setPorcentajeIva(new BigDecimal("19"));
        detalle.setPorcentajeIla(BigDecimal.ZERO);
        detalle.setTotalDescuento(BigDecimal.ZERO);
        detalle.setTotalIla(BigDecimal.ZERO);
        detalle.setTotalIva(BigDecimal.ZERO);
        detalle.setTotalLinea(BigDecimal.ZERO);
        detalle.setUnidad("KG");
        detalle.setPiezas(piezas == null ? BigDecimal.ZERO : piezas);
        detalle.setPiezasUsadas(new ArrayList<>());
        return detalle;
    }

    private Venta ventaEntity(Long id, EstadoVenta estado) {
        Venta venta = new Venta();
        venta.setId(id);
        venta.setEstado(estado);
        // VentaMapper.toVentaDTO solo copia estadoVenta al DTO si condicionVenta != null
        // (ver hallazgo documentado en facturarSinArgumentos_delegaEnFacturarConVentasFinishedMapeadas).
        cl.eos.dipalza.entity.CondicionVenta condicionVenta = new cl.eos.dipalza.entity.CondicionVenta();
        condicionVenta.setCodigo("CTD");
        condicionVenta.setDescripcion("Contado");
        venta.setCondicionVenta(condicionVenta);
        return venta;
    }

    private VentaItemResultado itemResultadoExitoso(String articulo, float ventaNeta, float iva, float ila, float descuento) {
        return new VentaItemResultado(articulo, 1, 100f, ventaNeta, 1f, 0f, iva, ila, descuento, null, null);
    }

    /** Configura los stubs mínimos para que un identificador/folio se generen sin error. */
    private void stubFolioYIdentificadorExitosos(String identificador, long numeroFolio) {
        when(jdbcFacturacionTemplate.queryForObject(contains("PARAMETROS"), eq(String.class)))
                .thenReturn(identificador);
        when(jdbcFacturacionTemplate.queryForObject(contains("folios"), eq(Long.class), any(Object[].class)))
                .thenReturn(numeroFolio);
    }

    @SuppressWarnings("unchecked")
    private void stubConsultasAuxiliaresSinDatos() {
        // obtenerComisionVendedor / obtenerNumeroDiasAsociadosCondicionVenta (mismo overload: ResultSetExtractor)
        when(jdbcFacturacionTemplate.query(anyString(), any(ResultSetExtractor.class), any(Object[].class)))
                .thenReturn(BigDecimal.ZERO);
        // grabarIla (RowMapper / DataClassRowMapper)
        when(jdbcFacturacionTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of());
    }

    private static String contains(String fragment) {
        return org.mockito.ArgumentMatchers.argThat(s -> s != null && s.toUpperCase().contains(fragment.toUpperCase()));
    }

    // -------------------------------------------------------------------------
    // Validaciones / casos borde
    // -------------------------------------------------------------------------

    @Test
    void facturar_ventaNula_retornaMensajeYNoConsultaDetalles() {
        List<VentaDTO> ventas = new ArrayList<>();
        ventas.add(null);

        List<VentaFacturaResultado> resultados = service.facturar(ventas);

        assertThat(resultados).hasSize(1);
        assertThat(resultados.get(0).mensaje()).contains("vacío");
        verify(ventaDetalleRepository, never()).findAllOptimizedByVentaId(anyLong());
    }

    @Test
    void facturar_estadoNoFinished_retornaMensajeDeEstadoInvalidoYNoConsultaDetalles() {
        VentaDTO venta = ventaDTO(5L, EstadoVenta.OPENED);

        List<VentaFacturaResultado> resultados = service.facturar(List.of(venta));

        assertThat(resultados).hasSize(1);
        assertThat(resultados.get(0).mensaje()).contains("no permite su facturación");
        verify(ventaDetalleRepository, never()).findAllOptimizedByVentaId(anyLong());
    }

    @Test
    void facturar_sinDetalles_retornaMensajeSinDetalles() {
        VentaDTO venta = ventaDTO(7L, EstadoVenta.FINISHED);
        when(ventaDetalleRepository.findAllOptimizedByVentaId(7L)).thenReturn(List.of());

        List<VentaFacturaResultado> resultados = service.facturar(List.of(venta));

        assertThat(resultados).hasSize(1);
        assertThat(resultados.get(0).mensaje()).contains("no tiene detalles");
        verifyNoInteractions(jdbcFacturacionTemplate);
    }

    @Test
    void facturar_productoNoExisteEnBdDestino_relanzaComoRuntimeExceptionYQuedaPendiente() {
        Long ventaId = 8L;
        Venta ventaEnt = ventaEntity(ventaId, EstadoVenta.FINISHED);
        VentaDTO venta = ventaDTO(ventaId, EstadoVenta.FINISHED);
        VentaDetalle detalle = detalleEntity(1L, ventaEnt, "ART1", BigDecimal.TEN, BigDecimal.ZERO);

        when(ventaDetalleRepository.findAllOptimizedByVentaId(ventaId)).thenReturn(List.of(detalle));
        stubFolioYIdentificadorExitosos("0000000001", 1L);
        when(resolver.resolve(any(VentaItemContext.class)))
                .thenThrow(new EmptyResultDataAccessException(1));

        // facturar(List) envuelve la ejecución en try/catch: la venta queda reportada como
        // pendiente (en vez de desaparecer silenciosamente) y nunca se marca CLOSED.
        List<VentaFacturaResultado> resultados = service.facturar(List.of(venta));

        assertThat(resultados).hasSize(1);
        assertThat(resultados.get(0).mensaje()).contains("pendiente").contains("Producto no encontrado");
        verify(ventaRepository, never()).save(any());
    }

    @Test
    void facturar_segundaVentaSigueProcesandoseAunqueLaPrimeraFalle() {
        Long idFalla = 8L;
        Long idOk = 9L;

        Venta ventaFallaEnt = ventaEntity(idFalla, EstadoVenta.FINISHED);
        VentaDTO ventaFalla = ventaDTO(idFalla, EstadoVenta.FINISHED);
        VentaDetalle detalleFalla = detalleEntity(1L, ventaFallaEnt, "ART1", BigDecimal.TEN, BigDecimal.ZERO);

        Venta ventaOkEnt = ventaEntity(idOk, EstadoVenta.FINISHED);
        VentaDTO ventaOk = ventaDTO(idOk, EstadoVenta.FINISHED);
        VentaDetalle detalleOk = detalleEntity(2L, ventaOkEnt, "ART2", BigDecimal.ONE, BigDecimal.ZERO);

        when(ventaDetalleRepository.findAllOptimizedByVentaId(idFalla)).thenReturn(List.of(detalleFalla));
        when(ventaDetalleRepository.findAllOptimizedByVentaId(idOk)).thenReturn(List.of(detalleOk));
        stubFolioYIdentificadorExitosos("0000000001", 1L);
        stubConsultasAuxiliaresSinDatos();

        when(resolver.resolve(any(VentaItemContext.class)))
                .thenThrow(new EmptyResultDataAccessException(1)) // primera venta falla
                .thenReturn(processor); // segunda venta sigue
        when(processor.procesar(any())).thenReturn(itemResultadoExitoso("ART2", 100f, 19f, 0f, 0f));
        when(ventaRepository.findByIdOptimized(idOk)).thenReturn(Optional.of(ventaOkEnt));

        List<VentaFacturaResultado> resultados = service.facturar(List.of(ventaFalla, ventaOk));

        // La venta que falla queda reportada como pendiente; la siguiente se factura con éxito.
        assertThat(resultados).hasSize(2);
        assertThat(resultados.get(0).mensaje()).contains("pendiente");
        assertThat(resultados.get(1).factura()).isEqualTo("0000001");
    }

    // -------------------------------------------------------------------------
    // Flujo feliz
    // -------------------------------------------------------------------------

    @Test
    void facturar_flujoFeliz_grabaEncabezadoTotalCuentaYFolioYCierraLaVenta() {
        Long ventaId = 100L;
        Venta ventaEnt = ventaEntity(ventaId, EstadoVenta.FINISHED);
        VentaDTO venta = ventaDTO(ventaId, EstadoVenta.FINISHED);
        VentaDetalle detalle = detalleEntity(1L, ventaEnt, "ART1", new BigDecimal("5"), BigDecimal.ZERO);

        when(ventaDetalleRepository.findAllOptimizedByVentaId(ventaId)).thenReturn(List.of(detalle));
        stubFolioYIdentificadorExitosos("0000000123", 41L);
        stubConsultasAuxiliaresSinDatos();
        when(jdbcFacturacionTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

        when(resolver.resolve(any(VentaItemContext.class))).thenReturn(processor);
        when(processor.procesar(any(VentaItemContext.class)))
                .thenReturn(itemResultadoExitoso("ART1", 500f, 95f, 10f, 0f));
        when(ventaRepository.findByIdOptimized(ventaId)).thenReturn(Optional.of(ventaEnt));

        List<VentaFacturaResultado> resultados = service.facturar(List.of(venta));

        assertThat(resultados).hasSize(1);
        VentaFacturaResultado resultado = resultados.get(0);
        assertThat(resultado.factura()).isEqualTo("0000041");
        assertThat(resultado.items()).hasSize(1);

        // Se grabó el encabezado de la factura (insert into encabezadocumento ...)
        verify(jdbcFacturacionTemplate, times(1))
                .update(contains("insert into encabezadocumento"), any(Object[].class));

        // Se grabó el total del documento, la cuenta contable y el folio
        verify(jdbcFacturacionTemplate, times(1))
                .update(contains("insert into totaldocumento"), any(Object[].class));
        verify(jdbcFacturacionTemplate, times(1))
                .update(contains("insert into ctadocto"), any(Object[].class));
        verify(jdbcFacturacionTemplate, times(1))
                .update(eq("INSERT INTO folios (numero, tipo1, tipo) VALUES (?, ?, ?)"), eq("0000041"), eq("E"), eq("06"));

        // La venta queda CLOSED
        ArgumentCaptor<Venta> ventaCaptor = ArgumentCaptor.forClass(Venta.class);
        verify(ventaRepository).save(ventaCaptor.capture());
        assertThat(ventaCaptor.getValue().getEstado()).isEqualTo(EstadoVenta.CLOSED);

        // El descuento de stockVentas/piezasVentas ya NO ocurre en FacturacionService: lo hace
        // el VentaItemProcessor (ver VentaItemPorcessorNoNumeradoTest), que aquí está mockeado.
        verifyNoInteractions(productoRepository);
    }

    @Test
    void facturar_productoDeDetalleNoExisteEnVentas_noTocaProductoRepositoryDirectamenteYNoLanzaExcepcion() {
        Long ventaId = 102L;
        Venta ventaEnt = ventaEntity(ventaId, EstadoVenta.FINISHED);
        VentaDTO venta = ventaDTO(ventaId, EstadoVenta.FINISHED);
        VentaDetalle detalle = detalleEntity(1L, ventaEnt, "ART_FANTASMA", BigDecimal.ONE, BigDecimal.ZERO);

        when(ventaDetalleRepository.findAllOptimizedByVentaId(ventaId)).thenReturn(List.of(detalle));
        stubFolioYIdentificadorExitosos("0000000001", 1L);
        stubConsultasAuxiliaresSinDatos();
        when(jdbcFacturacionTemplate.update(anyString(), any(Object[].class))).thenReturn(1);
        when(resolver.resolve(any(VentaItemContext.class))).thenReturn(processor);
        when(processor.procesar(any(VentaItemContext.class)))
                .thenReturn(itemResultadoExitoso("ART_FANTASMA", 100f, 19f, 0f, 0f));
        when(ventaRepository.findByIdOptimized(ventaId)).thenReturn(Optional.of(ventaEnt));

        List<VentaFacturaResultado> resultados = service.facturar(List.of(venta));

        assertThat(resultados).hasSize(1);
        // FacturacionService ya no toca productoRepository: el manejo de "producto no existe"
        // (sin actualizar stock ni lanzar excepción) es responsabilidad del processor.
        verify(productoRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // Múltiples facturas por límite de líneas
    // -------------------------------------------------------------------------

    /**
     * Cuando una venta se divide en varias facturas (por {@code NUMERO_LINEAS_FACTURA}),
     * CADA factura generada debe quedar con su propio total/cuenta/folio registrado —
     * no solo la última. Ver el fix de {@code procesarVenta()} que cierra cada factura
     * (vía {@code cerrarFactura}) antes de abrir la siguiente, y también al terminar el loop.
     */
    @Test
    void facturar_masDetallesQueElLimitePorFactura_registraTotalCuentaYFolioParaCadaFacturaGenerada() {
        FacturacionService servicioLimitado = crearService(2); // máx 2 líneas por factura

        Long ventaId = 200L;
        Venta ventaEnt = ventaEntity(ventaId, EstadoVenta.FINISHED);
        VentaDTO venta = ventaDTO(ventaId, EstadoVenta.FINISHED);

        List<VentaDetalle> detalles = List.of(
                detalleEntity(1L, ventaEnt, "A1", BigDecimal.ONE, BigDecimal.ZERO),
                detalleEntity(2L, ventaEnt, "A2", BigDecimal.ONE, BigDecimal.ZERO),
                detalleEntity(3L, ventaEnt, "A3", BigDecimal.ONE, BigDecimal.ZERO),
                detalleEntity(4L, ventaEnt, "A4", BigDecimal.ONE, BigDecimal.ZERO),
                detalleEntity(5L, ventaEnt, "A5", BigDecimal.ONE, BigDecimal.ZERO)
        );

        when(ventaDetalleRepository.findAllOptimizedByVentaId(ventaId)).thenReturn(detalles);
        // Cada llamada a Parametros/folios devuelve un valor distinto (simula incrementos reales)
        when(jdbcFacturacionTemplate.queryForObject(contains("PARAMETROS"), eq(String.class)))
                .thenReturn("0000000001", "0000000002", "0000000003");
        when(jdbcFacturacionTemplate.queryForObject(contains("folios"), eq(Long.class), any(Object[].class)))
                .thenReturn(1L, 2L, 3L);
        stubConsultasAuxiliaresSinDatos();
        when(jdbcFacturacionTemplate.update(anyString(), any(Object[].class))).thenReturn(1);
        when(resolver.resolve(any(VentaItemContext.class))).thenReturn(processor);
        when(processor.procesar(any(VentaItemContext.class)))
                .thenReturn(itemResultadoExitoso("A", 100f, 19f, 0f, 0f));
        when(ventaRepository.findByIdOptimized(ventaId)).thenReturn(Optional.of(ventaEnt));

        servicioLimitado.facturar(List.of(venta));

        // 5 detalles / 2 por factura = 3 encabezados (2 + 2 + 1)...
        verify(jdbcFacturacionTemplate, times(3))
                .update(contains("insert into encabezadocumento"), any(Object[].class));

        // ...y las 3 facturas deben quedar completamente registradas, cada una con su propio folio.
        verify(jdbcFacturacionTemplate, times(3))
                .update(contains("insert into totaldocumento"), any(Object[].class));
        verify(jdbcFacturacionTemplate, times(3))
                .update(contains("insert into ctadocto"), any(Object[].class));
        verify(jdbcFacturacionTemplate, times(1))
                .update(eq("INSERT INTO folios (numero, tipo1, tipo) VALUES (?, ?, ?)"), eq("0000001"), any(), any());
        verify(jdbcFacturacionTemplate, times(1))
                .update(eq("INSERT INTO folios (numero, tipo1, tipo) VALUES (?, ?, ?)"), eq("0000002"), any(), any());
        verify(jdbcFacturacionTemplate, times(1))
                .update(eq("INSERT INTO folios (numero, tipo1, tipo) VALUES (?, ?, ?)"), eq("0000003"), any(), any());
    }

    /**
     * Mismo escenario que el anterior pero con el valor REAL configurado en producción
     * para {@code NUMERO_LINEAS_FACTURA} (25): 30 detalles deben producir 2 facturas
     * (25 + 5), cada una con su propio total/cuenta/folio.
     */
    @Test
    void facturar_treintaDetallesConLimiteRealDe25_generaDosFacturasCompletas() {
        FacturacionService servicioReal = crearService(25);

        Long ventaId = 201L;
        Venta ventaEnt = ventaEntity(ventaId, EstadoVenta.FINISHED);
        VentaDTO venta = ventaDTO(ventaId, EstadoVenta.FINISHED);

        List<VentaDetalle> detalles = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            detalles.add(detalleEntity((long) i, ventaEnt, "A" + i, BigDecimal.ONE, BigDecimal.ZERO));
        }

        when(ventaDetalleRepository.findAllOptimizedByVentaId(ventaId)).thenReturn(detalles);
        when(jdbcFacturacionTemplate.queryForObject(contains("PARAMETROS"), eq(String.class)))
                .thenReturn("0000000001", "0000000002");
        when(jdbcFacturacionTemplate.queryForObject(contains("folios"), eq(Long.class), any(Object[].class)))
                .thenReturn(1L, 2L);
        stubConsultasAuxiliaresSinDatos();
        when(jdbcFacturacionTemplate.update(anyString(), any(Object[].class))).thenReturn(1);
        when(resolver.resolve(any(VentaItemContext.class))).thenReturn(processor);
        when(processor.procesar(any(VentaItemContext.class)))
                .thenReturn(itemResultadoExitoso("A", 100f, 19f, 0f, 0f));
        when(ventaRepository.findByIdOptimized(ventaId)).thenReturn(Optional.of(ventaEnt));

        servicioReal.facturar(List.of(venta));

        verify(jdbcFacturacionTemplate, times(2))
                .update(contains("insert into encabezadocumento"), any(Object[].class));
        verify(jdbcFacturacionTemplate, times(2))
                .update(contains("insert into totaldocumento"), any(Object[].class));
        verify(jdbcFacturacionTemplate, times(2))
                .update(contains("insert into ctadocto"), any(Object[].class));
        verify(jdbcFacturacionTemplate, times(1))
                .update(eq("INSERT INTO folios (numero, tipo1, tipo) VALUES (?, ?, ?)"), eq("0000001"), any(), any());
        verify(jdbcFacturacionTemplate, times(1))
                .update(eq("INSERT INTO folios (numero, tipo1, tipo) VALUES (?, ?, ?)"), eq("0000002"), any(), any());
    }

    // -------------------------------------------------------------------------
    // Colisión / agotamiento de folios
    // -------------------------------------------------------------------------

    @Test
    void facturar_colisionDeFolioUnaVez_reintentaYContinuaConExito() {
        Long ventaId = 300L;
        Venta ventaEnt = ventaEntity(ventaId, EstadoVenta.FINISHED);
        VentaDTO venta = ventaDTO(ventaId, EstadoVenta.FINISHED);
        VentaDetalle detalle = detalleEntity(1L, ventaEnt, "ART1", BigDecimal.ONE, BigDecimal.ZERO);

        when(ventaDetalleRepository.findAllOptimizedByVentaId(ventaId)).thenReturn(List.of(detalle));
        when(jdbcFacturacionTemplate.queryForObject(contains("PARAMETROS"), eq(String.class)))
                .thenReturn("0000000001");
        when(jdbcFacturacionTemplate.queryForObject(contains("folios"), eq(Long.class), any(Object[].class)))
                .thenThrow(new DuplicateKeyException("colisión"))
                .thenReturn(7L); // segundo intento exitoso
        stubConsultasAuxiliaresSinDatos();
        when(jdbcFacturacionTemplate.update(anyString(), any(Object[].class))).thenReturn(1);
        when(resolver.resolve(any(VentaItemContext.class))).thenReturn(processor);
        when(processor.procesar(any(VentaItemContext.class)))
                .thenReturn(itemResultadoExitoso("ART1", 100f, 19f, 0f, 0f));
        when(ventaRepository.findByIdOptimized(ventaId)).thenReturn(Optional.of(ventaEnt));

        List<VentaFacturaResultado> resultados = service.facturar(List.of(venta));

        assertThat(resultados).hasSize(1);
        assertThat(resultados.get(0).factura()).isEqualTo("0000007");
        verify(jdbcFacturacionTemplate, times(2))
                .queryForObject(contains("folios"), eq(Long.class), any(Object[].class));
    }

    /**
     * Si la generación de folio falla 5 veces seguidas
     * ({@link cl.eos.dipalza.exceptions.NumeroFolioException}), la excepción ya NO se
     * traga: se propaga fuera de {@code procesarVenta()}, lo que hace ROLLBACK de la
     * transacción de esta venta (ninguna escritura en Mastersoft queda confirmada) y la
     * venta permanece intacta en estado FINISHED para reintentarse completa en la
     * próxima corrida de {@code facturar()}. El resultado para esta venta indica
     * explícitamente que quedó pendiente, en vez de reportar éxito.
     */
    @Test
    void facturar_folioAgotaCincoReintentos_dejaLaVentaPendienteSinGrabarNiCerrarNada() {
        Long ventaId = 301L;
        Venta ventaEnt = ventaEntity(ventaId, EstadoVenta.FINISHED);
        VentaDTO venta = ventaDTO(ventaId, EstadoVenta.FINISHED);
        VentaDetalle detalle = detalleEntity(1L, ventaEnt, "ART1", BigDecimal.ONE, BigDecimal.ZERO);

        when(ventaDetalleRepository.findAllOptimizedByVentaId(ventaId)).thenReturn(List.of(detalle));
        when(jdbcFacturacionTemplate.queryForObject(contains("PARAMETROS"), eq(String.class)))
                .thenReturn("0000000001");
        when(jdbcFacturacionTemplate.queryForObject(contains("folios"), eq(Long.class), any(Object[].class)))
                .thenThrow(new DuplicateKeyException("colisión persistente"));

        List<VentaFacturaResultado> resultados = service.facturar(List.of(venta));

        // El resultado indica explícitamente que la venta quedó pendiente (no "exitosamente").
        assertThat(resultados).hasSize(1);
        assertThat(resultados.get(0).items()).isNull();
        assertThat(resultados.get(0).mensaje()).contains("pendiente").contains("folio");

        // El procesador de ítems nunca llegó a invocarse para esa línea.
        verifyNoInteractions(resolver);
        // No se grabó ningún encabezado, total, cuenta o folio real en Mastersoft.
        verify(jdbcFacturacionTemplate, never()).update(anyString(), any(Object[].class));
        // La venta NO se marca como cerrada/facturada: queda intacta para reintentarse.
        verify(ventaRepository, never()).save(any());
        // El stock de ventas pendientes NO se descuenta.
        verify(productoRepository, never()).save(any());
    }

    /**
     * Cuando una venta YA cerró exitosamente una o más facturas en este mismo intento
     * (por {@code NUMERO_LINEAS_FACTURA}) y luego, al abrir una factura posterior, se
     * agota el folio, TODA la venta debe quedar pendiente — incluidas las facturas que
     * ya se habían cerrado en este intento — porque viven en la misma transacción
     * {@code REQUIRES_NEW} y esta hace ROLLBACK completo al propagarse la excepción.
     * Esto evita inconsistencias de "facturación parcial" sin necesitar un estado
     * intermedio en el modelo de datos.
     */
    @Test
    void facturar_folioSeAgotaEnLaSegundaFacturaDeUnaVentaConVarias_revierteTodoYQuedaPendiente() {
        FacturacionService servicioLimitado = crearService(2); // máx 2 líneas por factura

        Long ventaId = 302L;
        Venta ventaEnt = ventaEntity(ventaId, EstadoVenta.FINISHED);
        VentaDTO venta = ventaDTO(ventaId, EstadoVenta.FINISHED);

        List<VentaDetalle> detalles = List.of(
                detalleEntity(1L, ventaEnt, "A1", BigDecimal.ONE, BigDecimal.ZERO),
                detalleEntity(2L, ventaEnt, "A2", BigDecimal.ONE, BigDecimal.ZERO),
                detalleEntity(3L, ventaEnt, "A3", BigDecimal.ONE, BigDecimal.ZERO)
        );

        when(ventaDetalleRepository.findAllOptimizedByVentaId(ventaId)).thenReturn(detalles);
        when(jdbcFacturacionTemplate.queryForObject(contains("PARAMETROS"), eq(String.class)))
                .thenReturn("0000000001");
        // Primera factura (A1, A2) obtiene folio sin problema; la segunda (A3) agota los 5 reintentos.
        when(jdbcFacturacionTemplate.queryForObject(contains("folios"), eq(Long.class), any(Object[].class)))
                .thenReturn(1L)
                .thenThrow(new DuplicateKeyException("colisión persistente"));
        stubConsultasAuxiliaresSinDatos();
        when(jdbcFacturacionTemplate.update(anyString(), any(Object[].class))).thenReturn(1);
        when(resolver.resolve(any(VentaItemContext.class))).thenReturn(processor);
        when(processor.procesar(any(VentaItemContext.class)))
                .thenReturn(itemResultadoExitoso("A", 100f, 19f, 0f, 0f));

        List<VentaFacturaResultado> resultados = servicioLimitado.facturar(List.of(venta));

        assertThat(resultados).hasSize(1);
        assertThat(resultados.get(0).mensaje()).contains("pendiente");

        // La venta no queda cerrada ni el stock descontado: todo se revirtió.
        verify(ventaRepository, never()).save(any());
        verify(productoRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // facturar() sin argumentos
    // -------------------------------------------------------------------------

    @Test
    void facturarSinArgumentos_delegaEnFacturarConVentasFinishedMapeadas() {
        Long ventaId = 400L;
        Venta ventaEnt = ventaEntity(ventaId, EstadoVenta.FINISHED);
        when(ventaRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(List.of(ventaEnt));
        when(ventaDetalleRepository.findAllOptimizedByVentaId(ventaId)).thenReturn(List.of());

        List<VentaFacturaResultado> resultados = service.facturar();

        assertThat(resultados).hasSize(1);
        assertThat(resultados.get(0).mensaje()).contains("no tiene detalles");
        verify(ventaRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class));
    }

    // -------------------------------------------------------------------------
    // Procesador real (no mockeado): confirma que el descuento de stock es simple
    // -------------------------------------------------------------------------

    /**
     * Tras el fix del descuento duplicado, {@code FacturacionService} ya no descuenta
     * stock por su cuenta: el ÚNICO descuento ocurre dentro de
     * {@code VentaItemPorcessorNoNumerado.procesar()} (vía {@code actualizarStockProducto}).
     * Este test usa el processor REAL (no un mock) con un repositorio que simula la
     * persistencia real (cada {@code findById} devuelve una instancia nueva reflejando
     * el último valor guardado), para confirmar sin ambigüedad que el efecto neto es
     * un único descuento (base 50 - cantidad 3 = 47), con un solo {@code save()}.
     */
    @Test
    void facturar_conProcesadorRealNoNumerado_confirmaQueElDescuentoDeStockEsSimpleTrasElFix() {
        java.util.concurrent.atomic.AtomicReference<BigDecimal> stockVentasPersistido =
                new java.util.concurrent.atomic.AtomicReference<>(new BigDecimal("50"));

        when(productoRepository.findById("ART1")).thenAnswer(inv -> {
            Producto p = new Producto();
            p.setArticulo("ART1");
            p.setDescripcion("Producto ART1");
            p.setUnidad("KG");
            p.setStock(new BigDecimal("1000")); // stock ERP, no relacionado a stockVentas
            p.setVentaNeto(new BigDecimal("100"));
            p.setCosto(BigDecimal.ZERO);
            p.setStockVentas(stockVentasPersistido.get());
            p.setPiezasVentas(BigDecimal.ZERO);
            return Optional.of(p);
        });
        doAnswer(inv -> {
            Producto guardado = inv.getArgument(0);
            stockVentasPersistido.set(guardado.getStockVentas());
            return guardado;
        }).when(productoRepository).save(any(Producto.class));

        cl.eos.dipalza.service.grabacion.VentaItemPorcessorNoNumerado procesadorReal =
                new cl.eos.dipalza.service.grabacion.VentaItemPorcessorNoNumerado(
                        transactionManager, jdbcFacturacionTemplate, productoRepository);
        VentaItemProcessorResolver resolverReal =
                new VentaItemProcessorResolver(List.of(procesadorReal));
        FacturacionService servicioConProcesadorReal = new FacturacionService(
                transactionManager, jdbcFacturacionTemplate, resolverReal,
                ventaRepository, ventaDetalleRepository, configuracion,
                productoRepository, ilaRepository, productoMapper);

        Long ventaId = 500L;
        Venta ventaEnt = ventaEntity(ventaId, EstadoVenta.FINISHED);
        VentaDTO venta = ventaDTO(ventaId, EstadoVenta.FINISHED);
        VentaDetalle detalle = detalleEntity(1L, ventaEnt, "ART1", new BigDecimal("3"), BigDecimal.ZERO);
        // El producto del detalle no se usa para el cálculo (el processor real hace su propio findById);
        // solo necesitamos que el mapeo de VentaMapper funcione.
        when(ventaDetalleRepository.findAllOptimizedByVentaId(ventaId)).thenReturn(List.of(detalle));
        stubFolioYIdentificadorExitosos("0000000001", 1L);
        stubConsultasAuxiliaresSinDatos();
        when(jdbcFacturacionTemplate.update(anyString(), any(Object[].class))).thenReturn(1);
        when(ventaRepository.findByIdOptimized(ventaId)).thenReturn(Optional.of(ventaEnt));

        servicioConProcesadorReal.facturar(List.of(venta));

        assertThat(stockVentasPersistido.get()).isEqualByComparingTo("47"); // 50 - 3, un solo descuento
        verify(productoRepository, times(1)).save(any(Producto.class));
    }
}
