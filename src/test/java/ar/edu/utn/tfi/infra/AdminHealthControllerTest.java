package ar.edu.utn.tfi.infra;

import ar.edu.utn.tfi.security.SecurityConfig;
import ar.edu.utn.tfi.web.AdminHealthController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminHealthController.class)
@Import(SecurityConfig.class)

public class AdminHealthControllerTest {
    @Autowired
    MockMvc mvc;

    @Test
    @DisplayName("/admin/health debe pedir login (401 si no envío credenciales)")
    void requiere_auth() throws Exception {
        mvc.perform(get("/admin/health"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("/admin/health responde 200 con admin/admin")
    void ok_con_admin() throws Exception {
        mvc.perform(get("/admin/health").with(httpBasic("admin","admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }
}
