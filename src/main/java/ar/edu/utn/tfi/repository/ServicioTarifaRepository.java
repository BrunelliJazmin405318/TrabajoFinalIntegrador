package ar.edu.utn.tfi.repository;

import ar.edu.utn.tfi.domain.ServicioTarifa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ServicioTarifaRepository extends JpaRepository<ServicioTarifa, Long> {
    Optional<ServicioTarifa> findByNombreServicioAndVehiculoTipo(String nombreServicio, String vehiculoTipo);
    List<ServicioTarifa> findByVehiculoTipoAndNombreServicioIn(String vehiculoTipo, Collection<String> nombres);
}

