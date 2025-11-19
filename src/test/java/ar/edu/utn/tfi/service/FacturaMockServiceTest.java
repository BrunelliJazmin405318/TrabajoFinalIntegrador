package ar.edu.utn.tfi.service;

import ar.edu.utn.tfi.domain.FacturaMock;
import ar.edu.utn.tfi.domain.Presupuesto;
import ar.edu.utn.tfi.domain.PresupuestoItem;
import ar.edu.utn.tfi.repository.FacturaMockRepository;
import ar.edu.utn.tfi.repository.PresupuestoItemRepository;
import ar.edu.utn.tfi.repository.PresupuestoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests de reglas de negocio de FacturaMockService.
 * No se levanta Spring ni BD: todo con Mockito.
 */
@ExtendWith(MockitoExtension.class)
class FacturaMockServiceTest {

    @Mock
    PresupuestoRepository presupuestoRepo;

    @Mock
    FacturaMockRepository facturaRepo;

    @Mock
    PresupuestoItemRepository itemRepo;

    @Mock
    MailService mailService;

    @Mock
    OrdenRepuestoService ordenRepuestoService;

    @InjectMocks
    FacturaMockService service;

    // ─────────────────────────────────────────────
    // 1) Caso feliz: generar factura tipo B
    // ─────────────────────────────────────────────
    @Test
    @DisplayName("generar(): crea factura B con total = servicios + repuestos y manda mail")
    void generar_ok_tipoB() {
        // ---------- Arrange ----------
        Long presupuestoId = 10L;

        // Presupuesto con FINAL acreditado y OT entregada
        Presupuesto p = new Presupuesto();
        p.setId(presupuestoId);
        p.setFinalEstado("ACREDITADA");
        p.setTotal(new BigDecimal("1000.00"));
        p.setClienteNombre("Cliente Demo");
        p.setClienteEmail("cliente@demo.test");
        p.setOtNroOrden("OT-0001");

        when(presupuestoRepo.findById(presupuestoId))
                .thenReturn(Optional.of(p));

        // OT en etapa ENTREGADO (regla nueva)
        when(ordenRepuestoService.getEtapaActualPorNroOrden("OT-0001"))
                .thenReturn("ENTREGADO");

        // No hay factura previa para este presupuesto
        when(facturaRepo.findByPresupuestoId(presupuestoId))
                .thenReturn(Optional.empty());

        // Cantidad actual de facturas (para armar número correlativo)
        when(facturaRepo.count()).thenReturn(0L); // → FB-000001

        // Total de repuestos
        when(ordenRepuestoService.totalPorNro("OT-0001"))
                .thenReturn(new BigDecimal("500.00"));

        // Cuando se guarda la factura, devolvemos el mismo objeto
        ArgumentCaptor<FacturaMock> facturaCaptor = ArgumentCaptor.forClass(FacturaMock.class);
        when(facturaRepo.save(any(FacturaMock.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, FacturaMock.class));

        // ---------- Act ----------
        FacturaMock f = service.generar(presupuestoId, "b"); // minúscula para probar normalización

        // ---------- Assert ----------
        // Tipo normalizado a B
        assertThat(f.getTipo()).isEqualTo("B");

        // Número con prefijo FB- y correlativo 000001
        assertThat(f.getNumero()).startsWith("FB-");
        assertThat(f.getNumero()).endsWith("000001");

        // Total = servicios (1000) + repuestos (500) = 1500
        assertThat(f.getTotal()).isEqualByComparingTo("1500.00");

        // Copia datos de cliente desde presupuesto
        assertThat(f.getClienteNombre()).isEqualTo("Cliente Demo");
        assertThat(f.getClienteEmail()).isEqualTo("cliente@demo.test");

        // Se asocia al presupuesto correcto
        assertThat(f.getPresupuesto()).isSameAs(p);

        // Se guardó la factura en el repo
        verify(facturaRepo).save(facturaCaptor.capture());
        FacturaMock guardada = facturaCaptor.getValue();
        assertThat(guardada.getNumero()).isEqualTo(f.getNumero());
        assertThat(guardada.getTipo()).isEqualTo("B");

        // Se envió el mail de factura emitida
        verify(mailService).enviarFacturaEmitida(f);
    }

    // ─────────────────────────────────────────────
    // 2) Errores de negocio en generar()
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("generar(): si el presupuesto no existe → EntityNotFoundException")
    void generar_presupuestoNoExiste_lanzaEntityNotFound() {
        when(presupuestoRepo.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generar(999L, "A"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("generar(): si el FINAL no está ACREDITADO → IllegalStateException")
    void generar_finalNoAcreditado_lanzaIllegalState() {
        Presupuesto p = new Presupuesto();
        p.setId(10L);
        p.setFinalEstado("PENDIENTE");

        when(presupuestoRepo.findById(10L)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.generar(10L, "A"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FINAL está acreditado");
    }

    @Test
    @DisplayName("generar(): si la OT existe pero no está ENTREGADO → IllegalStateException")
    void generar_otNoEntregada_lanzaIllegalState() {
        Presupuesto p = new Presupuesto();
        p.setId(10L);
        p.setFinalEstado("ACREDITADA");
        p.setOtNroOrden("OT-0001");

        when(presupuestoRepo.findById(10L)).thenReturn(Optional.of(p));
        when(ordenRepuestoService.getEtapaActualPorNroOrden("OT-0001"))
                .thenReturn("TRABAJO"); // no ENTREGADO

        assertThatThrownBy(() -> service.generar(10L, "B"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ENTREGADO");
    }

    @Test
    @DisplayName("generar(): si ya hay factura para ese presupuesto → IllegalStateException")
    void generar_facturaYaExiste_lanzaIllegalState() {
        Presupuesto p = new Presupuesto();
        p.setId(10L);
        p.setFinalEstado("ACREDITADA");

        when(presupuestoRepo.findById(10L)).thenReturn(Optional.of(p));
        when(facturaRepo.findByPresupuestoId(10L))
                .thenReturn(Optional.of(new FacturaMock()));

        assertThatThrownBy(() -> service.generar(10L, "B"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ya existe una factura");
    }

    @Test
    @DisplayName("generar(): tipo inválido → IllegalArgumentException")
    void generar_tipoInvalido_lanzaIllegalArgument() {
        Presupuesto p = new Presupuesto();
        p.setId(10L);
        p.setFinalEstado("ACREDITADA");

        when(presupuestoRepo.findById(10L)).thenReturn(Optional.of(p));
        when(facturaRepo.findByPresupuestoId(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generar(10L, "Z"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tipo de factura inválido");
    }

    // ─────────────────────────────────────────────
    // 3) getOrCreateByPresupuesto()
    // ─────────────────────────────────────────────
    @Test
    @DisplayName("getOrCreateByPresupuesto(): si ya existe factura, NO llama a generar()")
    void getOrCreate_devuelveExistente() {
        Long presupuestoId = 10L;
        FacturaMock existente = new FacturaMock();
        existente.setId(1L);
        existente.setNumero("FB-000001");

        when(facturaRepo.findByPresupuestoId(presupuestoId))
                .thenReturn(Optional.of(existente));

        FacturaMock result = service.getOrCreateByPresupuesto(presupuestoId, "B");

        assertThat(result).isSameAs(existente);

        // No debería haber conteo ni generar nueva
        verify(facturaRepo, never()).count();
    }

    // ─────────────────────────────────────────────
    // 4) renderPdfByPresupuesto(): PDF no vacío
    // ─────────────────────────────────────────────
    @Test
    @DisplayName("renderPdfByPresupuesto(): genera un PDF no vacío para la factura existente")
    void renderPdfByPresupuesto_devuelveBytes() {
        // ---------- Arrange ----------
        Long presupuestoId = 10L;

        Presupuesto p = new Presupuesto();
        p.setId(presupuestoId);
        p.setTotal(new BigDecimal("1000.00"));
        p.setClienteNombre("Cliente Demo");
        p.setClienteEmail("cliente@demo.test");
        p.setOtNroOrden(null); // así evitamos que consulte repuestos en el PDF

        FacturaMock f = new FacturaMock();
        f.setId(1L);
        f.setTipo("B");
        f.setNumero("FB-000001");
        f.setTotal(new BigDecimal("1000.00"));
        f.setPresupuesto(p);

        // Ya existe factura para ese presupuesto
        when(facturaRepo.findByPresupuestoId(presupuestoId))
                .thenReturn(Optional.of(f));

        // Items del presupuesto
        PresupuestoItem it = new PresupuestoItem();
        it.setServicioNombre("Rectificado de motor");
        it.setPrecioUnitario(new BigDecimal("1000.00"));
        it.setPresupuesto(p);

        when(itemRepo.findByPresupuestoId(presupuestoId))
                .thenReturn(List.of(it));

        // ---------- Act ----------
        byte[] pdf = service.renderPdfByPresupuesto(presupuestoId, "B");

        // ---------- Assert ----------
        assertThat(pdf).isNotNull();
        assertThat(pdf.length).isGreaterThan(0);

        // (Opcional) Podríamos hacer asserts más finos sobre contenido,
        // pero con que no explote y genere algo, ya cubrimos el flujo feliz del PDF.
    }
}