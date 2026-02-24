package cl.eos.dipalza.model;

import java.time.LocalDateTime;

public record HistorialPosicionDTO(Long id, String vendedorId, LocalDateTime fechaHora, double latitude, double longitude) {};
