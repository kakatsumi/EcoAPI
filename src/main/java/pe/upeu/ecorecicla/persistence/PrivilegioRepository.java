package pe.upeu.ecorecicla.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrivilegioRepository extends JpaRepository<Privilegio, Long> {
    Optional<Privilegio> findByCodigo(String codigo);
}
