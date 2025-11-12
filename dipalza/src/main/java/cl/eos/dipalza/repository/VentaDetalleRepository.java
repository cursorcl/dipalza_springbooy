// file: cl/eos/dipalza/repository/VentaRepository.java
package cl.eos.dipalza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cl.eos.dipalza.entity.Venta; // <-- ajuste este import si su entity está en otro paquete
import cl.eos.dipalza.entity.VentaDetalle;

@Repository
public interface VentaDetalleRepository extends JpaRepository<VentaDetalle, Long>, JpaSpecificationExecutor<Venta> {

	
    @Query("SELECT vd FROM VentaDetalle vd LEFT JOIN FETCH vd.piezasUsadas WHERE vd.venta.id = :ventaId")
    List<VentaDetalle> findDetallesWithPiezas(@Param("ventaId") Long ventaId);
    
    @EntityGraph(attributePaths = {"venta"})
    @Query("SELECT d FROM VentaDetalle d WHERE d.venta.id = :ventaId")
    List<VentaDetalle> findByVentaId(@Param("ventaId") Long ventaId);
}
