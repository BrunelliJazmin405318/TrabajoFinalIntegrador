// src/test/java/ar/edu/utn/tfi/repository/OrdenTrabajoReportRepositoryTest.java
package ar.edu.utn.tfi.repository;

import ar.edu.utn.tfi.infra.PostgresTestBase;
import ar.edu.utn.tfi.repository.OrdenTrabajoReportRepository.ClienteRanking;
import ar.edu.utn.tfi.repository.OrdenTrabajoReportRepository.TipoCantidad;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrdenTrabajoReportRepositoryTest extends PostgresTestBase {

    @Autowired
    OrdenTrabajoReportRepository repo;

    @Test
    @DisplayName("contarPorTipoEntre(...) corre sin errores y respeta la proyección")
    void contarPorTipoEntre_ok() {
        LocalDateTime desde = LocalDateTime.now().minusYears(5);
        LocalDateTime hasta = LocalDateTime.now().plusDays(1);

        List<TipoCantidad> res = repo.contarPorTipoEntre(desde, hasta);

        assertNotNull(res);
        for (TipoCantidad row : res) {
            assertNotNull(row.getTipo());
            assertNotNull(row.getCnt());
        }
    }

    @Test
    @DisplayName("contarMotoresPorEtapaRango(...) corre sin errores")
    void contarMotoresPorEtapaRango_ok() {
        LocalDateTime desde = LocalDateTime.now().minusYears(5);
        LocalDateTime hasta = LocalDateTime.now().plusDays(1);

        List<Map<String, Object>> res = repo.contarMotoresPorEtapaRango(desde, hasta);

        assertNotNull(res);
        // Si hay filas, validamos que tengan claves básicas
        for (Map<String, Object> row : res) {
            assertTrue(row.containsKey("etapa"));
            assertTrue(row.containsKey("cnt"));
        }
    }

    @Test
    @DisplayName("rankingClientesEntre(...) corre sin errores y respeta la proyección")
    void rankingClientesEntre_ok() {
        LocalDateTime desde = LocalDateTime.now().minusYears(5);
        LocalDateTime hasta = LocalDateTime.now().plusDays(1);

        List<ClienteRanking> res = repo.rankingClientesEntre(desde, hasta, PageRequest.of(0, 10));

        assertNotNull(res);
        for (ClienteRanking row : res) {
            assertNotNull(row.getClienteId());
            assertNotNull(row.getNombre());
            // teléfono podría ser null, así que no lo hacemos obligatorio
            assertNotNull(row.getCantidad());
        }
    }
}