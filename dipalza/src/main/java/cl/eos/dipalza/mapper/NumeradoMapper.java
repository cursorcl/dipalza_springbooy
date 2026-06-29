package cl.eos.dipalza.mapper;

import cl.eos.dipalza.entity.Numerado;
import cl.eos.dipalza.entity.Producto;
import cl.eos.dipalza.model.NumeradoDTO;
import org.springframework.stereotype.Component;

@Component
public class NumeradoMapper {

    public NumeradoDTO toDTO(Numerado numerado) {
        NumeradoDTO numeradoDTO = new NumeradoDTO();
        numeradoDTO.setId(numerado.getId());
        numeradoDTO.setCodigoProducto(numerado.getProducto().getArticulo());
        numeradoDTO.setNombreProducto(numerado.getProducto().getDescripcion());
        numeradoDTO.setNumero(numerado.getNumero());
        numeradoDTO.setEstado(numerado.getEstado());
        numeradoDTO.setPeso(numerado.getPeso());
        return numeradoDTO;
    }

    public Numerado toEntity(NumeradoDTO numeradoDTO, Producto producto) {
        Numerado numerado = new Numerado();
        numerado.setId(numeradoDTO.getId());
        numerado.setEstado(numeradoDTO.getEstado());
        numerado.setProducto(producto);
        numerado.setPeso(numeradoDTO.getPeso());
        numerado.setNumero(numeradoDTO.getNumero());
        return numerado;
    }

    
}
