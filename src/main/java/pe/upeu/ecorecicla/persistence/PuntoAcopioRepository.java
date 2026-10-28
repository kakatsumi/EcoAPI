package pe.upeu.ecorecicla.persistence;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PuntoAcopioRepository extends JpaRepository<PuntoAcopio, Long> {
    Optional<PuntoAcopio> findByNombre(String nombre);
    Page<PuntoAcopio> findByActivoTrue(Pageable pageable);
    Page<PuntoAcopio> findByActivoTrueAndZonaIgnoreCase(String zona, Pageable pageable);
}
