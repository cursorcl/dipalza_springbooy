package cl.eos.dipalza.service;


import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cl.eos.dipalza.entity.CondicionVenta;
import cl.eos.dipalza.mapper.CondicionVentaMapper;
import cl.eos.dipalza.model.CondicionVentaDTO;
import cl.eos.dipalza.repository.CondicionVentaRepository;

@Service
public class CondicionVentaService {

    @Autowired
    private CondicionVentaRepository condicionVentaRepository;
    
    @Autowired
    private CondicionVentaMapper condicionVentaMapper;

    public List<CondicionVentaDTO> getAllCondicionVentas() {
        return condicionVentaRepository.findAll()
                .stream()
                .map(condicionVentaMapper::toDTO)
                .collect(Collectors.toList());
    }
    

    public Optional<CondicionVentaDTO> getCondicionVentaById(String codigo) {
        return condicionVentaRepository.findById(codigo)
                .map(condicionVentaMapper::toDTO);
    }

    public CondicionVentaDTO createOrUpdateCondicionVenta(CondicionVentaDTO condicionVentaDTO) {
        CondicionVenta condicionVenta = condicionVentaMapper.toEntity(condicionVentaDTO);
        CondicionVenta savedCondicionVenta = condicionVentaRepository.save(condicionVenta);
        return condicionVentaMapper.toDTO(savedCondicionVenta);
    }

    public boolean deleteCondicionVenta(String codigo) {
        if (condicionVentaRepository.existsById(codigo)) {
            condicionVentaRepository.deleteById(codigo);
            return true;
        }
        return false;
    }
}