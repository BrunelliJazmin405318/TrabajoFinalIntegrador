package ar.edu.utn.tfi.infra;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public class DatabaseSmokeTest {
    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("tfi_test")
                    .withUsername("tfi")
                    .withPassword("tfi");

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    JdbcTemplate jdbc;

    @Test
    @DisplayName("Flyway aplicado y tablas creadas")
    void flyway_ok() {
        Integer applied = jdbc.queryForObject(
                "select count(*) from flyway_schema_history", Integer.class);
        assertThat(applied).isGreaterThanOrEqualTo(1);

        Integer tabCliente = jdbc.queryForObject(
                "select count(*) from information_schema.tables where table_name = 'cliente'", Integer.class);
        assertThat(tabCliente).isEqualTo(1);
    }

}
