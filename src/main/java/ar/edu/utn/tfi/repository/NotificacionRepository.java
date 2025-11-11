package ar.edu.utn.tfi.repository;

import ar.edu.utn.tfi.domain.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    List<Notificacion> findTop20ByClienteEmailAndReadAtIsNullOrderByCreatedAtDesc(String email);
}

