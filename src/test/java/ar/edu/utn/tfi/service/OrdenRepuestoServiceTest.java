// src/test/java/ar/edu/utn/tfi/service/OrdenRepuestoServiceTest.java
package ar.edu.utn.tfi.service;

import ar.edu.utn.tfi.domain.OrdenRepuesto;
import ar.edu.utn.tfi.domain.OrdenTrabajo;
import ar.edu.utn.tfi.repository.OrdenRepuestoRepository;
import ar.edu.utn.tfi.repository.OrdenTrabajoRepository;
import ar.edu.utn.tfi.web.dto.RepuestoCreateReq;
import ar.edu.utn.tfi.web.dto.RepuestoDTO;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios de OrdenRepuestoService.
 * Usamos Mockito para mockear los repositorios y NO levantar Spring.
 */
@ExtendWith(MockitoExtension.class)
class OrdenRepuestoServiceTest {

    @Mock
    OrdenTrabajoRepository ordenRepo;

    @Mock
    OrdenRepuestoRepository repuestoRepo;

    @Captor
    ArgumentCaptor<OrdenRepuesto> repuestoCaptor;

    OrdenRepuestoService service;

    @BeforeEach
    void setUp() {
        // Construimos el service con los mocks
        service = new OrdenRepuestoService(ordenRepo, repuestoRepo);
    }

    @Test
    @DisplayName("listarPorNro devuelve los repuestos de la orden en forma de DTO")
    void listarPorNro_ok() {
        // arrange
        String nroOrden = "OT-0001";
        OrdenTrabajo ot = new OrdenTrabajo();
        ot.setId(10L);
        ot.setNroOrden(nroOrden);

        OrdenRepuesto r1 = new OrdenRepuesto();
        r1.setId(1L);
        r1.setOrdenId(10L);
        r1.setDescripcion("Bujía");
        r1.setCantidad(new BigDecimal("2"));
        r1.setPrecioUnit(new BigDecimal("1500"));

        OrdenRepuesto r2 = new OrdenRepuesto();
        r2.setId(2L);
        r2.setOrdenId(10L);
        r2.setDescripcion("Junta tapa");
        r2.setCantidad(BigDecimal.ONE);
        r2.setPrecioUnit(new BigDecimal("8000"));

        when(ordenRepo.findByNroOrden(nroOrden)).thenReturn(Optional.of(ot));
        when(repuestoRepo.findByOrdenIdOrderByIdAsc(10L)).thenReturn(List.of(r1, r2));

        // act
        List<RepuestoDTO> dtos = service.listarPorNro(nroOrden);

        // assert
        assertEquals(2, dtos.size());
        assertEquals("Bujía", dtos.get(0).descripcion());
        assertEquals(new BigDecimal("2"), dtos.get(0).cantidad());
        assertEquals("Junta tapa", dtos.get(1).descripcion());
    }

    @Test
    @DisplayName("listarPorNro lanza EntityNotFound si la OT no existe")
    void listarPorNro_ordenNoExiste() {
        // arrange
        when(ordenRepo.findByNroOrden("OT-9999")).thenReturn(Optional.empty());

        // act + assert
        assertThrows(EntityNotFoundException.class,
                () -> service.listarPorNro("OT-9999"));
    }

