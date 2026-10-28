package pe.upeu.ecorecicla.persistence;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles", uniqueConstraints = @UniqueConstraint(name = "uk_rol_nombre", columnNames = "nombre"))
public class Rol {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    NombreRol nombre;
    @ManyToMany
    @JoinTable(name = "rol_privilegios", joinColumns = @JoinColumn(name = "rol_id"),
            inverseJoinColumns = @JoinColumn(name = "privilegio_id"),
            uniqueConstraints = @UniqueConstraint(name = "uk_rol_privilegio", columnNames = {"rol_id", "privilegio_id"}))
    Set<Privilegio> privilegios = new HashSet<>();

    protected Rol() {}
    public Rol(NombreRol nombre) { this.nombre = nombre; }
    public Long getId() { return id; }
    public NombreRol getNombre() { return nombre; }
    public Set<Privilegio> getPrivilegios() { return privilegios; }
}
