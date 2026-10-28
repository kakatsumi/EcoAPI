package pe.upeu.ecorecicla.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    @EntityGraph(attributePaths = {"roles", "roles.privilegios"})
    Optional<Usuario> findByUsername(String username);
}
