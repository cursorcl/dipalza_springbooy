package cl.eos.dipalza.mapper;


import org.springframework.stereotype.Component;

import cl.eos.dipalza.entity.Conduccion;
import cl.eos.dipalza.entity.Ruta;
import cl.eos.dipalza.model.RutaDTO;

@Component
public class RutaMapper {

    // Convierte de Entidad -> DTO
    public RutaDTO toDTO(Ruta ruta) {
        if (ruta == null) return null;


        RutaDTO dto = new RutaDTO();
        dto.setCodigo(ruta.getCodigo());
        dto.setDescripcion(ruta.getDescripcion());
        dto.setCodigoConduccion(ruta.getConduccion().getCodigo());
        dto.setNombreConduccion(ruta.getConduccion().getDescripcion());
        return dto;
    }

    // Convierte de DTO -> Entidad
    public Ruta toEntity(RutaDTO dto, Conduccion conduccion) {
        if (dto == null) return null;

        Ruta ruta = new Ruta();
        ruta.setCodigo(dto.getCodigo());
        ruta.setDescripcion(dto.getDescripcion());
        ruta.setConduccion(conduccion);
        return ruta;
    }
}
