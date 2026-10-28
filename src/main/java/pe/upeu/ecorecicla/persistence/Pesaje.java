package pe.upeu.ecorecicla.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "pesajes", uniqueConstraints = @UniqueConstraint(name = "uk_pesaje_muestra", columnNames = "codigo_muestra"),
        indexes = {
            @Index(name = "idx_pesaje_ruta_punto", columnList = "ruta_id,punto_id"),
            @Index(name = "idx_pesaje_operario_fecha", columnList = "operario_id,fecha_hora")
        })
@Check(constraints = "peso_bruto >= 0 AND tara >= 0 AND peso_neto >= 0 AND peso_neto = peso_bruto - tara")
public class Pesaje {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ruta_id", nullable = false, foreignKey = @ForeignKey(name = "fk_pesaje_ruta"))
    Ruta ruta;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "punto_id", nullable = false, foreignKey = @ForeignKey(name = "fk_pesaje_punto"))
    PuntoAcopio punto;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operario_id", nullable = false, foreignKey = @ForeignKey(name = "fk_pesaje_operario"))
    Usuario operario;
    @Column(nullable = false, length = 80)
    String material;
    @Column(name = "peso_bruto", nullable = false, precision = 10, scale = 3)
    BigDecimal pesoBruto;
    @Column(nullable = false, precision = 10, scale = 3)
    BigDecimal tara;
    @Column(name = "peso_neto", nullable = false, precision = 10, scale = 3)
    BigDecimal pesoNeto;
    @Column(length = 500)
    String observaciones;
    @Column(name = "fecha_hora", nullable = false)
    LocalDateTime fechaHora;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    EstadoPesaje estado;
    @Column(name = "codigo_muestra", length = 40)
    String codigoMuestra;

    protected Pesaje() {}
    public Pesaje(Ruta ruta, PuntoAcopio punto, Usuario operario, String material,
                  BigDecimal pesoBruto, BigDecimal tara, LocalDateTime fechaHora, String codigoMuestra) {
        if (pesoBruto == null || tara == null || tara.signum() < 0 || pesoBruto.compareTo(tara) < 0) {
            throw new IllegalArgumentException("Los pesos deben cumplir: bruto >= tara >= 0");
        }
        this.ruta = ruta; this.punto = punto; this.operario = operario; this.material = material;
        this.pesoBruto = pesoBruto; this.tara = tara; this.pesoNeto = pesoBruto.subtract(tara);
        this.fechaHora = fechaHora; this.codigoMuestra = codigoMuestra; this.estado = EstadoPesaje.ACTIVO;
    }
    public Long getId() { return id; }
    public Ruta getRuta() { return ruta; }
    public PuntoAcopio getPunto() { return punto; }
    public Usuario getOperario() { return operario; }
    public String getMaterial() { return material; }
    public BigDecimal getPesoBruto() { return pesoBruto; }
    public BigDecimal getTara() { return tara; }
    public BigDecimal getPesoNeto() { return pesoNeto; }
    public String getObservaciones() { return observaciones; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public EstadoPesaje getEstado() { return estado; }
    public void corregir(String material, BigDecimal pesoBruto, BigDecimal tara, String observaciones) {
        this.material = material;
        this.pesoBruto = pesoBruto;
        this.tara = tara;
        this.pesoNeto = pesoBruto.subtract(tara);
        this.observaciones = observaciones;
    }
    public void anular() { this.estado = EstadoPesaje.ANULADO; }
}
