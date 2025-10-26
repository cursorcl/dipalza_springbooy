package cl.eos.dipalza.service;


import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cl.eos.dipalza.entity.Ruta;
import cl.eos.dipalza.mapper.RutaMapper;
import cl.eos.dipalza.model.RutaDTO;
import cl.eos.dipalza.repository.RutaRepository;

@Service
public class RutaService {

    @Autowired
    private RutaRepository rutaRepository;
    
    @Autowired
    private RutaMapper rutaMapper;

    public List<RutaDTO> getAllRutas() {
        return rutaRepository.findAll()
                .stream()
                .map(rutaMapper::toDTO)
                .collect(Collectors.toList());
    }
    

    public Optional<RutaDTO> getRutaById(String codigo) {
        return rutaRepository.findById(codigo)
                .map(rutaMapper::toDTO);
    }

    public RutaDTO createOrUpdateRuta(RutaDTO rutaDTO) {
        Ruta ruta = rutaMapper.toEntity(rutaDTO);
        Ruta savedRuta = rutaRepository.save(ruta);
        return rutaMapper.toDTO(savedRuta);
    }

    public boolean deleteRuta(String codigo) {
        if (rutaRepository.existsById(codigo)) {
            rutaRepository.deleteById(codigo);
            return true;
        }
        return false;
    }
}