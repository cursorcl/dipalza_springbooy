package cl.eos.dipalza.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Point;
import java.time.LocalDateTime;

@Setter
@Getter
@EqualsAndHashCode
@Entity
@Table(name = "historial_posicion", schema = "dbo")
public class HistorialPosicion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 3)
    private String vendedorId;

    private LocalDateTime fechaHora;

    @Column(columnDefinition = "geography")
    private Point posicion; // Representación de punto geográfico



}
