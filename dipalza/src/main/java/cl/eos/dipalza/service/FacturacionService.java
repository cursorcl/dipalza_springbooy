package cl.eos.dipalza.service;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import cl.eos.dipalza.model.ResultadoFacturacionDTO;
import cl.eos.dipalza.model.venta.VentaDTO;
import cl.eos.dipalza.model.venta.VentaDetalleDTO;

@Service
public class FacturacionService {

	public static final String NUMERO_LINEAS_FACTURA = "NUMERO_LINEAS_FACTURA";
	public static final String FACTURA_ELECTRONICA = "FACTURA_ELECTRONICA";
	private final JdbcTemplate jdbcTemplate;
	private PlatformTransactionManager transactionManager;
	private final ConfiguracionService configuracion;

	private final boolean facturaElectronica;
	private final int nroLineasPorFactura;
	private final String tipoFacturaName;

	public FacturacionService(JdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager,
			 ConfiguracionService configuracion) {
		this.jdbcTemplate = jdbcTemplate;
		this.transactionManager = transactionManager;
		this.configuracion = configuracion;

		this.facturaElectronica = this.configuracion.getBoolean(FACTURA_ELECTRONICA);
		this.tipoFacturaName = this.facturaElectronica ? "E" : " ";
		this.nroLineasPorFactura = this.configuracion.getInt(NUMERO_LINEAS_FACTURA);

	}

	public List<ResultadoFacturacionDTO> facturar(List<VentaDTO> ventas) {

		List<ResultadoFacturacionDTO> results = new ArrayList<>();

		// Configuramos la transacción para que sea INDEPENDIENTE por cada venta
		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		for (VentaDTO venta : ventas) {

			List<ResultadoFacturacionDTO> resultado = null;

			try {
				// Ejecutamos la lógica dentro de una transacción aislada
				resultado = transactionTemplate.execute(status -> {
					try {
						return procesarVenta(venta);
					} catch (Exception e) {
						// Si algo falla, marcamos rollback solo para esta venta
						status.setRollbackOnly();
						throw e; // Relanzamos para capturarla fuera
					}
				});
			} catch (Exception e) {
				// Capturamos el error para no detener el bucle
				String errorMsg = e.getMessage();
				// Simplificar mensaje si es muy largo
				if (errorMsg.length() > 100)
					errorMsg = errorMsg.substring(0, 100) + "...";

				resultado = null;//new ResultadoFacturacionDTO(venta.getId(), false, "Fallo: " + errorMsg, "");
			}

			results.addAll(resultado);
		}

		return results;

	}

	private List<ResultadoFacturacionDTO> procesarVenta(VentaDTO venta) {

		List<ResultadoFacturacionDTO> result = new ArrayList<>();

		int nroLinea = 0;
		String numeroFactura = null;
		String idBaseDatos = null;
		for (VentaDetalleDTO detalle : venta.getDetalles()) {
			try {
				if (nroLinea % nroLineasPorFactura == 0) {
					// Se trata de una nueva factura, por lo que debo hacer todo el proceso de
					// grabar un nuevo encabezado
					numeroFactura = obtenerNumeroFactura(this.tipoFacturaName);
					idBaseDatos = obteberYActualizarIdentificadorBaseDatos();
					procesarEncabezadoVenta(venta, numeroFactura, idBaseDatos);
				}

				procesarDetalleVenta(venta, detalle, numeroFactura, idBaseDatos);
				result.add(new ResultadoFacturacionDTO(venta.getId(), true,
						String.format("Se ha generado factura %s con id bd %s", numeroFactura, idBaseDatos),
						numeroFactura));
			} catch (EmptyResultDataAccessException ex) {
				throw new RuntimeException("Producto no encontrado en BD destino: " + detalle.getIdProducto());
			}
		}

		return result;

	}
	
	private void procesarEncabezadoVenta(VentaDTO venta, String numeroFactura, String idBaseDatos) {
		String sql = "insert into encabezadocumento (fecha, vence, afectoexento, rut, local, id, tipo, numero, codigo, tipo1, publicadonro ) values  (?, ?, ?, ?, ?, ?, ?, ?, ?,?,?)";

		LocalDate fechaVencimiento = venta.getFecha().plusDays(1);

		jdbcTemplate.update(connection -> {
			PreparedStatement pstmt = connection.prepareStatement(sql);
			pstmt.setDate(1, Date.valueOf(venta.getFecha()));
			pstmt.setDate(2, Date.valueOf(fechaVencimiento));

			pstmt.setString(3, "A");
			pstmt.setString(4, venta.getRutCliente());
			pstmt.setString(5, "000");
			pstmt.setString(6, idBaseDatos);
			pstmt.setString(7, "06");
			pstmt.setString(8, numeroFactura);
			pstmt.setString(9, venta.getCodigoCliente() + " ");
			pstmt.setString(10, this.tipoFacturaName);
			pstmt.setString(11, numeroFactura);

			pstmt.executeUpdate();
			return pstmt;
		});
	}

	private void procesarDetalleVenta(VentaDTO venta, VentaDetalleDTO detalle, String numeroFactura,
			String idBaseDatos) {

		if(detalle.getPiezas() >0 )
		{
			
		}
		else {
			
		}

	}



	private String obteberYActualizarIdentificadorBaseDatos() {
		String sql = """
				    UPDATE PARAMETROS
				    SET FolioDocumento = RIGHT('0000000000' + CAST(CAST(FolioDocumento AS BIGINT) + 1 AS VARCHAR(20)), 10)
				    OUTPUT INSERTED.FolioDocumento
				""";
		return jdbcTemplate.queryForObject(sql, String.class);
	}

	private String obtenerNumeroFactura(String tipoFactura) {
		int intentos = 0;
		int maxIntentos = 5; // Intentará 5 veces antes de rendirse

		while (intentos < maxIntentos) {
			try {
				// 1. CALCULAR: Buscamos el máximo actual y sumamos 1
				// COALESCE(..., 0) maneja el caso de la tabla vacía.
				String sqlMax = "SELECT COALESCE(MAX(CAST(numero AS BIGINT)), 0) + 1 FROM folios WHERE tipo = '06' AND tipo1 = ?";

				Long numeroCalculado = jdbcTemplate.queryForObject(sqlMax, Long.class, tipoFactura);

				// 2. FORMATEAR: Convertimos a String con 10 dígitos y ceros a la izquierda
				String nuevoFolioString = String.format("%010d", numeroCalculado);

				// 3. INSERTAR (RESERVAR): Intentamos guardar en la BD
				// Es vital que la tabla 'folios' tenga UNIQUE CONSTRAINT en (tipo, tipo1,
				// numero)
				String sqlInsert = "INSERT INTO folios (numero, tipo1, tipo) VALUES (?, ?, '06')";

				jdbcTemplate.update(sqlInsert, nuevoFolioString, tipoFactura);

				// 4. ÉXITO: Si llegamos aquí, nadie nos ganó el número. Lo retornamos.
				return nuevoFolioString;

			} catch (DuplicateKeyException e) {
				// 5. FALLO: Alguien insertó ese número milisegundos antes que nosotros.
				// No lanzamos error, simplemente aumentamos el contador y el 'while' repetirá
				// el proceso.
				intentos++;
				System.out.println("Colisión de folios detectada. Reintentando... Intento " + intentos);
			}
		}

		// Si sale del while, falló 5 veces seguidas (sistema muy saturado)
		throw new RuntimeException(
				"No se pudo obtener un folio después de " + maxIntentos + " intentos. Intente nuevamente.");
	}
}
