// src/test/java/ar/edu/utn/tfi/web/AdminSecurityGenericTest.java
package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.repository.FacturaMockRepository;
import ar.edu.utn.tfi.repository.PresupuestoRepository;
import ar.edu.utn.tfi.security.SecurityConfig;
import ar.edu.utn.tfi.service.PresupuestoGestionService;
import ar.edu.utn.tfi.service.PresupuestoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminPresupuestoController.class)
@Import(SecurityConfig.class)
@SuppressWarnings("removal")
class AdminSecurityGenericTest {

    @Autowired
    MockMvc mvc;

    // Dependencias del controller a mockear
    @MockBean PresupuestoService solicitudesService;
    @MockBean PresupuestoGestionService presupuestosService;
    @MockBean FacturaMockRepository facturaRepo;
    @MockBean PresupuestoRepository presupuestoRepo;

    @Test
    @DisplayName("GET /admin/presupuestos → 401 si no hay autenticación")
    void adminEndpoint_401_sinLogin() throws Exception {
        mvc.perform(get("/admin/presupuestos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /admin/presupuestos → 401 si las credenciales son inválidas")
    void adminEndpoint_401_credencialesInvalidas() throws Exception {
        mvc.perform(get("/admin/presupuestos")
                        .with(httpBasic("user", "user"))) // usuario que NO existe
                .andExpect(status().isUnauthorized());
    }
}