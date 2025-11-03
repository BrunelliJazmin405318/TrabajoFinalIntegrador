package ar.edu.utn.tfi.repository;

import ar.edu.utn.tfi.domain.MpEventLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MpEventLogRepository extends JpaRepository<MpEventLog, Long> {
    boolean existsByRequestId(String requestId);
}
