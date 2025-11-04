// src/main/java/ar/edu/utn/tfi/repository/PagoManualRepository.java
package ar.edu.utn.tfi.repository;

import ar.edu.utn.tfi.domain.PagoManual;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagoManualRepository extends JpaRepository<PagoManual, Long> {
}