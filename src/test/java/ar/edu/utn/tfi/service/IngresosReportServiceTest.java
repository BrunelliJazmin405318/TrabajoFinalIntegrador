// src/test/java/ar/edu/utn/tfi/service/IngresosReportServiceTest.java
package ar.edu.utn.tfi.service;

import ar.edu.utn.tfi.repository.IngresosReportRepository;
import ar.edu.utn.tfi.repository.IngresosReportRepository.IngresoMesTipo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngresosReportServiceTest {

    @Mock
    IngresosReportRepository repo;

    IngresosReportService service;

    @BeforeEach
    void setUp() {
        service = new IngresosReportService(repo);
    }

    // Pequeño helper para crear mocks de la proyección IngresoMesTipo
    private IngresoMesTipo ingreso(String mes, String tipo, String total) {
        return new IngresoMesTipo() {
            @Override public String getMes() { return mes; }
            @Override public String getTipo() { return tipo; }
            @Override public BigDecimal getTotal() { return new BigDecimal(total); }
        };
    }

    @Test
    @DisplayName("ingresosSenasVsFinales arma eje de meses continuo y suma correctamente SENA/FINAL")
    void ingresosSenasVsFinales_ok() {
        // from → to: 2025-01 a 2025-03 → meses: [2025-01, 2025-02, 2025-03]
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to   = LocalDate.of(2025, 3, 31);

        // Datos que simularía devolver el repositorio
        List<IngresoMesTipo> rows = List.of(
                ingreso("2025-01", "SENA",  "10000.00"),
                ingreso("2025-01", "FINAL", "5000.00"),
                ingreso("2025-03", "SENA",  "7000.00")
        );

        // no nos importa tanto el rango exacto, por eso usamos any()
        when(repo.listarIngresos(any(), any())).thenReturn(rows);

        // act
        Map<String, Object> out = service.ingresosSenasVsFinales(from, to);

        // assert
        @SuppressWarnings("unchecked")
        List<String> labels = (List<String>) out.get("labels");
        assertEquals(List.of("2025-01", "2025-02", "2025-03"), labels);

        @SuppressWarnings("unchecked")
        List<BigDecimal> senas = (List<BigDecimal>) out.get("senas");
        @SuppressWarnings("unchecked")
        List<BigDecimal> finales = (List<BigDecimal>) out.get("finales");

        // Sumas por mes
        assertEquals(new BigDecimal("10000.00"), senas.get(0)); // ene SENA
        assertEquals(BigDecimal.ZERO, senas.get(1));            // feb SENA
        assertEquals(new BigDecimal("7000.00"), senas.get(2));  // mar SENA

        assertEquals(new BigDecimal("5000.00"), finales.get(0)); // ene FINAL
        assertEquals(BigDecimal.ZERO, finales.get(1));           // feb FINAL
        assertEquals(BigDecimal.ZERO, finales.get(2));           // mar FINAL

        // Totales
        BigDecimal totalSena    = (BigDecimal) out.get("totalSena");
        BigDecimal totalFinal   = (BigDecimal) out.get("totalFinal");
        BigDecimal totalGeneral = (BigDecimal) out.get("totalGeneral");

        assertEquals(new BigDecimal("17000.00"), totalSena);         // 10000 + 7000
        assertEquals(new BigDecimal("5000.00"), totalFinal);         // 5000
        assertEquals(new BigDecimal("22000.00"), totalGeneral);      // 17000 + 5000

        // También podés chequear que respete el from/to string
        assertEquals("2025-01-01", out.get("from"));
        assertEquals("2025-03-31", out.get("to"));

        // Y que se haya llamado al repo
        verify(repo).listarIngresos(any(), any());
    }

    @Test
    @DisplayName("ingresosSenasVsFinales corrige si to < from y devuelve meses en orden")
    void ingresosSenasVsFinales_intercambiaFechasSiVienenInvertidas() {
        // Pasamos from > to a propósito
        LocalDate from = LocalDate.of(2025, 5, 31);
        LocalDate to   = LocalDate.of(2025, 3, 1);

        when(repo.listarIngresos(any(), any())).thenReturn(List.of());

        Map<String, Object> out = service.ingresosSenasVsFinales(from, to);

        @SuppressWarnings("unchecked")
        List<String> labels = (List<String>) out.get("labels");

        // Debe ordenarlos de menor a mayor mes: 2025-03, 2025-04, 2025-05
        assertEquals(List.of("2025-03", "2025-04", "2025-05"), labels);

        // Totales en cero porque la lista venía vacía
        assertEquals(BigDecimal.ZERO, out.get("totalSena"));
        assertEquals(BigDecimal.ZERO, out.get("totalFinal"));
        assertEquals(BigDecimal.ZERO, out.get("totalGeneral"));
    }
}