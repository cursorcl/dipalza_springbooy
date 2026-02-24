package cl.eos.dipalza.service;

import cl.eos.dipalza.entity.HistorialPosicion;
import cl.eos.dipalza.entity.Posicion;
import cl.eos.dipalza.model.PosicionDTO;
import cl.eos.dipalza.repository.HistorialPosicionRepository;
import cl.eos.dipalza.repository.PosicionRepository;
import jakarta.transaction.Transactional;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PosicionService {

    private final PosicionRepository posicionRepository;
    private final HistorialPosicionRepository historialRepo;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public PosicionService(PosicionRepository posicionRepository, HistorialPosicionRepository historialRepo) {
        this.posicionRepository = posicionRepository;
        this.historialRepo = historialRepo;
    }

    ///  obtiene las últimas posiciones de cada movil
    public List<Posicion> obtenerPosiciones() {
        return posicionRepository.findAll();
    }

    ///  obtiene las últimas posiciones de cada movil
    public Posicion obtenerPosicionPorVendedor(String vendedorId) {
        return  posicionRepository.findById(vendedorId).orElse(null);
    }

    /// obtiene la historia en forma paginada para un vendedor
    public Page<HistorialPosicion> obtenerHistorialPosicionesVendedor(String vendedorId, int pagina, int tamano) {
        Pageable pageable = PageRequest.of(pagina, tamano);
        return historialRepo.findByVendedorIdOrderByFechaHoraDesc(vendedorId, pageable);
    }

    ///  obtiene la historia en forma paginadda
    public Page<HistorialPosicion> obtenerHistorialPosiciones( int pagina, int tamano) {
        Pageable pageable = PageRequest.of(pagina, tamano);
        return historialRepo.findByOrderByFechaHoraDesc(pageable);
    }

    /// Obtiene historial de posiciones en un rango de fecha para un vendedor
    public Page<HistorialPosicion> obtenerHistorialPosicionesVendedorEntre(
            String vendedorId,
            LocalDateTime inicio,
            LocalDateTime fin,
            int pagina,
            int tamano
    )
    {
        Pageable pageable = PageRequest.of(pagina, tamano);
        return historialRepo.findByVendedorIdAndFechaHoraBetween(vendedorId, inicio, fin, pageable);
    }

    /// Obtiene historial de posiciones en un rango de fecha de todos los vendedores
    public Page<HistorialPosicion> obtenerHistorialPosicionesEntre(
            LocalDateTime inicio,
            LocalDateTime fin,
            int pagina,
            int tamano
    ) {
        Pageable pageable = PageRequest.of(pagina, tamano);
        return historialRepo.findByFechaHoraBetween(inicio, fin, pageable);
    }

    ///  Almacena el registro de posición asociado al movil
    @Transactional
    public void registrarUbicacion(PosicionDTO dto) {

        var lon = dto.longitude();
        var lat = dto.latitude();
        var id = dto.vendedorId();
        var fecha = dto.fechaHora();

        Point punto = geometryFactory.createPoint(new Coordinate(lon, lat));
        LocalDateTime ahora = LocalDateTime.now();

        // 1. Actualizar o crear el estado actual
        Posicion posicion = posicionRepository.findById(id).orElse(new Posicion());
        posicion.setVendedorId(id);
        posicion.setPosicion(punto);
        posicion.setFechaHora(ahora);
        posicionRepository.save(posicion);

        // 2. Insertar en el historial
        HistorialPosicion historial = new HistorialPosicion();
        historial.setVendedorId(id);
        historial.setPosicion(punto);
        historial.setFechaHora(ahora);
        historialRepo.save(historial);
    }
}
