package cl.eos.dipalza.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;

@Setter
@Getter
@EqualsAndHashCode
@Entity
@Table(name = "posicion", schema = "dbo")
public class Posicion {
    @Id
    private String vendedorId;

    private Point posicion;
    @Column(name = "ultimaActualizacion")
    private LocalDateTime fechaHora;

}
