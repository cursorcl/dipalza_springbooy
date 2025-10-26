package cl.eos.dipalza.service;


import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cl.eos.dipalza.entity.Cliente;
import cl.eos.dipalza.entity.ids.ClienteId;
import cl.eos.dipalza.mapper.ClienteMapper;
import cl.eos.dipalza.model.ClienteDTO;
import cl.eos.dipalza.repository.ClienteRepository;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;
    
    @Autowired
    private ClienteMapper clienteMapper;

    public List<ClienteDTO> getAllClientes() {
        return clienteRepository.findAll()
                .stream()
                .map(clienteMapper::toDTO)
                .collect(Collectors.toList());
    }
    
    public List<ClienteDTO> getClientesByRuta(String ruta) {
        return clienteRepository.getClienteByCodigoRuta(ruta)
                .stream()
                .map(clienteMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<ClienteDTO> getClienteById(String rut, String codigo) {
        return clienteRepository.findById(new ClienteId(rut, codigo))
                .map(clienteMapper::toDTO);
    }

    public ClienteDTO createOrUpdateCliente(ClienteDTO clienteDTO) {
        Cliente cliente = clienteMapper.toEntity(clienteDTO);
        Cliente savedCliente = clienteRepository.save(cliente);
        return clienteMapper.toDTO(savedCliente);
    }

    public boolean deleteCliente(String rut, String codigo) {
        ClienteId id = new ClienteId(rut, codigo);
        if (clienteRepository.existsById(id)) {
            clienteRepository.deleteById(id);
            return true;
        }
        return false;
    }
}