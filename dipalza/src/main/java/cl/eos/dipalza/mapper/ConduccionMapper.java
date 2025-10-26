package cl.eos.dipalza.mapper;

import org.springframework.stereotype.Component;

import cl.eos.dipalza.entity.Conduccion;
import cl.eos.dipalza.model.ConduccionDTO;

@Component
public class ConduccionMapper {

    // Convierte de Entidad -> DTO
    public ConduccionDTO toDTO(Conduccion conduccion) {
    	
        if (conduccion == null) return null;
        ConduccionDTO dto = new ConduccionDTO();
        dto.setCodigo(conduccion.getCodigo());
        dto.setDescripcion(conduccion.getDescripcion());
        dto.setValor(conduccion.getValor());
        return dto;
    }

    // Convierte de DTO -> Entidad
    public Conduccion toEntity(ConduccionDTO dto) {
        if (dto == null) return null;

        Conduccion conduccion = new Conduccion();
        conduccion.setCodigo(dto.getCodigo());
        conduccion.setDescripcion(dto.getDescripcion());
        conduccion.setValor(dto.getValor());
        return conduccion;
    }
}
