package pe.upeu.ecorecicla.persistence;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "usuarios", uniqueConstraints = @UniqueConstraint(name = "uk_usuario_username", columnNames = "username"))
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(nullable = false, length = 80)
    String username;
    @Column(name = "password_hash", nullable = false, length = 100)
    String passwordHash;
    @Column(nullable = false)
    boolean activo = true;
    @ManyToMany
    @JoinTable(name = "usuario_roles", joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "rol_id"),
            uniqueConstraints = @UniqueConstraint(name = "uk_usuario_rol", columnNames = {"usuario_id", "rol_id"}))
    Set<Rol> roles = new HashSet<>();

    protected Usuario() {}
    public Usuario(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isActivo() { return activo; }
    public Set<Rol> getRoles() { return roles; }
}
