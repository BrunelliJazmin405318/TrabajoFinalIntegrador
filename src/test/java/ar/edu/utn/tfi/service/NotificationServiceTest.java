package ar.edu.utn.tfi.service;

import ar.edu.utn.tfi.domain.Notificacion;
import ar.edu.utn.tfi.domain.OrdenTrabajo;
import ar.edu.utn.tfi.domain.Presupuesto;
import ar.edu.utn.tfi.domain.SolicitudPresupuesto;
import ar.edu.utn.tfi.repository.NotificacionRepository;
import ar.edu.utn.tfi.repository.SolicitudPresupuestoRepository;
import ar.edu.utn.tfi.service.WhatsAppGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    NotificacionRepository repo;

    @Mock
    NotificationHub hub;

    @Mock
    WhatsAppGateway whatsapp;

    @Mock
    SolicitudPresupuestoRepository solicitudRepo;

    @Captor
    ArgumentCaptor<Notificacion> notificacionCaptor;

    NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(repo, hub, whatsapp, solicitudRepo);
    }

    @Test
    void notificarDecisionSolicitud_aprobada_enviaWhatsAppYGuardaNotificacion() {
        // arrange
        SolicitudPresupuesto s = new SolicitudPresupuesto();
        s.setId(15L);
        s.setClienteNombre("Wanda Nara");
        s.setClienteTelefono("+5493574000000");
        s.setEstado("APROBADO");
        s.setDecisionMotivo("Todo ok");

        // act
        service.notificarDecisionSolicitud(s);

        // mensaje que debe armar (igual que en NotificationService)
        String expectedMsg = "Hola " + s.getClienteNombre()
                + ", tu solicitud #" + s.getId() + " fue APROBADA. Motivo: " + s.getDecisionMotivo();

        // se envía WhatsApp
        verify(whatsapp).send(s.getClienteTelefono(), expectedMsg);

        // se persiste la notificación
        verify(repo).save(notificacionCaptor.capture());
        Notificacion n = notificacionCaptor.getValue();

        assertEquals(s.getId(), n.getSolicitudId());
        assertEquals("WHATSAPP", n.getCanal());
        assertEquals("SOLICITUD_APROBADO", n.getType());
        assertEquals("Estado de tu solicitud", n.getTitle());
        assertEquals(expectedMsg, n.getMessage());
        assertEquals(s.getClienteTelefono(), n.getClienteDestino());
        assertEquals("ENVIADA", n.getEstado());
    }

    @Test
    void notificarDecisionSolicitud_rechazada_enviaWhatsAppYGuardaNotificacion() {
        // arrange
        SolicitudPresupuesto s = new SolicitudPresupuesto();
        s.setId(20L);
        s.setClienteNombre("Carlos Pérez");
        s.setClienteTelefono("+5493574001111");
        s.setEstado("RECHAZADO");
        s.setDecisionMotivo("No trabajamos ese modelo");

        // act
        service.notificarDecisionSolicitud(s);

        String expectedMsg = "Hola " + s.getClienteNombre()
                + ", tu solicitud #" + s.getId() + " fue RECHAZADA. Motivo: " + s.getDecisionMotivo();

        verify(whatsapp).send(s.getClienteTelefono(), expectedMsg);

        verify(repo).save(notificacionCaptor.capture());
        Notificacion n = notificacionCaptor.getValue();

        assertEquals(s.getId(), n.getSolicitudId());
        assertEquals("WHATSAPP", n.getCanal());
        assertEquals("SOLICITUD_RECHAZADO", n.getType());
        assertEquals("Estado de tu solicitud", n.getTitle());
        assertEquals(expectedMsg, n.getMessage());
        assertEquals(s.getClienteTelefono(), n.getClienteDestino());
    }

    @Test
    void notificarPresupuestoGenerado_enviaWhatsAppYRegistraNotificacion() {
        // arrange
        Presupuesto p = new Presupuesto();
        p.setId(14L);
        p.setTotal(new BigDecimal("220000.00"));

        SolicitudPresupuesto s = new SolicitudPresupuesto();
        s.setId(15L);
        s.setClienteNombre("Wanda Nara");
        s.setClienteTelefono("+5493574408201");

        // act
        service.notificarPresupuestoGenerado(p, s);

        String linkSolicitud = "http://localhost:8080/public/presupuestos/solicitud/" + s.getId();
        String expectedMsg = "Hola " + s.getClienteNombre()
                + ", ya generamos tu presupuesto #" + p.getId()
                + " para la solicitud #" + s.getId()
                + ". Monto estimado: " + p.getTotal()
                + ". Podés verlo acá: " + linkSolicitud;

        verify(whatsapp).send(s.getClienteTelefono(), expectedMsg);

        verify(repo).save(notificacionCaptor.capture());
        Notificacion n = notificacionCaptor.getValue();

        assertEquals(s.getId(), n.getSolicitudId());
        assertEquals("WHATSAPP", n.getCanal());
        assertEquals("PRESUPUESTO_GENERADO", n.getType());
        assertEquals("Presupuesto generado", n.getTitle());
        assertEquals(expectedMsg, n.getMessage());
        assertEquals(s.getClienteTelefono(), n.getClienteDestino());
    }

    @Test
    void emitirListoRetirar_enviaWhatsApp_inAppYEventoSSE() {
        // arrange
        OrdenTrabajo ot = new OrdenTrabajo();
        ot.setId(3L);
        ot.setNroOrden("OT-0003");

        String telefono = "+5493574408201";

        // act
        service.emitirListoRetirar(ot, telefono);

        String expectedMsg = "Tu orden " + ot.getNroOrden() + " está lista para retirar.";

        // 1) WhatsApp
        verify(whatsapp).send(telefono, expectedMsg);

        // 2) Se guardan 2 notificaciones (IN_APP + WHATSAPP)
        verify(repo, times(2)).save(notificacionCaptor.capture());
        List<Notificacion> saves = notificacionCaptor.getAllValues();

        boolean tieneInApp = saves.stream()
                .anyMatch(n -> "IN_APP".equals(n.getCanal()) &&
                        "Orden lista para retirar".equals(n.getTitle()) &&
                        expectedMsg.equals(n.getMessage()));

        boolean tieneWhatsApp = saves.stream()
                .anyMatch(n -> "WHATSAPP".equals(n.getCanal()) &&
                        "Orden lista para retirar".equals(n.getTitle()) &&
                        expectedMsg.equals(n.getMessage()));

        assertTrue(tieneInApp, "Debe guardar una notificación IN_APP");
        assertTrue(tieneWhatsApp, "Debe guardar una notificación WHATSAPP");

        // 3) Evento SSE al hub
        verify(hub).push(eq(ot.getNroOrden()), eq("listo-retirar"), contains("LISTO_RETIRAR"));
    }
}