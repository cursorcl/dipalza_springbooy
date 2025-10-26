// file: cl/eos/dipalza/repository/VentaRepository.java
package cl.eos.dipalza.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cl.eos.dipalza.entity.Venta; // <-- ajuste este import si su entity está en otro paquete

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {


    /**
     * Ejemplo de consulta directa por rango de fechas (opcional).
     */
    @Query("""
           select v
           from Venta v
           where v.fecha >= :desde and v.fecha < :hasta
           """)
    Page<Venta> findByFechaBetween(@Param("desde") java.time.LocalDate desde,
                                   @Param("hasta") java.time.LocalDate hasta,
                                   Pageable pageable);
    
    @Query("SELECT v FROM Venta v WHERE v.vendedor.id.codigo = :vendedorCodigo AND CAST(v.fecha AS date) = :fecha")
    List<Venta> findVentasByVendedorAndFecha(@Param("vendedorCodigo") String vendedorCodigo, @Param("fecha") LocalDate fecha);
    
    @Query("SELECT v FROM Venta v WHERE v.cliente.id.rut = :rutCliente ORDER BY v.fecha DESC")
    Venta findLastVentaByCliente(@Param("rutCliente") String rutCliente);

    @Query("SELECT v FROM Venta v JOIN FETCH v.detalles vd WHERE v.id = :ventaId")
    Venta findVentaWithDetalles(@Param("ventaId") Long ventaId);

 // ventas de un día por vendedor (rango [inicio, fin))
    @EntityGraph(value = "Venta.header", type = EntityGraph.EntityGraphType.LOAD)
    @Query("""
       select v
         from Venta v
        where v.vendedor.id.codigo = :codigoVendedor
          and  CAST(v.fecha AS date) = :fecha
        order by v.fecha desc
    """)
    List<Venta> findHeaderByVendedorAndDia(
        @Param("codigoVendedor") String codigoVendedor,
        @Param("fecha") LocalDate fecha);
    
    

}
