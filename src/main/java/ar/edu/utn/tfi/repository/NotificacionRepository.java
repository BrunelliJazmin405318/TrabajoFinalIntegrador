package ar.edu.utn.tfi.repository;

import ar.edu.utn.tfi.domain.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    // Usado por NotificacionService:
    // trae las últimas 20 no leídas para un email de cliente
    List<Notificacion> findTop20ByClienteEmailAndReadAtIsNullOrderByCreatedAtDesc(String clienteEmail);
    List<Notificacion> findTop20ByNroOrdenOrderByCreatedAtDesc(String nroOrden);

}
