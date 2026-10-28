package pe.upeu.ecorecicla.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "puntos_acopio", uniqueConstraints = @UniqueConstraint(name = "uk_punto_nombre", columnNames = "nombre"),
        indexes = @Index(name = "idx_punto_zona_activo", columnList = "zona,activo"))
public class PuntoAcopio {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(nullable = false, length = 100)
    String nombre;
    @Column(nullable = false, length = 200)
    String direccion;
    @Column(nullable = false, length = 60)
    String zona;
    @Column(nullable = false)
    boolean activo = true;

    protected PuntoAcopio() {}
    public PuntoAcopio(String nombre, String direccion, String zona) {
        this.nombre = nombre; this.direccion = direccion; this.zona = zona;
    }
    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDireccion() { return direccion; }
    public String getZona() { return zona; }
    public boolean isActivo() { return activo; }
}
