// src/test/java/ar/edu/utn/tfi/service/CrearOrdenServiceTest.java
package ar.edu.utn.tfi.service;

import ar.edu.utn.tfi.domain.Cliente;
import ar.edu.utn.tfi.domain.OrdenEtapaHistorial;
import ar.edu.utn.tfi.domain.OrdenTrabajo;
import ar.edu.utn.tfi.domain.UnidadTrabajo;
import ar.edu.utn.tfi.repository.ClienteRepository;
import ar.edu.utn.tfi.repository.OrdenEtapaHistorialRepository;
import ar.edu.utn.tfi.repository.OrdenTrabajoRepository;
import ar.edu.utn.tfi.repository.UnidadTrabajoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrearOrdenServiceTest {

    @Mock
    ClienteRepository clienteRepo;

    @Mock
    UnidadTrabajoRepository unidadRepo;

    @Mock
    OrdenTrabajoRepository ordenRepo;

    @Mock
    OrdenEtapaHistorialRepository historialRepo;

    @Mock
    NotificationService notificationService;

    @InjectMocks
    CrearOrdenService service;

    @Test
    @DisplayName("crearOT crea cliente+unidad+orden y notifica ingreso (caso feliz)")
    void crearOT_ok_creaTodo() {
        // given
        var req = new CrearOrdenService.CreateOTReq(
                "Juan Perez",
                "+5493511111111",
                "MOTOR",
                "Ford",
                "Fiesta",
                "ABC123"
        );
        String usuario = "admin";

        // no existe cliente todavía
        when(clienteRepo.findByNombreAndTelefono(req.clienteNombre(), req.clienteTelefono()))
                .thenReturn(Optional.empty());

        // no existe unidad por nroMotor
        when(unidadRepo.findByNroMotor(req.nroMotor()))
                .thenReturn(Optional.empty());

        // simulo que la próxima OT es la número 6 -> OT-0006
        when(ordenRepo.count()).thenReturn(5L);

        // stub: al guardar cliente, le pongo un id
        when(clienteRepo.save(any(Cliente.class)))
                .thenAnswer(inv -> {
                    Cliente c = inv.getArgument(0);
                    c.setId(10L);
                    return c;
                });

        // stub: al guardar unidad, le pongo un id
        when(unidadRepo.save(any(UnidadTrabajo.class)))
                .thenAnswer(inv -> {
                    UnidadTrabajo u = inv.getArgument(0);
                    u.setId(20L);
                    return u;
                });

        // stub: al guardar orden, seteo id y devuelvo el mismo objeto
        when(ordenRepo.save(any(OrdenTrabajo.class)))
                .thenAnswer(inv -> {
                    OrdenTrabajo ot = inv.getArgument(0);
                    ot.setId(30L);
                    return ot;
                });

        // when
        var resp = service.crearOT(req, usuario);

        // then: respuesta correcta
        assertThat(resp.ordenId()).isEqualTo(30L);
        assertThat(resp.nroOrden()).isEqualTo("OT-0006");

        // cliente: busca y luego guarda nuevo
        verify(clienteRepo).findByNombreAndTelefono("Juan Perez", "+5493511111111");
        verify(clienteRepo).save(any(Cliente.class));

        // unidad: busca por motor y crea nueva
        verify(unidadRepo).findByNroMotor("ABC123");
        verify(unidadRepo).save(any(UnidadTrabajo.class));

        // orden creada en estado INGRESO con nro correcto
        ArgumentCaptor<OrdenTrabajo> otCap = ArgumentCaptor.forClass(OrdenTrabajo.class);
        verify(ordenRepo, atLeastOnce()).save(otCap.capture());
        OrdenTrabajo otGuardada = otCap.getValue();
        assertThat(otGuardada.getNroOrden()).isEqualTo("OT-0006");
        assertThat(otGuardada.getEstadoActual()).isEqualTo("INGRESO");
        assertThat(otGuardada.getUnidadId()).isEqualTo(20L);

        // historial de etapa inicial
        ArgumentCaptor<OrdenEtapaHistorial> histCap = ArgumentCaptor.forClass(OrdenEtapaHistorial.class);
        verify(historialRepo).save(histCap.capture());
        OrdenEtapaHistorial h = histCap.getValue();
        assertThat(h.getOrdenId()).isEqualTo(30L);
        assertThat(h.getEtapaCodigo()).isEqualTo("INGRESO");
        assertThat(h.getUsuario()).isEqualTo("admin");
        assertThat(h.getObservacion()).isEqualTo("Alta de orden");

        // notificación de ingreso enviada
        verify(notificationService).notificarIngresoOrden(any(OrdenTrabajo.class), any(Cliente.class));
    }

    @Test
    @DisplayName("crearOT reutiliza cliente existente y actualiza unidad sin cliente")
    void crearOT_reusaCliente_y_actualizaUnidad() {
        // given
        var req = new CrearOrdenService.CreateOTReq(
                "Cliente Existente",
                "123",
                "TAPA",
                "VW",
                "Gol",
                "MOTOR-1"
        );

        Cliente existente = new Cliente();
        existente.setId(1L);
        existente.setNombre("Cliente Existente");
        existente.setTelefono("123");

        UnidadTrabajo unidadExistente = new UnidadTrabajo();
        unidadExistente.setId(5L);
        unidadExistente.setCliente(null); // sin cliente asociado todavía

        when(clienteRepo.findByNombreAndTelefono("Cliente Existente", "123"))
                .thenReturn(Optional.of(existente));
        when(unidadRepo.findByNroMotor("MOTOR-1"))
                .thenReturn(Optional.of(unidadExistente));
        when(ordenRepo.count()).thenReturn(0L);
        when(ordenRepo.save(any(OrdenTrabajo.class)))
                .thenAnswer(inv -> {
                    OrdenTrabajo ot = inv.getArgument(0);
                    ot.setId(99L);
                    return ot;
                });
        when(unidadRepo.save(any(UnidadTrabajo.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // when
        var resp = service.crearOT(req, "user1");

        // then
        assertThat(resp.ordenId()).isEqualTo(99L);
        assertThat(resp.nroOrden()).isEqualTo("OT-0001");

        // no crea nuevo cliente
        verify(clienteRepo, never()).save(any(Cliente.class));

        // actualiza unidad para asociarle el cliente
        ArgumentCaptor<UnidadTrabajo> unidadCap = ArgumentCaptor.forClass(UnidadTrabajo.class);
        verify(unidadRepo).save(unidadCap.capture());
        assertThat(unidadCap.getValue().getCliente()).isEqualTo(existente);
    }

    @Test
    @DisplayName("crearOT lanza error si falta nombre de cliente")
    void crearOT_error_sinNombre() {
        var req = new CrearOrdenService.CreateOTReq(
                "  ",          // nombre vacío
                "123",
                "MOTOR",
                "Ford",
                "Fiesta",
                "ABC"
        );

        assertThrows(IllegalArgumentException.class,
                () -> service.crearOT(req, "admin"));

        verifyNoInteractions(clienteRepo, unidadRepo, ordenRepo, historialRepo, notificationService);
    }

    @Test
    @DisplayName("crearOT lanza error si tipo unidad inválido")
    void crearOT_error_tipoInvalido() {
        var req = new CrearOrdenService.CreateOTReq(
                "Juan",
                "123",
                "RUEDA",   // inválido
                "Ford",
                "Fiesta",
                "ABC"
        );

        assertThrows(IllegalArgumentException.class,
                () -> service.crearOT(req, "admin"));

        verifyNoInteractions(clienteRepo, unidadRepo, ordenRepo, historialRepo, notificationService);
    }
}