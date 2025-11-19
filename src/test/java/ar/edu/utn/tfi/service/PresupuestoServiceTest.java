package ar.edu.utn.tfi.service;

import ar.edu.utn.tfi.domain.Presupuesto;
import ar.edu.utn.tfi.domain.SolicitudPresupuesto;
import ar.edu.utn.tfi.repository.PresupuestoRepository;
import ar.edu.utn.tfi.repository.SolicitudPresupuestoRepository;
import ar.edu.utn.tfi.web.dto.SolicitudCreateDTO;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests de reglas de negocio para PresupuestoService.
 * - Crear solicitudes públicas
 * - Aprobar / rechazar solicitudes
 * - Listar solicitudes y presupuestos
 */
@ExtendWith(MockitoExtension.class)
class PresupuestoServiceTest {

    @Mock
    SolicitudPresupuestoRepository solicitudRepo;

    @Mock
    PresupuestoRepository presupuestoRepo;

    @Mock
    NotificationService notificationService;

    @Captor
    ArgumentCaptor<SolicitudPresupuesto> solicitudCaptor;

    PresupuestoService service;

    @BeforeEach
    void setUp() {
        service = new PresupuestoService(solicitudRepo, presupuestoRepo, notificationService);
    }

    // ─────────────────────────────────────────────
    // crearSolicitud()
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("crearSolicitud(): si el DTO es null → IllegalArgumentException")
    void crearSolicitud_null_throw() {
        assertThatThrownBy(() -> service.crearSolicitud(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no puede ser nula");
    }

    @Test
    @DisplayName("crearSolicitud(): si falta clienteNombre → IllegalArgumentException")
    void crearSolicitud_sinNombre_throw() {
        SolicitudCreateDTO dto = new SolicitudCreateDTO(
                "",          // clienteNombre
                "3510000000",
                "mail@test.com",
                "MOTOR",
                "Fiat",
                "Uno",
                "ABC123",
                "Cambio de aros",
                "COTIZACION"
        );

        assertThatThrownBy(() -> service.crearSolicitud(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("clienteNombre es obligatorio");
    }

    @Test
    @DisplayName("crearSolicitud(): si falta tipoUnidad → IllegalArgumentException")
    void crearSolicitud_sinTipoUnidad_throw() {
        SolicitudCreateDTO dto = new SolicitudCreateDTO(
                "Jaz",
                "3510000000",
                "mail@test.com",
                "",           // tipoUnidad vacío
                "Fiat",
                "Uno",
                "ABC123",
                "Cambio de aros",
                "COTIZACION"
        );

        assertThatThrownBy(() -> service.crearSolicitud(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tipoUnidad es obligatorio");
    }

    @Test
    @DisplayName("crearSolicitud(): guarda solicitud en PENDIENTE y tipoConsulta por defecto COTIZACION")
    void crearSolicitud_ok_defaultTipoConsulta() {
        // Arrange: DTO sin tipoConsulta (null) para probar el default
        SolicitudCreateDTO dto = new SolicitudCreateDTO(
                "  Jaz  ",
                "3510000000",
                "jaz@test.com",
                "motor",
                "Fiat",
                "Uno",
                "ABC123",
                "Rectificado completo",
                null           // tipoConsulta → debe quedar COTIZACION
        );

        // Simulamos que el repo devuelve la misma entidad que recibe
        when(solicitudRepo.save(any(SolicitudPresupuesto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, SolicitudPresupuesto.class));

        // Act
        SolicitudPresupuesto saved = service.crearSolicitud(dto);

        // Assert: verificamos lo que se mandó a guardar
        verify(solicitudRepo).save(solicitudCaptor.capture());
        SolicitudPresupuesto s = solicitudCaptor.getValue();

        // Nombre recortado
        assertThat(s.getClienteNombre()).isEqualTo("Jaz");
        assertThat(s.getClienteTelefono()).isEqualTo("3510000000");
        assertThat(s.getClienteEmail()).isEqualTo("jaz@test.com");

        // tipoUnidad en mayúsculas
        assertThat(s.getTipoUnidad()).isEqualTo("MOTOR");

        // Se copian los demás campos
        assertThat(s.getMarca()).isEqualTo("Fiat");
        assertThat(s.getModelo()).isEqualTo("Uno");
        assertThat(s.getNroMotor()).isEqualTo("ABC123");
        assertThat(s.getDescripcion()).isEqualTo("Rectificado completo");

        // Estado inicial y tipoConsulta por defecto
        assertThat(s.getEstado()).isEqualTo("PENDIENTE");
        assertThat(s.getTipoConsulta()).isEqualTo("COTIZACION");

        // Y el método devuelve lo mismo que se guardó
        assertThat(saved).isSameAs(s);
    }

    // ─────────────────────────────────────────────
    // getById()
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("getById(): si no existe lanza EntityNotFoundException")
    void getById_notFound_throw() {
        when(solicitudRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Solicitud no encontrada");
    }

    @Test
    @DisplayName("getById(): devuelve la solicitud si existe")
    void getById_ok() {
        SolicitudPresupuesto s = new SolicitudPresupuesto();
        s.setId(5L);
        when(solicitudRepo.findById(5L)).thenReturn(Optional.of(s));

        SolicitudPresupuesto result = service.getById(5L);

        assertThat(result).isSameAs(s);
    }

    // ─────────────────────────────────────────────
    // listar(estado) → solicitudes
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("listar(estado): sin estado → repo.findAllByOrderByCreadaEnDesc()")
    void listarSolicitudes_sinEstado() {
        service.listar(null);

        verify(solicitudRepo).findAllByOrderByCreadaEnDesc();
        verify(solicitudRepo, never()).findByEstadoOrderByCreadaEnDesc(any());
    }

    @Test
    @DisplayName("listar(estado): con estado → repo.findByEstadoOrderByCreadaEnDesc(upper)")
    void listarSolicitudes_conEstado() {
        service.listar("aprobado");

        verify(solicitudRepo).findByEstadoOrderByCreadaEnDesc("APROBADO");
        verify(solicitudRepo, never()).findAllByOrderByCreadaEnDesc();
    }

    // ─────────────────────────────────────────────
    // aprobar()
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("aprobar(): solo se puede si está PENDIENTE, si no → IllegalStateException")
    void aprobar_noPendiente_throw() {
        SolicitudPresupuesto s = new SolicitudPresupuesto();
        s.setId(10L);
        s.setEstado("APROBADO");

        when(solicitudRepo.findById(10L)).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> service.aprobar(10L, "admin", "ok"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Solo se puede aprobar si está PENDIENTE");
    }

    @Test
    @DisplayName("aprobar(): cambia estado a APROBADO, setea usuario/fecha/motivo y notifica por WhatsApp")
    void aprobar_ok() {
        // Arrange
        SolicitudPresupuesto s = new SolicitudPresupuesto();
        s.setId(10L);
        s.setEstado("PENDIENTE");

        when(solicitudRepo.findById(10L)).thenReturn(Optional.of(s));
        when(solicitudRepo.save(any(SolicitudPresupuesto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, SolicitudPresupuesto.class));

        // Act
        SolicitudPresupuesto result = service.aprobar(10L, "admin-user", "Todo correcto");

        // Assert: se guardó con los datos correctos
        verify(solicitudRepo).save(solicitudCaptor.capture());
        SolicitudPresupuesto saved = solicitudCaptor.getValue();

        assertThat(saved.getEstado()).isEqualTo("APROBADO");
        assertThat(saved.getDecisionUsuario()).isEqualTo("admin-user");
        assertThat(saved.getDecisionMotivo()).isEqualTo("Todo correcto");
        assertThat(saved.getDecisionFecha()).isNotNull();

        // Y notificación por WhatsApp
        verify(notificationService).notificarDecisionSolicitud(saved);

        // el método devuelve el mismo objeto que se guardó
        assertThat(result).isSameAs(saved);
    }

    // ─────────────────────────────────────────────
    // rechazar()
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("rechazar(): solo se puede si está PENDIENTE, si no → IllegalStateException")
    void rechazar_noPendiente_throw() {
        SolicitudPresupuesto s = new SolicitudPresupuesto();
        s.setId(10L);
        s.setEstado("APROBADO");

        when(solicitudRepo.findById(10L)).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> service.rechazar(10L, "admin", "No trabajamos ese modelo"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Solo se puede rechazar si está PENDIENTE");
    }

    @Test
    @DisplayName("rechazar(): cambia estado a RECHAZADO, setea usuario/fecha/motivo y notifica por WhatsApp")
    void rechazar_ok() {
        // Arrange
        SolicitudPresupuesto s = new SolicitudPresupuesto();
        s.setId(20L);
        s.setEstado("PENDIENTE");

        when(solicitudRepo.findById(20L)).thenReturn(Optional.of(s));
        when(solicitudRepo.save(any(SolicitudPresupuesto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, SolicitudPresupuesto.class));

        // Act
        SolicitudPresupuesto result = service.rechazar(20L, "admin", "No trabajamos ese modelo");

        // Assert
        verify(solicitudRepo).save(solicitudCaptor.capture());
        SolicitudPresupuesto saved = solicitudCaptor.getValue();

        assertThat(saved.getEstado()).isEqualTo("RECHAZADO");
        assertThat(saved.getDecisionUsuario()).isEqualTo("admin");
        assertThat(saved.getDecisionMotivo()).isEqualTo("No trabajamos ese modelo");
        assertThat(saved.getDecisionFecha()).isNotNull();

        verify(notificationService).notificarDecisionSolicitud(saved);
        assertThat(result).isSameAs(saved);
    }

    // ─────────────────────────────────────────────
    // listar(estado, solicitudId) → Presupuestos
    // ─────────────────────────────────────────────

    @Test
    @DisplayName("listar(estado, solicitudId): sin estado ni solicitud → findAllByOrderByCreadaEnDesc()")
    void listarPresupuestos_sinFiltros() {
        service.listar(null, null);

        verify(presupuestoRepo).findAllByOrderByCreadaEnDesc();
        verifyNoMoreInteractions(presupuestoRepo);
    }

    @Test
    @DisplayName("listar(estado, solicitudId): solo estado → findAllByEstadoOrderByCreadaEnDesc(estado)")
    void listarPresupuestos_soloEstado() {
        service.listar("APROBADO", null);

        verify(presupuestoRepo).findAllByEstadoOrderByCreadaEnDesc("APROBADO");
        verifyNoMoreInteractions(presupuestoRepo);
    }

    @Test
    @DisplayName("listar(estado, solicitudId): solo solicitudId → findAllBySolicitudIdOrderByCreadaEnDesc(id)")
    void listarPresupuestos_soloSolicitudId() {
        service.listar(null, 5L);

        verify(presupuestoRepo).findAllBySolicitudIdOrderByCreadaEnDesc(5L);
        verifyNoMoreInteractions(presupuestoRepo);
    }

    @Test
    @DisplayName("listar(estado, solicitudId): ambos → findAllByEstadoAndSolicitudIdOrderByCreadaEnDesc")
    void listarPresupuestos_estadoYSolicitudId() {
        service.listar("APROBADO", 5L);

        verify(presupuestoRepo).findAllByEstadoAndSolicitudIdOrderByCreadaEnDesc("APROBADO", 5L);
        verifyNoMoreInteractions(presupuestoRepo);
    }
}