package cl.eos.dipalza.mapper;


import org.springframework.stereotype.Component;

import cl.eos.dipalza.entity.CondicionVenta;
import cl.eos.dipalza.model.CondicionVentaDTO;

@Component
public class CondicionVentaMapper {

    // Convierte de Entidad -> DTO
    public CondicionVentaDTO toDTO(CondicionVenta condicionVenta) {
        if (condicionVenta == null) return null;


        CondicionVentaDTO dto = new CondicionVentaDTO();
        dto.setCodigo(condicionVenta.getCodigo());
        dto.setDescripcion(condicionVenta.getDescripcion());
        dto.setDias(condicionVenta.getDias());
        return dto;
    }

    // Convierte de DTO -> Entidad
    public CondicionVenta toEntity(CondicionVentaDTO dto) {
        if (dto == null) return null;

        CondicionVenta condicionVenta = new CondicionVenta();
        condicionVenta.setCodigo(dto.getCodigo());
        condicionVenta.setDescripcion(dto.getDescripcion());
        condicionVenta.setDias(dto.getDias());
        return condicionVenta;
    }
}
