package cl.eos.dipalza.mapper;


import org.springframework.stereotype.Component;

import cl.eos.dipalza.entity.Ruta;
import cl.eos.dipalza.model.RutaDTO;

@Component
public class RutaMapper {

    // Convierte de Entidad -> DTO
    public RutaDTO toDTO(Ruta Ruta) {
        if (Ruta == null) return null;


        RutaDTO dto = new RutaDTO();
        dto.setCodigo(Ruta.getCodigo());
        dto.setDescripcion(Ruta.getDescripcion());
        return dto;
    }

    // Convierte de DTO -> Entidad
    public Ruta toEntity(RutaDTO dto) {
        if (dto == null) return null;

        Ruta ruta = new Ruta();
        ruta.setCodigo(dto.getCodigo());
        ruta.setDescripcion(dto.getDescripcion());
        return ruta;
    }
}
