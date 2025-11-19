// src/test/java/ar/edu/utn/tfi/repository/OrdenEtapaHistorialRepositoryTest.java
package ar.edu.utn.tfi.repository;

import ar.edu.utn.tfi.domain.OrdenEtapaHistorial;
import ar.edu.utn.tfi.infra.PostgresTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrdenEtapaHistorialRepositoryTest extends PostgresTestBase {

    @Autowired
    OrdenEtapaHistorialRepository repo;

    @Test
    @DisplayName("findByOrdenIdOrdenadoPorCatalogo(...) corre sin errores")
    void findByOrdenIdOrdenadoPorCatalogo_ok() {
        // Usamos un ID real o cualquiera (si no hay datos, igual no debe romper)
        Long ordenId = 1L;

        List<OrdenEtapaHistorial> res = repo.findByOrdenIdOrdenadoPorCatalogo(ordenId);

        assertNotNull(res);
        // Si hay resultados, chequeamos que todos sean de esa orden
        for (OrdenEtapaHistorial h : res) {
            assertEquals(ordenId, h.getOrdenId());
            assertNotNull(h.getEtapaCodigo());
        }
    }

    @Test
    @DisplayName("findTopByOrdenIdAndFechaFinIsNullOrderByFechaInicioDesc(...) corre sin errores")
    void findTopByOrdenIdAndFechaFinIsNullOrderByFechaInicioDesc_ok() {
        Long ordenId = 1L;

        var opt = repo.findTopByOrdenIdAndFechaFinIsNullOrderByFechaInicioDesc(ordenId);

        // Solo verificamos que no rompa y que, si existe, tenga ordenId correcto
        opt.ifPresent(h -> assertEquals(ordenId, h.getOrdenId()));
    }
}