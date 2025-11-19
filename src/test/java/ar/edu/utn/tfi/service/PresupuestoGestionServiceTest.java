// src/test/java/ar/edu/utn/tfi/service/PresupuestoGestionServiceTest.java
package ar.edu.utn.tfi.service;

import ar.edu.utn.tfi.domain.PagoManual;
import ar.edu.utn.tfi.domain.Presupuesto;
import ar.edu.utn.tfi.domain.PresupuestoItem;
import ar.edu.utn.tfi.domain.ServicioTarifa;
import ar.edu.utn.tfi.domain.SolicitudPresupuesto;
import ar.edu.utn.tfi.repository.PagoManualRepository;
import ar.edu.utn.tfi.repository.PresupuestoItemRepository;
import ar.edu.utn.tfi.repository.PresupuestoRepository;
import ar.edu.utn.tfi.repository.ServicioTarifaRepository;
import ar.edu.utn.tfi.repository.SolicitudPresupuestoRepository;
import ar.edu.utn.tfi.service.Pagos.PaymentApiService;
import ar.edu.utn.tfi.web.dto.ExtraItemReq;
import ar.edu.utn.tfi.web.dto.PagoApiReq;
import ar.edu.utn.tfi.web.dto.PagoInfoDTO;
import ar.edu.utn.tfi.web.dto.PagoManualReq;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PresupuestoGestionServiceTest {

    @Mock SolicitudPresupuestoRepository solicitudRepo;
    @Mock ServicioTarifaRepository tarifaRepo;
    @Mock PresupuestoRepository presupuestoRepo;
    @Mock PresupuestoItemRepository itemRepo;
    @Mock MailService mailService;
    @Mock PaymentApiService paymentApiService;
    @Mock PagoManualRepository pagoManualRepo;
    @Mock OrdenRepuestoService ordenRepuestoService;
    @Mock NotificationService notificationService;

    @InjectMocks
    PresupuestoGestionService service;

    // ───────────────────────────── generarDesdeSolicitud ─────────────────────────────

    @Test
    @DisplayName("generarDesdeSolicitud: caso feliz con extras y notificación WA")
    void generarDesdeSolicitud_ok() {
        // 👉 Arrange (preparo datos de entrada y mocks)

        Long solicitudId = 1L;
        String vehiculoTipo = "auto";
        String piezaTipo = "MOTOR";
        List<String> servicios = List.of("RECTIFICADO", "ARMADO");

        // Mock de la solicitud
        SolicitudPresupuesto sol = new SolicitudPresupuesto();
        sol.setId(solicitudId);
        sol.setClienteNombre("Juan Pérez");
        sol.setClienteEmail("juan@test.com");
        sol.setTipoUnidad("MOTOR");
        sol.setMarca("Ford");
        sol.setModelo("Fiesta");

        when(solicitudRepo.findById(solicitudId)).thenReturn(Optional.of(sol));
        when(presupuestoRepo.existsBySolicitudId(solicitudId)).thenReturn(false);

        // Tarifas para los servicios
        ServicioTarifa t1 = new ServicioTarifa();
        t1.setNombreServicio("RECTIFICADO");
        t1.setPrecio(new BigDecimal("100000"));

        ServicioTarifa t2 = new ServicioTarifa();
        t2.setNombreServicio("ARMADO");
        t2.setPrecio(new BigDecimal("50000"));

        when(tarifaRepo.findByVehiculoTipoAndNombreServicioIn("AUTO", List.of("RECTIFICADO", "ARMADO")))
                .thenReturn(List.of(t1, t2));

        // Extra como mock para no depender del constructor del record
        ExtraItemReq extra = mock(ExtraItemReq.class);
        when(extra.nombre()).thenReturn("Aceites y selladores");
        when(extra.precio()).thenReturn(new BigDecimal("20000"));

        List<ExtraItemReq> extras = List.of(extra);

        // Cuando se guarda el presupuesto, le ponemos ID
        when(presupuestoRepo.save(any(Presupuesto.class)))
                .thenAnswer(inv -> {
                    Presupuesto p = inv.getArgument(0);
                    p.setId(10L);
                    return p;
                });

        // 👉 Act (ejecuto el método a testear)
        Presupuesto result = service.generarDesdeSolicitud(
                solicitudId,
                vehiculoTipo,
                piezaTipo,
                servicios,
                extras
        );

        // 👉 Assert (verifico resultados)

        // Total = 100000 + 50000 + 20000 = 170000
        assertThat(result.getTotal()).isEqualByComparingTo("170000");

        // Verifico que se haya guardado el presupuesto
        verify(presupuestoRepo).save(any(Presupuesto.class));

        // Verifico que se hayan guardado items:
        //   2 de tarifas + 1 extra = 3 llamadas a itemRepo.save
        verify(itemRepo, times(3)).save(any(PresupuestoItem.class));

        // Verifico que se disparó la notificación de presupuesto generado
        verify(notificationService).notificarPresupuestoGenerado(result, sol);
    }

    @Test
    @DisplayName("generarDesdeSolicitud: falla si hay servicios sin tarifa configurada")
    void generarDesdeSolicitud_servicioSinTarifa_lanzaError() {
        Long solicitudId = 1L;

        SolicitudPresupuesto sol = new SolicitudPresupuesto();
        sol.setId(solicitudId);
        sol.setClienteNombre("Cliente");
        sol.setClienteEmail("c@test.com");
        sol.setTipoUnidad("MOTOR");
        sol.setMarca("Ford");
        sol.setModelo("Fiesta");

        when(solicitudRepo.findById(solicitudId)).thenReturn(Optional.of(sol));
        when(presupuestoRepo.existsBySolicitudId(solicitudId)).thenReturn(false);

        // El front manda dos servicios, pero solo hay tarifa para uno
        List<String> servicios = List.of("RECTIFICADO", "ARMADO");
        ServicioTarifa t1 = new ServicioTarifa();
        t1.setNombreServicio("RECTIFICADO");
        t1.setPrecio(new BigDecimal("100000"));

        when(tarifaRepo.findByVehiculoTipoAndNombreServicioIn("AUTO", List.of("RECTIFICADO", "ARMADO")))
                .thenReturn(List.of(t1)); // solo uno encontrado

        // Act + Assert
        assertThatThrownBy(() -> service.generarDesdeSolicitud(
                solicitudId,
                "auto",
                "MOTOR",
                servicios,
                List.of()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Servicios sin tarifa");

        // No debe guardar presupuesto ni items ni notificar
        verify(presupuestoRepo, never()).save(any());
        verify(itemRepo, never()).save(any());
        verify(notificationService, never()).notificarPresupuestoGenerado(any(), any());
    }

    // ───────────────────────────── aprobar / rechazar ─────────────────────────────

    @Test
    @DisplayName("aprobar: pasa de PENDIENTE a APROBADO y notifica por WhatsApp")
    void aprobar_ok() {
        // Presupuesto existente PENDIENTE
        Presupuesto p = new Presupuesto();
        p.setId(5L);
        p.setEstado("PENDIENTE");

        when(presupuestoRepo.findById(5L)).thenReturn(Optional.of(p));
        when(presupuestoRepo.save(any(Presupuesto.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Presupuesto result = service.aprobar(5L, "adminUser", "Todo OK");

        assertThat(result.getEstado()).isEqualTo("APROBADO");
        assertThat(result.getDecisionUsuario()).isEqualTo("adminUser");
        assertThat(result.getDecisionMotivo()).isEqualTo("Todo OK");
        assertThat(result.getDecisionFecha()).isNotNull();

        verify(presupuestoRepo).save(p);
        verify(notificationService).notificarDecisionPresupuesto(p);
    }

    @Test
    @DisplayName("aprobar: lanza error si el presupuesto no está PENDIENTE")
    void aprobar_estadoInvalido() {
        Presupuesto p = new Presupuesto();
        p.setId(5L);
        p.setEstado("APROBADO"); // ya aprobado

        when(presupuestoRepo.findById(5L)).thenReturn(Optional.of(p));

        assertThrows(IllegalStateException.class,
                () -> service.aprobar(5L, "admin", "nota"));

        verify(presupuestoRepo, never()).save(any());
        verify(notificationService, never()).notificarDecisionPresupuesto(any());
    }

    @Test
    @DisplayName("rechazar: pasa de PENDIENTE a RECHAZADO y notifica por WhatsApp")
    void rechazar_ok() {
        Presupuesto p = new Presupuesto();
        p.setId(7L);
        p.setEstado("PENDIENTE");

        when(presupuestoRepo.findById(7L)).thenReturn(Optional.of(p));
        when(presupuestoRepo.save(any(Presupuesto.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Presupuesto result = service.rechazar(7L, "user", "Falta información");

        assertThat(result.getEstado()).isEqualTo("RECHAZADO");
        assertThat(result.getDecisionUsuario()).isEqualTo("user");
        assertThat(result.getDecisionMotivo()).isEqualTo("Falta información");
        assertThat(result.getDecisionFecha()).isNotNull();

        verify(notificationService).notificarDecisionPresupuesto(p);
    }

    // ───────────────────────────── cobrarSenaApi ─────────────────────────────

    @Test
    @DisplayName("cobrarSenaApi: crea pago en MP y marca seña como acreditada si status=approved")
    void cobrarSenaApi_ok() {
        // 👉 Presupuesto aprobado con total 100000
        Presupuesto p = new Presupuesto();
        p.setId(10L);
        p.setEstado("APROBADO");
        p.setTotal(new BigDecimal("100000"));

        when(presupuestoRepo.findById(10L)).thenReturn(Optional.of(p));

        // Simulamos respuesta de MP
        Map<String, Object> resp = Map.of(
                "status", "approved",
                "id", "PAY-123",
                "date_approved", OffsetDateTime.now().toString()
        );

        when(paymentApiService.crearPago(eq(10L), any(BigDecimal.class), any(PagoApiReq.class)))
                .thenReturn(resp);

        // Hacemos un mock de PagoApiReq para no depender del constructor real
        PagoApiReq req = mock(PagoApiReq.class);

        when(presupuestoRepo.save(any(Presupuesto.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Presupuesto result = service.cobrarSenaApi(10L, req);

        // 30% de 100000 = 30000
        assertThat(result.getSenaMonto()).isEqualByComparingTo("30000.00");
        assertThat(result.getSenaEstado()).isEqualTo("ACREDITADA");
        assertThat(result.getSenaPaymentId()).isEqualTo("PAY-123");
        assertThat(result.getSenaPaymentStatus()).isEqualTo("approved");
        assertThat(result.getSenaPaidAt()).isNotNull();

        verify(paymentApiService).crearPago(eq(10L), eq(new BigDecimal("30000.00")), eq(req));
        verify(presupuestoRepo).save(p);
    }

    @Test
    @DisplayName("cobrarSenaApi: falla si el presupuesto no está APROBADO")
    void cobrarSenaApi_estadoInvalido() {
        Presupuesto p = new Presupuesto();
        p.setId(10L);
        p.setEstado("PENDIENTE");
        p.setTotal(new BigDecimal("100000"));

        when(presupuestoRepo.findById(10L)).thenReturn(Optional.of(p));

        assertThrows(IllegalStateException.class,
                () -> service.cobrarSenaApi(10L, null));

        verify(paymentApiService, never()).crearPago(anyLong(), any(), any());
    }

    // ───────────────────────────── getPagoInfoPublico ─────────────────────────────

    @Test
    @DisplayName("getPagoInfoPublico: calcula seña sugerida, saldo y puedePagar")
    void getPagoInfoPublico_ok() {
        Presupuesto p = new Presupuesto();
        p.setId(20L);
        p.setEstado("APROBADO");
        p.setClienteEmail("cliente@test.com");
        p.setTotal(new BigDecimal("100000"));
        p.setSenaMonto(new BigDecimal("30000")); // ya pagada seña
        p.setFinalMonto(new BigDecimal("10000")); // pago final parcial

        when(presupuestoRepo.findById(20L)).thenReturn(Optional.of(p));

        PagoInfoDTO dto = service.getPagoInfoPublico(20L);

        // montoSena sugerido = 30% de total = 30000
        assertThat(dto.montoSena()).isEqualByComparingTo("30000.00");

        // saldo = 100000 - 30000 - 10000 = 60000
        assertThat(dto.saldoRestante()).isEqualByComparingTo("60000.00");

        assertThat(dto.puedePagar()).isTrue();
        assertThat(dto.presupuestoEstado()).isEqualTo("APROBADO");
        assertThat(dto.clienteEmail()).isEqualTo("cliente@test.com");
    }

    // ───────────────────────────── registrarPagoManual (SENA) ─────────────────────────────

    @Test
    @DisplayName("registrarPagoManual: registra seña manual válida y guarda PagoManual")
    void registrarPagoManual_sena_ok() {
        // 👉 Presupuesto ya APROBADO, sin seña aún
        Presupuesto p = new Presupuesto();
        p.setId(30L);
        p.setEstado("APROBADO");
        p.setTotal(new BigDecimal("100000"));

        when(presupuestoRepo.findById(30L)).thenReturn(Optional.of(p));
        when(presupuestoRepo.save(any(Presupuesto.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Mock de request para no depender del constructor del record
        PagoManualReq req = mock(PagoManualReq.class);
        when(req.presupuestoId()).thenReturn(30L);
        when(req.tipo()).thenReturn("SENA");
        when(req.monto()).thenReturn(new BigDecimal("30000.00")); // 30% exacto
        when(req.medio()).thenReturn("EFECTIVO");
        when(req.referencia()).thenReturn("RECIBO-001");
        when(req.fechaPago()).thenReturn(LocalDate.of(2025, 1, 1));
        when(req.nota()).thenReturn("Seña inicial");

        when(pagoManualRepo.save(any(PagoManual.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PagoManual pm = service.registrarPagoManual(req, "cajero1");

        // Verificamos estado del presupuesto
        assertThat(p.getSenaEstado()).isEqualTo("ACREDITADA");
        assertThat(p.getSenaMonto()).isEqualByComparingTo("30000.00");
        assertThat(p.getSenaPaymentStatus()).isEqualTo("manual");
        assertThat(p.getSenaPaymentId()).isEqualTo("RECIBO-001");
        assertThat(p.getSenaPaidAt()).isNotNull();

        // Verificamos datos del PagoManual
        assertThat(pm.getPresupuesto()).isEqualTo(p);
        assertThat(pm.getTipo()).isEqualTo("SENA");
        assertThat(pm.getMedio()).isEqualTo("EFECTIVO");
        assertThat(pm.getReferencia()).isEqualTo("RECIBO-001");
        assertThat(pm.getMonto()).isEqualByComparingTo("30000.00");
        assertThat(pm.getUsuario()).isEqualTo("cajero1");
        assertThat(pm.getNota()).isEqualTo("Seña inicial");

        verify(pagoManualRepo).save(any(PagoManual.class));
    }

    // ───────────────────────────── registrarPagoManual (FINAL) ─────────────────────────────

    @Test
    @DisplayName("registrarPagoManual: pago FINAL válido con repuestos y etapa LISTO_RETIRAR")
    void registrarPagoManual_final_ok() {
        // 👉 Presupuesto aprobado con seña ya acreditada
        Presupuesto p = new Presupuesto();
        p.setId(40L);
        p.setEstado("APROBADO");
        p.setTotal(new BigDecimal("100000"));       // servicios
        p.setSenaEstado("ACREDITADA");
        p.setSenaMonto(new BigDecimal("30000"));    // seña
        p.setOtNroOrden("OT-0001");

        when(presupuestoRepo.findById(40L)).thenReturn(Optional.of(p));

        // total repuestos = 20000
        when(ordenRepuestoService.calcularTotalRepuestosPorNroOrden("OT-0001"))
                .thenReturn(new BigDecimal("20000"));
        // etapa actual = LISTO_RETIRAR (permite pago final)
        when(ordenRepuestoService.getEtapaActualPorNroOrden("OT-0001"))
                .thenReturn("LISTO_RETIRAR");

        when(presupuestoRepo.save(any(Presupuesto.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Total real = 100000 + 20000 = 120000
        // saldo final esperado = 120000 - 30000 = 90000
        BigDecimal montoFinal = new BigDecimal("90000.00");

        PagoManualReq req = mock(PagoManualReq.class);
        when(req.presupuestoId()).thenReturn(40L);
        when(req.tipo()).thenReturn("FINAL");
        when(req.monto()).thenReturn(montoFinal);
        when(req.medio()).thenReturn("TRANSFERENCIA");
        when(req.referencia()).thenReturn("OP-999");
        when(req.fechaPago()).thenReturn(null); // usa LocalDateTime.now()
        when(req.nota()).thenReturn("Pago final");

        when(pagoManualRepo.save(any(PagoManual.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PagoManual pm = service.registrarPagoManual(req, "caja2");

        // Verificamos estado final
        assertThat(p.getFinalEstado()).isEqualTo("ACREDITADA");
        assertThat(p.getFinalMonto()).isEqualByComparingTo("90000.00");
        assertThat(p.getFinalPaymentStatus()).isEqualTo("manual");
        assertThat(p.getFinalPaymentId()).isEqualTo("OP-999");
        assertThat(p.getFinalPaidAt()).isNotNull();

        assertThat(pm.getTipo()).isEqualTo("FINAL");
        assertThat(pm.getMonto()).isEqualByComparingTo("90000.00");
        assertThat(pm.getReferencia()).isEqualTo("OP-999");
    }

    @Test
    @DisplayName("registrarPagoManual FINAL: falla si la OT no está en etapa LISTO_RETIRAR/ENTREGADO")
    void registrarPagoManual_final_etapaInvalida() {
        Presupuesto p = new Presupuesto();
        p.setId(50L);
        p.setEstado("APROBADO");
        p.setTotal(new BigDecimal("100000"));
        p.setSenaEstado("ACREDITADA");
        p.setSenaMonto(new BigDecimal("30000"));
        p.setOtNroOrden("OT-0002");

        when(presupuestoRepo.findById(50L)).thenReturn(Optional.of(p));

        // OT en etapa intermedia
        when(ordenRepuestoService.getEtapaActualPorNroOrden("OT-0002"))
                .thenReturn("EN_PROCESO");

        PagoManualReq req = mock(PagoManualReq.class);
        when(req.presupuestoId()).thenReturn(50L);
        when(req.tipo()).thenReturn("FINAL");
        when(req.monto()).thenReturn(new BigDecimal("70000"));
        when(req.medio()).thenReturn("EFECTIVO");
        when(req.referencia()).thenReturn("XXX");

        assertThatThrownBy(() -> service.registrarPagoManual(req, "user"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se puede registrar el pago FINAL");

        // No debe guardar cambios en presupuesto ni en pagos manuales
        verify(presupuestoRepo, never()).save(any());
        verify(pagoManualRepo, never()).save(any());
    }
}