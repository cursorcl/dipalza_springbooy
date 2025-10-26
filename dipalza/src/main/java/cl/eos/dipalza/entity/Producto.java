package cl.eos.dipalza.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "producto", schema = "dbo")
public class Producto {

	@Id
	@Column(name = "Articulo", length = 255, nullable = false)
	private String articulo;

	@Column(name = "Descripcion", length = 200, nullable = false)
	private String descripcion;

	@Column(name = "VentaNeto", columnDefinition = "money", nullable = false)
	private BigDecimal ventaNeto;

	@Column(name = "PorcIla", precision = 5, scale = 2) // ajusta scale si corresponde
	private BigDecimal porcIla;

	@Column(name = "PorcCarne", precision = 5, scale = 2)
	private BigDecimal porcCarne;

	@Column(name = "Unidad", length = 10, nullable = false)
	private String unidad;

	@Column(name = "Stock", columnDefinition = "money")
	private BigDecimal stock;

	@Column(name = "Pieces")
	private Integer pieces;

	@Column(name = "Numbered")
	private Boolean numbered;

	// En la tabla se llama "Codigolla" (con doble L)
	@Column(name = "CodigoIla", length = 20)
	private String codigoila;

	@Column(name = "last_update", nullable = false)
	private LocalDate lastUpdate;

	// RowVersion (timestamp/rowversion). Usar para locking optimista.
	@Version
	@Column(name = "rv")
	private byte[] rv;

	public Producto() {
	}

	// Getters y setters
	public String getArticulo() {
		return articulo;
	}

	public void setArticulo(String articulo) {
		this.articulo = articulo;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}

	public BigDecimal getVentaNeto() {
		return ventaNeto;
	}

	public void setVentaNeto(BigDecimal ventaNeto) {
		this.ventaNeto = ventaNeto;
	}

	public BigDecimal getPorcIla() {
		return porcIla;
	}

	public void setPorcIla(BigDecimal porcIla) {
		this.porcIla = porcIla;
	}

	public BigDecimal getPorcCarne() {
		return porcCarne;
	}

	public void setPorcCarne(BigDecimal porcCarne) {
		this.porcCarne = porcCarne;
	}

	public String getUnidad() {
		return unidad;
	}

	public void setUnidad(String unidad) {
		this.unidad = unidad;
	}

	public BigDecimal getStock() {
		return stock;
	}

	public void setStock(BigDecimal stock) {
		this.stock = stock;
	}

	public Integer getPieces() {
		return pieces;
	}

	public void setPieces(Integer pieces) {
		this.pieces = pieces;
	}

	public Boolean getNumbered() {
		return numbered;
	}

	public void setNumbered(Boolean numbered) {
		this.numbered = numbered;
	}

	public String getCodigoila() {
		return codigoila;
	}

	public void setCodigoila(String codigoila) {
		this.codigoila = codigoila;
	}

	public LocalDate getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(LocalDate lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	public byte[] getRv() {
		return rv;
	}

	public void setRv(byte[] rv) {
		this.rv = rv;
	}
}
