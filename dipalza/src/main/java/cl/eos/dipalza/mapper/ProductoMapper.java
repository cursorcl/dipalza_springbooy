package cl.eos.dipalza.mapper;

import org.springframework.stereotype.Component;

import cl.eos.dipalza.entity.Producto;
import cl.eos.dipalza.model.ProductoDTO;

@Component
public class ProductoMapper {

    // Convierte de Entidad -> DTO
    public ProductoDTO toDTO(Producto producto) {
        if (producto == null) return null;

        ProductoDTO dto = new ProductoDTO();
	    dto.setArticulo(producto.getArticulo());
	    dto.setDescripcion(producto.getDescripcion());
	    dto.setVentaNeto(producto.getVentaNeto());
	    dto.setPorcIla(producto.getPorcIla());
	    dto.setPorcCarne(producto.getPorcCarne());
	    dto.setUnidad(producto.getUnidad());
	    dto.setStock(producto.getStock());
	    dto.setPieces(producto.getPieces());
	    dto.setNumbered(producto.getNumbered());
	    dto.setCodigoila(producto.getCodigoila());
	    dto.setLastUpdate(producto.getLastUpdate());

        return dto;
    }

    // Convierte de DTO -> Entidad
    public Producto toEntity(ProductoDTO dto) {
        if (dto == null) return null;


        Producto producto = new Producto();
        producto.setArticulo(dto.getArticulo());
	    producto.setDescripcion(dto.getDescripcion());
	    producto.setVentaNeto(dto.getVentaNeto());
	    producto.setPorcIla(dto.getPorcIla());
	    producto.setPorcCarne(dto.getPorcCarne());
	    producto.setUnidad(dto.getUnidad());
	    producto.setStock(dto.getStock());
	    producto.setPieces(dto.getPieces());
	    producto.setNumbered(dto.getNumbered());
	    producto.setCodigoila(dto.getCodigoila());
	    producto.setLastUpdate(dto.getLastUpdate());
        
        return producto;
    }
}
