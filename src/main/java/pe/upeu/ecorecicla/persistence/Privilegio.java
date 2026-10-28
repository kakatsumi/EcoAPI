package pe.upeu.ecorecicla.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "privilegios", uniqueConstraints = @UniqueConstraint(name = "uk_privilegio_codigo", columnNames = "codigo"))
public class Privilegio {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(nullable = false, length = 50)
    String codigo;

    protected Privilegio() {}
    public Privilegio(String codigo) { this.codigo = codigo; }
    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
}
