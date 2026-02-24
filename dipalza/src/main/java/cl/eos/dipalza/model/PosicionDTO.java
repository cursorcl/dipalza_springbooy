package cl.eos.dipalza.model;

import java.time.LocalDateTime;

public record PosicionDTO (String vendedorId,  LocalDateTime fechaHora, double latitude, double longitude) {};
