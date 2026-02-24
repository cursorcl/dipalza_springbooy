package cl.eos.dipalza.mapper;

import cl.eos.dipalza.entity.HistorialPosicion;
import cl.eos.dipalza.entity.Posicion;
import cl.eos.dipalza.model.HistorialPosicionDTO;
import cl.eos.dipalza.model.PosicionDTO;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

/**
 * Clase utilitaria que permite realizar la conversión en ambos sentidos de:
 * - Posicion <--> PosicionDTO
 * - HistorialPosicion <--> HistorialPosicionDTO
 *
 * Estas clases se usan para registrar la posición reportada por la aplicación movil.
 */
public class PosicionMapper {

    private static final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public static HistorialPosicionDTO toHistorialDTO(HistorialPosicion posicion) {
        return new HistorialPosicionDTO(
                posicion.getId(), posicion.getVendedorId(), posicion.getFechaHora(), posicion.getPosicion().getY(), posicion.getPosicion().getX());
    }
    public static HistorialPosicion toHistorial(HistorialPosicionDTO dto) {
        var historialPosicion = new HistorialPosicion();
        historialPosicion.setVendedorId(dto.vendedorId());
        historialPosicion.setFechaHora(dto.fechaHora());
        Point point = geometryFactory.createPoint(new Coordinate(dto.longitude(), dto.latitude()));
        historialPosicion.setPosicion(point);
        return historialPosicion;
    }
    public static PosicionDTO toPosicionDTO(Posicion posicion) {
        return new PosicionDTO(
                posicion.getVendedorId(), posicion.getFechaHora(), posicion.getPosicion().getY(), posicion.getPosicion().getX());
    }
    public static Posicion toPosicion(PosicionDTO dto) {
        var posicion = new Posicion();
        posicion.setVendedorId(dto.vendedorId());
        posicion.setFechaHora(dto.fechaHora());
        Point point = geometryFactory.createPoint(new Coordinate(dto.longitude(), dto.latitude()));
        posicion.setPosicion(point);
        return posicion;
    }
}
