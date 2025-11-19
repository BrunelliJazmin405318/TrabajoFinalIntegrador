package ar.edu.utn.tfi.infra;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest

class DatabaseSmokeTest extends PostgresTestBase {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void flyway_ok() {
        Integer applied = jdbc.queryForObject(
                "select count(*) from flyway_schema_history", Integer.class);
        assertThat(applied).isGreaterThanOrEqualTo(1);

        Integer tabCliente = jdbc.queryForObject(
                "select count(*) from information_schema.tables where table_name = 'cliente'",
                Integer.class);
        assertThat(tabCliente).isEqualTo(1);
    }
}