package cl.eos.dipalza.model;

public record ResultadoFacturacionDTO(Long idVentaOriginal, boolean exitoso, String mensaje,
		String nuevoFolioGenerado) {
};
