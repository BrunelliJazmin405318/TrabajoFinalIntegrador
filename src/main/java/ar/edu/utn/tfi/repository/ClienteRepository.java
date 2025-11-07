package ar.edu.utn.tfi.repository;

import ar.edu.utn.tfi.domain.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByNombreAndTelefono(String nombre, String telefono);
}