    @Test
    @DisplayName("agregar crea un repuesto cuando la etapa permite repuestos")
    void agregar_ok_enEtapaValida() {
        // arrange
        String nroOrden = "OT-0002";
        OrdenTrabajo ot = new OrdenTrabajo();
        ot.setId(20L);
        // Etapa válida para repuestos (DEBE estar en el set ETAPAS_PERMITEN_REPUESTOS)
        ot.setEstadoActual("DIAGNOSTICO");

        RepuestoCreateReq req = new RepuestoCreateReq(
                "Kit de juntas",
                new BigDecimal("3"),
                new BigDecimal("5000")
        );

        when(ordenRepo.findByNroOrden(nroOrden)).thenReturn(Optional.of(ot));
        // simulamos que el repo guarda y devuelve la entidad con id
        when(repuestoRepo.save(any(OrdenRepuesto.class))).thenAnswer(inv -> {
            OrdenRepuesto r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        // act
        RepuestoDTO dto = service.agregar(nroOrden, req, "adminUser");

        // assert DTO
        assertNotNull(dto);
        assertEquals("Kit de juntas", dto.descripcion());
        assertEquals(new BigDecimal("3"), dto.cantidad());
        assertEquals(new BigDecimal("5000"), dto.precioUnit());

        // assert entidad guardada (capturamos el objeto que se mandó al repo)
        verify(repuestoRepo).save(repuestoCaptor.capture());
        OrdenRepuesto saved = repuestoCaptor.getValue();

        assertEquals(20L, saved.getOrdenId());
        assertEquals("Kit de juntas", saved.getDescripcion());
        assertEquals(new BigDecimal("3"), saved.getCantidad());
        assertEquals(new BigDecimal("5000"), saved.getPrecioUnit());
        // subtotal = precio * cantidad
        assertEquals(new BigDecimal("15000"), saved.getSubtotal());
        assertEquals("adminUser", saved.getCreatedBy());
    }

    @Test
    @DisplayName("agregar lanza IllegalStateException si la etapa NO permite repuestos")
    void agregar_enEtapaNoPermitida_lanzaError() {
        // arrange
        String nroOrden = "OT-0003";
        OrdenTrabajo ot = new OrdenTrabajo();
        ot.setId(30L);
        // Etapa no permitida (por ejemplo ENTREGADO)
        ot.setEstadoActual("ENTREGADO");

        RepuestoCreateReq req = new RepuestoCreateReq(
                "Filtro de aceite",
                BigDecimal.ONE,
                new BigDecimal("2500")
        );

        when(ordenRepo.findByNroOrden(nroOrden)).thenReturn(Optional.of(ot));

        // act + assert
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.agregar(nroOrden, req, "admin"));

        assertTrue(ex.getMessage().contains("No se pueden gestionar repuestos en la etapa actual"));
        // no debe guardar nada
        verify(repuestoRepo, never()).save(any());
    }

    @Test
    @DisplayName("agregar valida descripción, cantidad y precio")
    void agregar_validacionesBasicas() {
        String nroOrden = "OT-0004";

        // 1) Descripción vacía
        RepuestoCreateReq sinDesc = new RepuestoCreateReq(
                "   ",
                BigDecimal.ONE,
                new BigDecimal("1000")
        );
        assertThrows(IllegalArgumentException.class,
                () -> service.agregar(nroOrden, sinDesc, "admin"));

        // 2) Cantidad <= 0
        RepuestoCreateReq cantCero = new RepuestoCreateReq(
                "Algo",
                new BigDecimal("0"),
                new BigDecimal("1000")
        );
        assertThrows(IllegalArgumentException.class,
                () -> service.agregar(nroOrden, cantCero, "admin"));

        // 3) Precio <= 0
        RepuestoCreateReq precioCero = new RepuestoCreateReq(
                "Algo",
                BigDecimal.ONE,
                new BigDecimal("0")
        );
        assertThrows(IllegalArgumentException.class,
                () -> service.agregar(nroOrden, precioCero, "admin"));
    }

    @Test
    @DisplayName("eliminar borra el repuesto si pertenece a la OT y la etapa es válida")
    void eliminar_ok() {
        // arrange
        String nroOrden = "OT-0005";
        OrdenTrabajo ot = new OrdenTrabajo();
        ot.setId(50L);
        ot.setEstadoActual("MAQUINADO"); // etapa válida

        OrdenRepuesto r = new OrdenRepuesto();
        r.setId(5L);
        r.setOrdenId(50L);

        when(ordenRepo.findByNroOrden(nroOrden)).thenReturn(Optional.of(ot));
        when(repuestoRepo.findById(5L)).thenReturn(Optional.of(r));

        // act
        service.eliminar(nroOrden, 5L, "admin");

        // assert
        verify(repuestoRepo).delete(r);
    }

    @Test
    @DisplayName("eliminar lanza IllegalArgumentException si el repuesto NO pertenece a la OT")
    void eliminar_repuestoDeOtraOrden_lanzaError() {
        // arrange
        String nroOrden = "OT-0006";
        OrdenTrabajo ot = new OrdenTrabajo();
        ot.setId(60L);
        ot.setEstadoActual("SEMI_ARMADO"); // válida

        OrdenRepuesto r = new OrdenRepuesto();
        r.setId(6L);
        r.setOrdenId(999L); // otra orden

        when(ordenRepo.findByNroOrden(nroOrden)).thenReturn(Optional.of(ot));
        when(repuestoRepo.findById(6L)).thenReturn(Optional.of(r));

        // act + assert
        assertThrows(IllegalArgumentException.class,
                () -> service.eliminar(nroOrden, 6L, "admin"));

        verify(repuestoRepo, never()).delete(any());
    }

    @Test
    @DisplayName("totalPorNro suma precio * cantidad de todos los repuestos de la OT")
    void totalPorNro_ok() {
        // arrange
        String nroOrden = "OT-0007";
        OrdenTrabajo ot = new OrdenTrabajo();
        ot.setId(70L);

        OrdenRepuesto r1 = new OrdenRepuesto();
        r1.setOrdenId(70L);
        r1.setCantidad(new BigDecimal("2"));
        r1.setPrecioUnit(new BigDecimal("1000")); // subtotal 2000

        OrdenRepuesto r2 = new OrdenRepuesto();
        r2.setOrdenId(70L);
        r2.setCantidad(new BigDecimal("3"));
        r2.setPrecioUnit(new BigDecimal("500")); // subtotal 1500

        when(ordenRepo.findByNroOrden(nroOrden)).thenReturn(Optional.of(ot));
        when(repuestoRepo.findByOrdenIdOrderByIdAsc(70L)).thenReturn(List.of(r1, r2));

        // act
        BigDecimal total = service.totalPorNro(nroOrden);

        // assert: 2000 + 1500 = 3500
        assertEquals(new BigDecimal("3500"), total);
    }

    @Test
    @DisplayName("calcularTotalRepuestosPorNroOrden usa cantidad=1 y precio=0 cuando hay nulls")
    void calcularTotalRepuestosPorNroOrden_manejaNulls() {
        // arrange
        String nroOrden = "OT-0008";
        OrdenTrabajo ot = new OrdenTrabajo();
        ot.setId(80L);

        OrdenRepuesto r1 = new OrdenRepuesto();
        r1.setOrdenId(80L);
        r1.setCantidad(null); // usa 1
        r1.setPrecioUnit(new BigDecimal("1000")); // subtotal 1000

        OrdenRepuesto r2 = new OrdenRepuesto();
        r2.setOrdenId(80L);
        r2.setCantidad(new BigDecimal("2"));
        r2.setPrecioUnit(null); // precio 0, subtotal 0

        when(ordenRepo.findByNroOrden(nroOrden)).thenReturn(Optional.of(ot));
        when(repuestoRepo.findByOrdenIdOrderByIdAsc(80L)).thenReturn(List.of(r1, r2));

        // act
        BigDecimal total = service.calcularTotalRepuestosPorNroOrden(nroOrden);

        // assert: 1000 + 0 = 1000
        assertEquals(new BigDecimal("1000"), total);
    }

    @Test
    @DisplayName("getEtapaActualPorNroOrden devuelve el estado_actual de la OT")
    void getEtapaActualPorNroOrden_ok() {
        // arrange
        String nroOrden = "OT-0009";
        OrdenTrabajo ot = new OrdenTrabajo();
        ot.setId(90L);
        ot.setEstadoActual("MAQUINADO");

        when(ordenRepo.findByNroOrden(nroOrden)).thenReturn(Optional.of(ot));

        // act
        String etapa = service.getEtapaActualPorNroOrden(nroOrden);

        // assert
        assertEquals("MAQUINADO", etapa);
    }
}