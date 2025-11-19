// src/test/java/ar/edu/utn/tfi/service/ReportesServiceTest.java
package ar.edu.utn.tfi.service;

import ar.edu.utn.tfi.repository.OrdenTrabajoReportRepository;
import ar.edu.utn.tfi.web.dto.ClienteRankingDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportesServiceTest {

    @Mock
    OrdenTrabajoReportRepository ordenReportRepo;

    ReportesService service;

    @BeforeEach
    void setUp() {
        service = new ReportesService(ordenReportRepo);
    }

    // ================== Test motoresVsTapas ==================

    /**
     * Verifica que motoresVsTapas:
     * - Llame al repo y procese la lista de TipoCantidad
     * - Devuelva correctamente motores, tapas, total y ratio
     */
    @Test
    @DisplayName("motoresVsTapas calcula correctamente cantidades y ratio")
    void motoresVsTapas_ok() {
        // Mock de la proyección TipoCantidad (MOTOR: 7, TAPA: 3)
        List<OrdenTrabajoReportRepository.TipoCantidad> rows = List.of(
                new TipoCantidadMock("MOTOR", 7L),
                new TipoCantidadMock("TAPA", 3L)
        );

        // cuando el repo reciba cualquier rango de fechas, devuelve esas filas
        when(ordenReportRepo.contarPorTipoEntre(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(rows);

        LocalDate desde = LocalDate.of(2025, 1, 1);
        LocalDate hasta = LocalDate.of(2025, 1, 31);

        Map<String, Object> result = service.motoresVsTapas(desde, hasta);

        assertEquals(7L, result.get("motores"));
        assertEquals(3L, result.get("tapas"));
        assertEquals(10L, result.get("total"));

        double ratio = (double) result.get("ratio");
        assertEquals(7.0 / 3.0, ratio, 0.0001);
    }

    // Implementación mock de la proyección TipoCantidad
    private static class TipoCantidadMock implements OrdenTrabajoReportRepository.TipoCantidad {
        private final String tipo;
        private final Long cnt;

        TipoCantidadMock(String tipo, Long cnt) {
            this.tipo = tipo;
            this.cnt = cnt;
        }

        @Override
        public String getTipo() {
            return tipo;
        }

        @Override
        public Long getCnt() {
            return cnt;
        }
    }

    // ================== Test motoresPorEtapa ==================

    /**
     * Verifica que motoresPorEtapa:
     * - Mapea bien la lista de Map devuelta por el repo
     * - Arma listas de etapas, cantidades y el total
     */
    @Test
    @DisplayName("motoresPorEtapa arma listas y total correctamente")
    void motoresPorEtapa_ok() {
        // Mock de filas devueltas por el native query:
        // etapa INGRESO -> 5
        // etapa LISTO_RETIRAR -> 2
        List<Map<String, Object>> rows = List.of(
                Map.of("etapa", "INGRESO", "cnt", 5L),
                Map.of("etapa", "LISTO_RETIRAR", "cnt", 2L)
        );

        when(ordenReportRepo.contarMotoresPorEtapaRango(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(rows);

        LocalDate from = LocalDate.of(2025, 2, 1);
        LocalDate to   = LocalDate.of(2025, 2, 28);

        Map<String, Object> result = service.motoresPorEtapa(from, to);

        @SuppressWarnings("unchecked")
        List<String> etapas = (List<String>) result.get("etapas");
        @SuppressWarnings("unchecked")
        List<Long> cantidades = (List<Long>) result.get("cantidades");
        long total = (long) result.get("total");

        assertEquals(List.of("INGRESO", "LISTO_RETIRAR"), etapas);
        assertEquals(List.of(5L, 2L), cantidades);
        assertEquals(7L, total);
    }

    // ================== Test rankingClientesFrecuentes ==================

    /**
     * Verifica que rankingClientesFrecuentes:
     * - Aplique defaults de fechas/top sin romper
     * - Mapee correctamente la proyección ClienteRanking a ClienteRankingDTO
     */
    @Test
    @DisplayName("rankingClientesFrecuentes mapea la proyección a DTO")
    void rankingClientesFrecuentes_ok() {
        // rows devueltos por el repo (proyección ClienteRanking)
        List<OrdenTrabajoReportRepository.ClienteRanking> rows = List.of(
                new ClienteRankingMock(1L, "Juan Pérez", "3510000000", 5L),
                new ClienteRankingMock(2L, "María López", "3511111111", 3L)
        );

        when(ordenReportRepo.rankingClientesEntre(any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(rows);

        // llamamos con from/to/top en null para que use los defaults internos
        List<ClienteRankingDTO> out = service.rankingClientesFrecuentes(null, null, null);

        assertEquals(2, out.size());

        ClienteRankingDTO c1 = out.get(0);
        assertEquals(1L, c1.clienteId());
        assertEquals("Juan Pérez", c1.clienteNombre());
        assertEquals("3510000000", c1.telefono());
        assertEquals(5L, c1.cantOrdenes());

        ClienteRankingDTO c2 = out.get(1);
        assertEquals(2L, c2.clienteId());
        assertEquals("María López", c2.clienteNombre());
        assertEquals("3511111111", c2.telefono());
        assertEquals(3L, c2.cantOrdenes());
    }

    // Implementación mock de la proyección ClienteRanking
    private static class ClienteRankingMock implements OrdenTrabajoReportRepository.ClienteRanking {
        private final Long clienteId;
        private final String nombre;
        private final String telefono;
        private final Long cantidad;

        ClienteRankingMock(Long clienteId, String nombre, String telefono, Long cantidad) {
            this.clienteId = clienteId;
            this.nombre = nombre;
            this.telefono = telefono;
            this.cantidad = cantidad;
        }

        @Override
        public Long getClienteId() {
            return clienteId;
        }

        @Override
        public String getNombre() {
            return nombre;
        }

        @Override
        public String getTelefono() {
            return telefono;
        }

        @Override
        public Long getCantidad() {
            return cantidad;
        }
    }
}