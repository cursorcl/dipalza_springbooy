package cl.eos.dipalza.model;

import java.time.LocalDateTime;

public record HistoricalPosicionRequest(LocalDateTime fechaInicio, LocalDateTime fechaTermino, int page, int size) {
}
