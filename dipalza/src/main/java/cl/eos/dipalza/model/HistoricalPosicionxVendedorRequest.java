package cl.eos.dipalza.model;

import java.time.LocalDateTime;

public record HistoricalPosicionxVendedorRequest(String vendedorId, LocalDateTime fechaInicio, LocalDateTime fechaTermino, int page, int size) {
}
