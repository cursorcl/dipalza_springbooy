package cl.eos.dipalza.mapper;

import org.springframework.stereotype.Component;

import cl.eos.dipalza.entity.Ila;
import cl.eos.dipalza.model.IlaDTO;

@Component
public class IlaMapper {

    // Convierte de Entidad -> DTO
    public IlaDTO toDTO(Ila conduccion) {
    	
        if (conduccion == null) return null;
        IlaDTO dto = new IlaDTO();
        dto.setCodigo(conduccion.getCodigo());
        dto.setDescripcion(conduccion.getDescripcion());
        dto.setValor(conduccion.getValor());
        return dto;
    }

    // Convierte de DTO -> Entidad
    public Ila toEntity(IlaDTO dto) {
        if (dto == null) return null;

        Ila entity = new Ila();
        entity.setCodigo(dto.getCodigo());
        entity.setDescripcion(dto.getDescripcion());
        entity.setValor(dto.getValor());
        return entity;
    }
}
