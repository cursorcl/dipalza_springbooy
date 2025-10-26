// file: cl/eos/dipalza/repository/VentaRepository.java
package cl.eos.dipalza.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import cl.eos.dipalza.entity.Venta; // <-- ajuste este import si su entity está en otro paquete
import cl.eos.dipalza.entity.VentaDetallePieza;

@Repository
public interface VentaDetallePiezaRepository extends JpaRepository<VentaDetallePieza,  Long>, JpaSpecificationExecutor<Venta> {

}
