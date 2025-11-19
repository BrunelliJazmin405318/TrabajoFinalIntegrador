// src/test/java/ar/edu/utn/tfi/service/ReporteExportServiceTest.java
package ar.edu.utn.tfi.service;

import ar.edu.utn.tfi.web.dto.ClienteRankingDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests de ReporteExportService.
 *
 * En estos tests NO validamos el contenido gráfico del PDF/XLSX,
 * sino que:
 *   - Se llama al service de reportes correcto.
 *   - Se generan bytes (no null / length > 0) cuando hay datos.
 *   - No explota cuando la lista viene vacía.
 */
@ExtendWith(MockitoExtension.class)
class ReporteExportServiceTest {

    @Mock
    ReportesService reportesService;

    @Mock
    IngresosReportService ingresosReportService;

    ReporteExportService service;

    @BeforeEach
    void setUp() {
        // Inyectamos los mocks en el service
        service = new ReporteExportService(reportesService, ingresosReportService);
    }

    // =========================================================
    //                CLIENTES FRECUENTES
    // =========================================================

    @Test
    @DisplayName("clientesFrecuentesXlsx genera XLSX con filas cuando hay datos")
    void clientesFrecuentesXlsx_conDatos() {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to   = LocalDate.of(2025, 1, 31);
        int top = 5;

        // armamos un ranking de ejemplo
        List<ClienteRankingDTO> ranking = List.of(
                new ClienteRankingDTO(1L, "Juan Pérez", "3510000000", 3L),
                new ClienteRankingDTO(2L, "ACME SRL", "3511111111", 2L)
        );

        when(reportesService.rankingClientesFrecuentes(from, to, top))
                .thenReturn(ranking);

        // act
        byte[] bytes = service.clientesFrecuentesXlsx(from, to, top);

        // assert
        assertNotNull(bytes);
        assertTrue(bytes.length > 0, "El XLSX no debería ser vacío");
        // verificamos que se delegó correctamente al service de reportes
        verify(reportesService).rankingClientesFrecuentes(from, to, top);
    }

    @Test
    @DisplayName("clientesFrecuentesPdf genera PDF aunque la lista venga vacía")
    void clientesFrecuentesPdf_listaVacia() {
        LocalDate from = LocalDate.of(2025, 2, 1);
        LocalDate to   = LocalDate.of(2025, 2, 28);
        int top = 10;

        when(reportesService.rankingClientesFrecuentes(from, to, top))
                .thenReturn(List.of()); // sin resultados

        // act
        byte[] bytes = service.clientesFrecuentesPdf(from, to, top);

        // assert
        assertNotNull(bytes);
        assertTrue(bytes.length > 0, "Debería generar un PDF con la tabla vacía");
        verify(reportesService).rankingClientesFrecuentes(from, to, top);
    }

    // =========================================================
    //                MOTORES VS TAPAS
    // =========================================================

    @Test
    @DisplayName("motoresVsTapasXlsx genera XLSX usando los datos del reporte")
    void motoresVsTapasXlsx_ok() {
        LocalDate from = LocalDate.of(2025, 3, 1);
        LocalDate to   = LocalDate.of(2025, 3, 31);

        Map<String, Object> data = Map.of(
                "motores", 10L,
                "tapas", 5L
        );

        when(reportesService.motoresVsTapas(from, to)).thenReturn(data);

        // act
        byte[] bytes = service.motoresVsTapasXlsx(from, to);

        // assert
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
        verify(reportesService).motoresVsTapas(from, to);
    }

    @Test
    @DisplayName("motoresVsTapasPdf genera PDF con conteos de motores y tapas")
    void motoresVsTapasPdf_ok() {
        LocalDate from = LocalDate.of(2025, 3, 1);
        LocalDate to   = LocalDate.of(2025, 3, 31);

        Map<String, Object> data = Map.of(
                "motores", 7L,
                "tapas", 3L
        );
        when(reportesService.motoresVsTapas(from, to)).thenReturn(data);

        // act
        byte[] bytes = service.motoresVsTapasPdf(from, to);

        // assert
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
        verify(reportesService).motoresVsTapas(from, to);
    }

    // =========================================================
    //                MOTORES POR ETAPA
    // =========================================================

    @Test
    @DisplayName("motoresPorEtapaXlsx genera XLSX con etapas y cantidades")
    void motoresPorEtapaXlsx_ok() {
        LocalDate from = LocalDate.of(2025, 4, 1);
        LocalDate to   = LocalDate.of(2025, 4, 30);

        Map<String, Object> data = Map.of(
                "etapas", List.of("INGRESO", "MAQUINADO", "ENTREGADO"),
                "cantidades", List.of(5L, 3L, 2L)
        );

        when(reportesService.motoresPorEtapa(from, to)).thenReturn(data);

        // act
        byte[] bytes = service.motoresPorEtapaXlsx(from, to);

        // assert
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
        verify(reportesService).motoresPorEtapa(from, to);
    }

    @Test
    @DisplayName("motoresPorEtapaPdf genera PDF aún si las listas vienen vacías")
    void motoresPorEtapaPdf_sinDatos() {
        LocalDate from = LocalDate.of(2025, 4, 1);
        LocalDate to   = LocalDate.of(2025, 4, 30);

        Map<String, Object> data = Map.of(
                "etapas", List.of(),
                "cantidades", List.of()
        );

        when(reportesService.motoresPorEtapa(from, to)).thenReturn(data);

        // act
        byte[] bytes = service.motoresPorEtapaPdf(from, to);

        // assert
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
        verify(reportesService).motoresPorEtapa(from, to);
    }

    // =========================================================
    //        INGRESOS: SEÑAS VS FINALES
    // =========================================================

    @Test
    @DisplayName("ingresosSenasFinalesXlsx genera XLSX con labels y totales")
    void ingresosSenasFinalesXlsx_ok() {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to   = LocalDate.of(2025, 12, 31);

        Map<String, Object> data = Map.of(
                "labels", List.of("2025-01", "2025-02"),
                "senas", List.of(10000.0, 15000.0),
                "finales", List.of(20000.0, 25000.0),
                "totalSena", new BigDecimal("25000.00"),
                "totalFinal", new BigDecimal("45000.00"),
                "totalGeneral", new BigDecimal("70000.00")
        );

        when(ingresosReportService.ingresosSenasVsFinales(from, to)).thenReturn(data);

        // act
        byte[] bytes = service.ingresosSenasFinalesXlsx(from, to);

        // assert
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
        verify(ingresosReportService).ingresosSenasVsFinales(from, to);
    }

    @Test
    @DisplayName("ingresosSenasFinalesPdf genera PDF aun con valores 0")
    void ingresosSenasFinalesPdf_sinDatos() {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to   = LocalDate.of(2025, 12, 31);

        Map<String, Object> data = Map.of(
                "labels", List.of(),
                "senas", List.of(),
                "finales", List.of(),
                "totalSena", BigDecimal.ZERO,
                "totalFinal", BigDecimal.ZERO,
                "totalGeneral", BigDecimal.ZERO
        );

        when(ingresosReportService.ingresosSenasVsFinales(from, to)).thenReturn(data);

        // act
        byte[] bytes = service.ingresosSenasFinalesPdf(from, to);

        // assert
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
        verify(ingresosReportService).ingresosSenasVsFinales(from, to);
    }
}