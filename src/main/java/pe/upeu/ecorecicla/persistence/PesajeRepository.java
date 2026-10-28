package pe.upeu.ecorecicla.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PesajeRepository extends JpaRepository<Pesaje, Long> {
    boolean existsByCodigoMuestra(String codigoMuestra);
}
