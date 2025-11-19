// src/test/java/ar/edu/utn/tfi/repository/IngresosReportRepositoryTest.java
package ar.edu.utn.tfi.repository;

import ar.edu.utn.tfi.infra.PostgresTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class IngresosReportRepositoryTest extends PostgresTestBase {

    @Autowired
    IngresosReportRepository repo;

    @Test
    @DisplayName("listarIngresos(...) corre sin errores y respeta la proyección")
    void listarIngresos_ok() {
        LocalDateTime desde = LocalDateTime.now().minusYears(5);
        LocalDateTime hasta = LocalDateTime.now().plusDays(1);

        List<IngresosReportRepository.IngresoMesTipo> res =
                repo.listarIngresos(desde, hasta);

        assertNotNull(res);

        // Si hay resultados, validamos estructura básica de la proyección
        for (IngresosReportRepository.IngresoMesTipo row : res) {
            assertNotNull(row.getMes());
            assertNotNull(row.getTipo());
            assertNotNull(row.getTotal());

            assertTrue(
                    "SENA".equalsIgnoreCase(row.getTipo())
                            || "FINAL".equalsIgnoreCase(row.getTipo()),
                    "Tipo debe ser SENA o FINAL"
            );
        }
    }
}