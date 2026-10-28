package pe.upeu.ecorecicla.persistence;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RutaRepository extends JpaRepository<Ruta, Long> {
    Optional<Ruta> findByFechaAndOperario(LocalDate fecha, Usuario operario);
}
