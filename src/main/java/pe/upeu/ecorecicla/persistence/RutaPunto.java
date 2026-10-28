package pe.upeu.ecorecicla.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "ruta_puntos", uniqueConstraints = {
        @UniqueConstraint(name = "uk_ruta_punto", columnNames = {"ruta_id", "punto_id"}),
        @UniqueConstraint(name = "uk_ruta_orden", columnNames = {"ruta_id", "orden_visita"})
}, indexes = @Index(name = "idx_ruta_punto_punto", columnList = "punto_id"))
public class RutaPunto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ruta_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ruta_punto_ruta"))
    Ruta ruta;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "punto_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ruta_punto_punto"))
    PuntoAcopio punto;
    @Column(name = "orden_visita", nullable = false)
    int ordenVisita;
    @Column(name = "motivo_omision", length = 300)
    String motivoOmision;

    protected RutaPunto() {}
    public RutaPunto(Ruta ruta, PuntoAcopio punto, int ordenVisita) {
        this.ruta = ruta; this.punto = punto; this.ordenVisita = ordenVisita;
    }
    public Long getId() { return id; }
    public Ruta getRuta() { return ruta; }
    public PuntoAcopio getPunto() { return punto; }
    public int getOrdenVisita() { return ordenVisita; }
    public String getMotivoOmision() { return motivoOmision; }
}
