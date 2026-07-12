package cl.eos.dipalza.mapper;

import cl.eos.dipalza.entity.*;
import cl.eos.dipalza.entity.ids.ClienteId;
import cl.eos.dipalza.entity.ids.VendedorId;
import cl.eos.dipalza.model.venta.VentaDTO;
import cl.eos.dipalza.model.venta.VentaDetalleDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class VentaMapperTest {

    private Venta ventaMinima() {
        Venta v = new Venta();
        v.setId(1L);
        v.setFecha(LocalDate.of(2026, 1, 15));
        v.setTotalNeto(BigDecimal.valueOf(1000));
        v.setTotalIva(BigDecimal.valueOf(190));
        v.setTotalIla(BigDecimal.ZERO);
        v.setTotalDescuento(BigDecimal.ZERO);
        v.setTotal(BigDecimal.valueOf(1190));
        return v;
    }

    @Test
    void toVentaDTO_sinClienteNiVendedor_camposNulos() {
        VentaDTO dto = VentaMapper.toVentaDTO(ventaMinima());
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getRutCliente()).isNull();
        assertThat(dto.getCodigoVendedor()).isNull();
    }

    @Test
    void toVentaDTO_conCliente_mapea_rutYCodigo() {
        Venta v = ventaMinima();
        Cliente c = new Cliente();
        c.setId(new ClienteId("12345678-9", "001"));
        c.setRazon("Empresa Test");
        v.setCliente(c);

        VentaDTO dto = VentaMapper.toVentaDTO(v);
        assertThat(dto.getRutCliente()).isEqualTo("12345678-9");
        assertThat(dto.getCodigoCliente()).isEqualTo("001");
        assertThat(dto.getNombreCliente()).isEqualTo("Empresa Test");
    }

    @Test
    void toVentaDTO_conVendedor_mapea_codigoYTipo() {
        Venta v = ventaMinima();
        Vendedor vend = new Vendedor();
        vend.setId(new VendedorId("V01", "0 "));
        vend.setNombre("Juan Pérez");
        v.setVendedor(vend);

        VentaDTO dto = VentaMapper.toVentaDTO(v);
        assertThat(dto.getCodigoVendedor()).isEqualTo("V01");
        assertThat(dto.getNombreVendedor()).isEqualTo("Juan Pérez");
    }

    @Test
    void toVentaDTO_detallesInicializados_aparecenEnDTO() {
        Venta v = ventaMinima();
        CondicionVenta cond = new CondicionVenta();
        cond.setDescripcion("Contado");
        v.setCondicionVenta(cond);
        v.setEstado(EstadoVenta.OPENED);

        VentaDetalle det = new VentaDetalle();
        det.setId(10L);
        det.setCantidad(BigDecimal.ONE);
        det.setPrecioUnitario(BigDecimal.valueOf(1000));
        det.setPorcentajeDescuento(BigDecimal.ZERO);
        det.setPorcentajeIva(BigDecimal.valueOf(19));
        det.setPorcentajeIla(BigDecimal.ZERO);
        v.addDetalle(det); // sets det.venta = v

        VentaDTO dto = VentaMapper.toVentaDTO(v);
        assertThat(dto.getDetalles()).isNotNull().hasSize(1);
    }

    @Test
    void toVentaDetalleDTO_sinProducto_idProductoNulo() {
        Venta v = ventaMinima();
        VentaDetalle det = new VentaDetalle();
        det.setId(5L);
        det.setCantidad(BigDecimal.valueOf(2));
        det.setPrecioUnitario(BigDecimal.valueOf(500));
        det.setPorcentajeDescuento(BigDecimal.ZERO);
        det.setPorcentajeIva(BigDecimal.valueOf(19));
        det.setPorcentajeIla(BigDecimal.ZERO);
        v.addDetalle(det); // sets det.venta = v

        VentaDetalleDTO dto = VentaMapper.toVentaDetalleDTO(det);
        assertThat(dto.getIdProducto()).isNull();
    }
}
