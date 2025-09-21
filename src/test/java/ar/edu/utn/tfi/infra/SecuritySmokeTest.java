package ar.edu.utn.tfi.infra;

import ar.edu.utn.tfi.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import({SecurityConfig.class, SecuritySmokeTest.TestEndpoints.class})
@TestPropertySource(properties = {
        // Evitamos levantar DataSource/JPA/Flyway para este test
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
public class SecuritySmokeTest {

    @Autowired
    MockMvc mvc;

    @Test
    @DisplayName("PUBLIC: entra sin login")
    void public_ok() throws Exception {
        mvc.perform(get("/public/ping").accept(MediaType.TEXT_PLAIN))
                .andExpect(status().isOk())
                .andExpect(content().string("ok-public"));
    }

    @Test
    @DisplayName("ADMIN: con admin/admin => 200")
    void admin_200_con_login() throws Exception {
        mvc.perform(get("/admin/ping")
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(content().string("ok-admin"));
    }

    /** Endpoints de PRUEBA solo para este test (no dependen de BD). */
    @TestConfiguration
    static class TestEndpoints {
        @Bean
        TestController testController() { return new TestController(); }
    }

    @RestController
    static class TestController {
        @GetMapping("/public/ping") String pub() { return "ok-public"; }
        @GetMapping("/admin/ping")  String adm() { return "ok-admin"; }
    }
}

