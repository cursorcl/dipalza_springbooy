package cl.eos.dipalza.entity;

public enum EstadoVenta {

	/** La venta está en proceso, se creó el encabezado y aún se está realizando la venta. */
	OPENED,
	/** El vendedor confirma la venta, queda lista para ser facturada. */
	FINISHED,
	/** Venta que se encuentra facturada, no se debe alterar */
	CLOSED;

	public static EstadoVenta fromName(String estado) {

		if (estado == null || estado.isBlank()) {
			return OPENED;
		}

		try {
			return EstadoVenta.valueOf(estado.trim().toUpperCase());
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	public boolean canTransitionTo(EstadoVenta newState) {
		return switch (this) {
			case OPENED -> newState == FINISHED;
			case FINISHED -> newState == CLOSED;
			case CLOSED -> false;
		};
	}

}
