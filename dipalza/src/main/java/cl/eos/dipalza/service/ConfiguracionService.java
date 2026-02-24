package cl.eos.dipalza.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cl.eos.dipalza.entity.Configuracion;
import cl.eos.dipalza.repository.ConfiguracionRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;

@Service
public class ConfiguracionService {

    @Autowired
    private ConfiguracionRepository repo;

    private Map<String, String> cache = new ConcurrentHashMap<>();

    @PostConstruct
    public void cargarCache() {
        repo.findAll().forEach(c -> cache.put(c.getPropiedad(), c.getValor()));
    }

    public void recargarCache() {
        cache.clear();
        cargarCache();
    }

    public String getString(String clave) {
        return cache.getOrDefault(clave, "");
    }

    public Integer getInt(String clave) {
        String val = cache.get(clave);
        if (val == null) return 0;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    public Double getDouble(String clave) {
        String val = cache.get(clave);
        if (val == null) return 0D;
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return 0D;
        }
    }

    public Boolean getBoolean(String clave) {
        String val = cache.get(clave);
        return Boolean.parseBoolean(val);
    }
    
    @Transactional
    public void actualizarConfig(String clave, String nuevoValor) {
        Configuracion config = repo.findById(clave)
            .orElseThrow(() -> new RuntimeException("Config no existe"));
        
        config.setValor(nuevoValor);
        repo.save(config);
        cache.put(clave, nuevoValor);
    }
}