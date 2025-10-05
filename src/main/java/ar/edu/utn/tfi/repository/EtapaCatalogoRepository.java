package ar.edu.utn.tfi.repository;

import ar.edu.utn.tfi.domain.EtapaCatalogo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EtapaCatalogoRepository extends JpaRepository<EtapaCatalogo, String> {
    Optional<EtapaCatalogo> findByCodigo(String codigo);
    Optional<EtapaCatalogo> findByOrden(Integer orden);
}
