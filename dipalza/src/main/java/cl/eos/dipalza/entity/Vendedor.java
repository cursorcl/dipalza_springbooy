package cl.eos.dipalza.entity;

import cl.eos.dipalza.entity.ids.VendedorId;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "vendedor", schema = "dbo")
public class Vendedor {

	@EmbeddedId
	@AttributeOverrides({
			@AttributeOverride(name = "codigo", column = @Column(name = "codigo", length = 3, nullable = false)),
			@AttributeOverride(name = "tipo", column = @Column(name = "tipo", length = 1, nullable = false)) })
	private VendedorId id;

	@Column(name = "rut", length = 10)
	private String rut;

	@Column(name = "nombre", length = 40)
	private String nombre;

	@Column(name = "direccion", length = 40)
	private String direccion;

	@Column(name = "comuna", length = 30)
	private String comuna;

	@Column(name = "ciudad", length = 30)
	private String ciudad;

	@Column(name = "telefono", length = 40)
	private String telefono;


	public Vendedor() {
	}

	public Vendedor(VendedorId id) {
		this.id = id;
	}

	// getters/setters
	public VendedorId getId() {
		return id;
	}

	public void setId(VendedorId id) {
		this.id = id;
	}

	public String getRut() {
		return rut;
	}

	public void setRut(String rut) {
		this.rut = rut;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getDireccion() {
		return direccion;
	}

	public void setDireccion(String direccion) {
		this.direccion = direccion;
	}

	public String getComuna() {
		return comuna;
	}

	public void setComuna(String comuna) {
		this.comuna = comuna;
	}

	public String getCiudad() {
		return ciudad;
	}

	public void setCiudad(String ciudad) {
		this.ciudad = ciudad;
	}

	public String getTelefono() {
		return telefono;
	}

	public void setTelefono(String telefono) {
		this.telefono = telefono;
	}

}