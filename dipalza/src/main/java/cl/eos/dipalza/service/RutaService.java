package cl.eos.dipalza.service;


import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import cl.eos.dipalza.entity.Conduccion;
import cl.eos.dipalza.entity.Ruta;
import cl.eos.dipalza.mapper.RutaMapper;
import cl.eos.dipalza.model.RutaDTO;
import cl.eos.dipalza.repository.ConduccionRepository;
import cl.eos.dipalza.repository.RutaRepository;

@Service
public class RutaService {

    private final RutaRepository rutaRepository;
    private final RutaMapper rutaMapper;
    private final ConduccionRepository conduccionRepository;
    
    public RutaService(RutaRepository rutaRepository, RutaMapper rutaMapper, ConduccionRepository conduccionRepository) {
    	this.rutaMapper = rutaMapper;
    	this.rutaRepository = rutaRepository;
    	this.conduccionRepository = conduccionRepository;
	}

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
    	
    	List<Conduccion> conducciones = this.conduccionRepository.findAll();
    	Conduccion conduccion = conducciones.stream().filter(c -> c.getCodigo().equals(rutaDTO.getCodigoConduccion())).findFirst().orElse(conducciones.getFirst());
    	
        Ruta ruta = rutaMapper.toEntity(rutaDTO, conduccion);
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