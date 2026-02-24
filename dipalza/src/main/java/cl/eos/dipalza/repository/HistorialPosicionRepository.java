package cl.eos.dipalza.repository;

import cl.eos.dipalza.entity.HistorialPosicion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface HistorialPosicionRepository extends JpaRepository<HistorialPosicion, String> {

    /// Query para obtener el historial paginado de un movil específico
    Page<HistorialPosicion> findByVendedorIdOrderByFechaHoraDesc(String vendedorId, Pageable pageable);


    /// Query para obtener el historial paginado de todos los moviles
    Page<HistorialPosicion> findByOrderByFechaHoraDesc( Pageable pageable);

    /// Query paginada filtrando por un rango de fechas para un vendedorId (útil para el histórico de 2-3 meses)
    Page<HistorialPosicion> findByVendedorIdAndFechaHoraBetween(
            String vendedorId,
            LocalDateTime inicio,
            LocalDateTime fin,
            Pageable pageable
    );

    /// Query paginada filtrando por un rango de fechas (útil para el histórico de 2-3 meses)
    Page<HistorialPosicion> findByFechaHoraBetween(
            LocalDateTime inicio,
            LocalDateTime fin,
            Pageable pageable
    );
}
