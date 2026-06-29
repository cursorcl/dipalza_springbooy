package cl.eos.dipalza.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ProductoDTO {

	private String articulo;

	private String descripcion;

	private BigDecimal ventaNeto;

	private BigDecimal precioLista2;

	private BigDecimal porcIla;

	private BigDecimal porcCarne;

	private String unidad;

	private BigDecimal stock;

	private Boolean numbered;

	private String codigoila;

	private LocalDate lastUpdate;
	
	private BigDecimal pieces;
	
	private BigDecimal stockVentas;
	
	private BigDecimal piezasVentas;

	private BigDecimal costo;
	
	private List<NumeradoDTO> numerados = new ArrayList<>();

	public ProductoDTO() {}

	public ProductoDTO(String articulo, String descripcion, BigDecimal ventaNeto, BigDecimal precioLista2, BigDecimal porcIla, BigDecimal porcCarne, String unidad, BigDecimal stock, Boolean numbered, String codigoila, LocalDate lastUpdate, BigDecimal pieces, BigDecimal stockVentas, BigDecimal piezasVentas, BigDecimal costo) {
		this.articulo = articulo;
		this.descripcion = descripcion;
		this.ventaNeto = ventaNeto;
		this.precioLista2 = precioLista2;
		this.porcIla = porcIla;
		this.porcCarne = porcCarne;
		this.unidad = unidad;
		this.stock = stock;
		this.numbered = numbered;
		this.codigoila = codigoila;
		this.lastUpdate = lastUpdate;
		this.pieces = pieces;
		this.stockVentas = stockVentas;
		this.piezasVentas = piezasVentas;
		this.costo = costo;
	}

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

	public List<NumeradoDTO> getNumerados() {
		return numerados;
	}

	public void setNumerados(List<NumeradoDTO> numerados) {
		this.numerados = numerados;
	}

	public BigDecimal getPieces() {
		return pieces;
	}

	public void setPieces(BigDecimal pieces) {
		this.pieces = pieces;
	}

	public BigDecimal getStockVentas() {
		return stockVentas;
	}

	public void setStockVentas(BigDecimal stockVentas) {
		this.stockVentas = stockVentas;
	}

	public BigDecimal getPiezasVentas() {
		return piezasVentas;
	}

	public void setPiezasVentas(BigDecimal piezasVentas) {
		this.piezasVentas = piezasVentas;
	}

	public BigDecimal getCosto() {
		return costo;
	}

	public void setCosto(BigDecimal costo) {
		this.costo = costo;
	}

	public BigDecimal getPrecioLista2() {
		return precioLista2;
	}

	public void setPrecioLista2(BigDecimal precioLista2) {
		this.precioLista2 = precioLista2;
	}
}
