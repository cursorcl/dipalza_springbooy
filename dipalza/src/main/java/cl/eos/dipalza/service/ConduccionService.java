package cl.eos.dipalza.service;


import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cl.eos.dipalza.entity.Conduccion;
import cl.eos.dipalza.mapper.ConduccionMapper;
import cl.eos.dipalza.model.ConduccionDTO;
import cl.eos.dipalza.repository.ConduccionRepository;

@Service
public class ConduccionService {

    @Autowired
    private ConduccionRepository conduccionRepository;
    
    @Autowired
    private ConduccionMapper conduccionMapper;

    public List<ConduccionDTO> getAllConduccions() {
        return conduccionRepository.findAll()
                .stream()
                .map(conduccionMapper::toDTO)
                .collect(Collectors.toList());
    }
    

    public Optional<ConduccionDTO> getConduccionById(String codigo) {
        return conduccionRepository.findById(codigo)
                .map(conduccionMapper::toDTO);
    }

    public ConduccionDTO createOrUpdateConduccion(ConduccionDTO conduccionDTO) {
        Conduccion conduccion = conduccionMapper.toEntity(conduccionDTO);
        Conduccion savedConduccion = conduccionRepository.save(conduccion);
        return conduccionMapper.toDTO(savedConduccion);
    }

    public boolean deleteConduccion(String codigo) {
        if (conduccionRepository.existsById(codigo)) {
            conduccionRepository.deleteById(codigo);
            return true;
        }
        return false;
    }
}