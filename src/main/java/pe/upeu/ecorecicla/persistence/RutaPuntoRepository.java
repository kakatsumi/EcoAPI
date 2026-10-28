package pe.upeu.ecorecicla.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RutaPuntoRepository extends JpaRepository<RutaPunto, Long> {
    boolean existsByRutaAndPunto(Ruta ruta, PuntoAcopio punto);
}
