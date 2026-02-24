package cl.eos.dipalza.entity;

public enum EstadoVenta {

	OPENED, // Se está creando aún
	FINISHED, // El vendedor terminó de crear la venta
	REOPENED, // El vendedor está modificando la venta
	CLOSED; // La venta ya se transfirió al otro sistema, no se puede tocar.

	public static EstadoVenta estadoVentaFromName(String estado) {
		  switch (estado) {
		    case "OPENED": return EstadoVenta.OPENED;
		    case "FINISHED":  return EstadoVenta.FINISHED;
		    case "REOPENED":  return EstadoVenta.REOPENED;
		    case "CLOSED":  return EstadoVenta.CLOSED;
		    default:         return EstadoVenta.OPENED;
		  }
	}
		  
}
