package pe.upeu.ecorecicla.persistence;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "rutas", uniqueConstraints = @UniqueConstraint(name = "uk_ruta_fecha_operario", columnNames = {"fecha", "operario_id"}),
        indexes = @Index(name = "idx_ruta_fecha_estado", columnList = "fecha,estado"))
public class Ruta {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(nullable = false)
    LocalDate fecha;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    EstadoRuta estado;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operario_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ruta_operario"))
    Usuario operario;

    protected Ruta() {}
    public Ruta(LocalDate fecha, EstadoRuta estado, Usuario operario) {
        this.fecha = fecha; this.estado = estado; this.operario = operario;
    }
    public Long getId() { return id; }
    public LocalDate getFecha() { return fecha; }
    public EstadoRuta getEstado() { return estado; }
    public Usuario getOperario() { return operario; }
}
