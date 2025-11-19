// src/test/java/ar/edu/utn/tfi/service/AuditoriaServiceTest.java
package ar.edu.utn.tfi.service;

import ar.edu.utn.tfi.domain.AuditoriaCambio;
import ar.edu.utn.tfi.domain.OrdenTrabajo;
import ar.edu.utn.tfi.repository.AuditoriaCambioRepository;
import ar.edu.utn.tfi.repository.OrdenTrabajoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditoriaServiceTest {

    @Mock
    AuditoriaCambioRepository repo;

    @Mock
    OrdenTrabajoRepository ordenRepo;

    @Captor
    ArgumentCaptor<AuditoriaCambio> auditoriaCaptor;

    AuditoriaService service;

    @BeforeEach
    void setUp() {
        service = new AuditoriaService(repo, ordenRepo);
    }

    @Test
    @DisplayName("registrarCambio guarda un registro de auditoría con los datos correctos")
    void registrarCambio_guardaAuditoria() {
        Long ordenId = 10L;
        String campo = "estado_actual";
        String anterior = "INGRESO";
        String nuevo = "DIAGNOSTICO";
        String usuario = "admin";

        // act
        service.registrarCambio(ordenId, campo, anterior, nuevo, usuario);

        // assert: capturamos el AuditoriaCambio que se guardó
        verify(repo).saveAndFlush(auditoriaCaptor.capture());
        AuditoriaCambio a = auditoriaCaptor.getValue();

        assertEquals(ordenId, a.getOrdenId());
        assertEquals(campo, a.getCampo());
        assertEquals(anterior, a.getValorAnterior());
        assertEquals(nuevo, a.getValorNuevo());
        assertEquals(usuario, a.getUsuario());
    }

    @Test
    @DisplayName("listarPorNro devuelve la lista de cambios de la OT")
    void listarPorNro_ok() {
        String nro = "OT-0005";

        OrdenTrabajo ot = new OrdenTrabajo();
        ot.setId(5L);
        ot.setNroOrden(nro);

        List<AuditoriaCambio> cambios = List.of(
                new AuditoriaCambio(),
                new AuditoriaCambio()
        );

        when(ordenRepo.findByNroOrden(nro)).thenReturn(java.util.Optional.of(ot));
        when(repo.findByOrdenIdOrderByFechaDesc(5L)).thenReturn(cambios);

        // act
        List<AuditoriaCambio> result = service.listarPorNro(nro);

        // assert
        assertEquals(cambios, result);
        verify(ordenRepo).findByNroOrden(nro);
        verify(repo).findByOrdenIdOrderByFechaDesc(5L);
    }

    @Test
    @DisplayName("listarPorNro lanza EntityNotFound si la OT no existe")
    void listarPorNro_ordenNoEncontrada() {
        String nro = "OT-9999";

        when(ordenRepo.findByNroOrden(nro)).thenReturn(java.util.Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> service.listarPorNro(nro));

        verify(ordenRepo).findByNroOrden(nro);
        verifyNoInteractions(repo); // nunca debería consultar auditoría si no existe la OT
    }
}