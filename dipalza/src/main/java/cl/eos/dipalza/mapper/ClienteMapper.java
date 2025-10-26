package cl.eos.dipalza.mapper;

import org.springframework.stereotype.Component;

import cl.eos.dipalza.entity.Cliente;
import cl.eos.dipalza.entity.ids.ClienteId;
import cl.eos.dipalza.model.ClienteDTO;

@Component
public class ClienteMapper {

    // Convierte de Entidad -> DTO
    public ClienteDTO toDTO(Cliente cliente) {
        if (cliente == null) return null;

        ClienteDTO dto = new ClienteDTO();
        dto.setRut(cliente.getId().getRut());
        dto.setCodigo(cliente.getId().getCodigo());
        dto.setRazon(cliente.getRazon());
        dto.setDireccion(cliente.getDireccion());
        dto.setCiudad(cliente.getCiudad());
        dto.setGiro(cliente.getGiro());
        dto.setTelefono(cliente.getTelefono());
        dto.setCodigoRuta(cliente.getCodigoRuta());
        dto.setCodigoVendedor(cliente.getCodigoVendedor());

        return dto;
    }

    // Convierte de DTO -> Entidad
    public Cliente toEntity(ClienteDTO dto) {
        if (dto == null) return null;

        ClienteId clienteId = new ClienteId(
            dto.getRut(),
            dto.getCodigo()
        );

        Cliente cliente = new Cliente();
        cliente.setId(clienteId);
        cliente.setRazon(dto.getRazon());
        cliente.setDireccion(dto.getDireccion());
        cliente.setCiudad(dto.getCiudad());
        cliente.setGiro(dto.getGiro());
        cliente.setTelefono(dto.getTelefono());
        cliente.setCodigoRuta(dto.getCodigoRuta());
        cliente.setCodigoVendedor(dto.getCodigoVendedor());
        
        return cliente;
    }
}
