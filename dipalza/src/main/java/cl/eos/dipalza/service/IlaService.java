package cl.eos.dipalza.service;


import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cl.eos.dipalza.entity.Ila;
import cl.eos.dipalza.mapper.IlaMapper;
import cl.eos.dipalza.model.IlaDTO;
import cl.eos.dipalza.repository.IlaRepository;

@Service
public class IlaService {

    @Autowired
    private IlaRepository ilaRepository;
    
    @Autowired
    private IlaMapper ilaMapper;

    public List<IlaDTO> getAllIlas() {
        return ilaRepository.findAll()
                .stream()
                .map(ilaMapper::toDTO)
                .collect(Collectors.toList());
    }
    
    
    public List<IlaDTO> findAllByOrderByDescripcionAsc() {
    	return ilaRepository.findAllByOrderByDescripcionAsc()
                .stream()
                .map(ilaMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<IlaDTO> getIlaById(String codigo) {
        return ilaRepository.findById(codigo)
                .map(ilaMapper::toDTO);
    }

    public IlaDTO createOrUpdateIla(IlaDTO ilaDTO) {
        Ila ila = ilaMapper.toEntity(ilaDTO);
        Ila savedIla = ilaRepository.save(ila);
        return ilaMapper.toDTO(savedIla);
    }

    public boolean deleteIla(String codigo) {
        if (ilaRepository.existsById(codigo)) {
            ilaRepository.deleteById(codigo);
            return true;
        }
        return false;
    }
}