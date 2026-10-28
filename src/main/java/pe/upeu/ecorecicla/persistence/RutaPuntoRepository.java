package pe.upeu.ecorecicla.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RutaPuntoRepository extends JpaRepository<RutaPunto, Long> {
    boolean existsByRutaAndPunto(Ruta ruta, PuntoAcopio punto);
    List<RutaPunto> findByRutaOrderByOrdenVisitaAsc(Ruta ruta);
    Optional<RutaPunto> findByRutaAndPunto(Ruta ruta, PuntoAcopio punto);
}
