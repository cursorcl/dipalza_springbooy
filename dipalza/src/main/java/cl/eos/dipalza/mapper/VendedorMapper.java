package cl.eos.dipalza.mapper;

import cl.eos.dipalza.entity.Vendedor;
import cl.eos.dipalza.entity.ids.VendedorId;
import cl.eos.dipalza.model.VendedorDTO;

public class VendedorMapper {
	public static VendedorDTO toDto(Vendedor v) {
		return new VendedorDTO(v.getId().getCodigo(), v.getId().getTipo(), v.getRut(), v.getNombre(), v.getCiudad(),
				v.getComuna(), v.getDireccion(), v.getTelefono());
	}

	public static Vendedor toEntity(VendedorDTO dto) {

		if (dto == null)
			return null;

		VendedorId id = new VendedorId();
		id.setCodigo(dto.codigo());
		id.setTipo(dto.tipo());

		Vendedor v = new Vendedor();
		v.setId(id);
		v.setCiudad(dto.ciudad());
		v.setComuna(dto.comuna());
		v.setDireccion(dto.direccion());
		v.setNombre(dto.nombre());
		v.setTelefono(dto.telefono());
		v.setRut(dto.rut());

		return v;
	}
}
