package cl.eos.dipalza.service;

import cl.eos.dipalza.entity.Numerado;
import cl.eos.dipalza.entity.Producto;
import cl.eos.dipalza.mapper.NumeradoMapper;
import cl.eos.dipalza.model.NumeradoDTO;
import cl.eos.dipalza.model.NumeradoResumenDTO;
import cl.eos.dipalza.repository.NumeradoRepository;
import cl.eos.dipalza.repository.ProductoRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class NumeradosService {

    private final NumeradoRepository numeradoRepository;
    private final ProductoRepository productoRepository;
    private final NumeradoMapper numeradoMapper;

    public NumeradosService(NumeradoRepository numeradoRepository, ProductoRepository productoRepository, NumeradoMapper numeradoMapper) {
        this.numeradoRepository = numeradoRepository;
        this.numeradoMapper = numeradoMapper;
        this.productoRepository = productoRepository;
    }

    public List<NumeradoDTO> findAll() {
        List<Numerado> numerados = numeradoRepository.findAll();
        if(numerados.isEmpty()) {
            return List.of();
        }

        return numerados.stream().map(numeradoMapper::toDTO).collect(Collectors.toList());
    }

    /**
     * Obtiene cuantos disponibles hay de cada numerado.
     * @return
     */
    public List<NumeradoResumenDTO> findGrouped() {
        List<NumeradoResumenDTO> numerados = numeradoRepository.findGroupedByEstado("D");
        return numerados;
    }
    public List<NumeradoDTO> findAllByEstado(@Param("estado") String estado) {
        List<Numerado> numerados = numeradoRepository.findByEstado(estado);
        if(numerados.isEmpty()) {
            return List.of();
        }
        return numerados.stream().map(numeradoMapper::toDTO).collect(Collectors.toList());
    }

    public NumeradoDTO findById(Long id) {

        Optional<Numerado> numerado = numeradoRepository.findById(id);
        if(numerado.isPresent()) {
            return numeradoMapper.toDTO(numerado.get());
        }
        return null;
    }

    public List<NumeradoDTO> findByProducto(String idProducto) {
        List<Numerado> numerados = numeradoRepository.findByProductoId(idProducto);
        if(numerados.isEmpty()) {
            return List.of();
        }
        return numerados.stream().map(numeradoMapper::toDTO).collect(Collectors.toList());
    }

    public NumeradoDTO save(NumeradoDTO n) {
        Producto producto = productoRepository.findByArticulo(n.getCodigoProducto());
        if(producto == null) {
            return null;
        }
        Numerado numerado = numeradoRepository.findById(n.getId()).orElse(null);
        if(numerado == null) {
            numerado = new Numerado();
        }
        numerado.setProducto(producto);
        numerado.setNumero(n.getNumero());
        numerado.setEstado(n.getEstado());
        numerado.setPeso(n.getPeso());
        numeradoRepository.save(numerado);
        return numeradoMapper.toDTO(numerado);
    }

    public void deleteById(Long id) {
        numeradoRepository.deleteById(id);
    }




    public Float findPrecioPromedioArticulo(String articulo) {
        List<Numerado> lista = numeradoRepository.findByProductoId(articulo);
        if(lista.isEmpty()) {
            return 0f;
        }
        BigDecimal promedio = lista.stream()
                .map(nn -> nn.getPeso())
                .reduce(BigDecimal.ZERO, BigDecimal::add) // Suma total empezando desde 0
                .divide(new BigDecimal(lista.size())); // División formal

        return promedio.floatValue();
    }

}
