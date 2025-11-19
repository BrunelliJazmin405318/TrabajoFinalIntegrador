// src/test/java/ar/edu/utn/tfi/web/PublicSolicitudEndToEndTest.java
package ar.edu.utn.tfi.web;

import ar.edu.utn.tfi.infra.PostgresTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PublicSolicitudEndToEndTest extends PostgresTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("Flujo público: crear solicitud y luego verla por ID")
    void crearYVerSolicitud_endToEnd() throws Exception {
        // 1) Crear solicitud pública
        String body = """
                {
                  "clienteNombre": "Juan Perez",
                  "clienteTelefono": "+54351...",
                  "clienteEmail": "juan@test.com",
                  "tipoUnidad": "MOTOR",
                  "marca": "Ford",
                  "modelo": "Fiesta",
                  "nroMotor": "ABC123",
                  "descripcion": "Golpe en cilindro",
                  "tipoConsulta": "COTIZACION"
                }
                """;

        MvcResult createResult = mvc.perform(post("/public/presupuestos/solicitud")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andReturn();

        String json = createResult.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(json);
        long solicitudId = root.get("id").asLong();

        // 2) Leer la solicitud por ID y verificar campos
        mvc.perform(get("/public/presupuestos/solicitud/{id}", solicitudId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(solicitudId))
                .andExpect(jsonPath("$.clienteNombre").value("Juan Perez"))
                .andExpect(jsonPath("$.clienteTelefono").value("+54351..."))
                .andExpect(jsonPath("$.clienteEmail").value("juan@test.com"))
                .andExpect(jsonPath("$.tipoUnidad").value("MOTOR"))
                .andExpect(jsonPath("$.descripcion").value("Golpe en cilindro"));
    }

    @Test
    @DisplayName("Flujo: crear solicitud pública, aprobarla como admin y verla ya aprobada")
    void crearAprobarYVerSolicitud_endToEnd() throws Exception {
        // 1) Crear solicitud pública
        String body = """
                {
                  "clienteNombre": "Maria Lopez",
                  "clienteTelefono": "+549351...",
                  "clienteEmail": "maria@test.com",
                  "tipoUnidad": "TAPA",
                  "marca": "Chevrolet",
                  "modelo": "Onix",
                  "nroMotor": "MTR-999",
                  "descripcion": "Perdida de compresión",
                  "tipoConsulta": "COTIZACION"
                }
                """;

        MvcResult createResult = mvc.perform(post("/public/presupuestos/solicitud")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andReturn();

        String json = createResult.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(json);
        long solicitudId = root.get("id").asLong();

        // 2) Aprobar la solicitud como ADMIN
        String nota = """
                { "nota": "Todo correcto, se puede presupuestar" }
                """;

        mvc.perform(put("/admin/presupuestos/solicitudes/{id}/aprobar", solicitudId)
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nota))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Aprobada"))
                .andExpect(jsonPath("$.id").value(solicitudId));

        // 3) Volver a consultar la solicitud por el endpoint público
        mvc.perform(get("/public/presupuestos/solicitud/{id}", solicitudId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(solicitudId))
                .andExpect(jsonPath("$.clienteNombre").value("Maria Lopez"))
                .andExpect(jsonPath("$.estado").value("APROBADO"))
                .andExpect(jsonPath("$.decisionUsuario").value("admin"))
                .andExpect(jsonPath("$.decisionMotivo").value("Todo correcto, se puede presupuestar"));
    }

    @Test
    @DisplayName("Flujo completo: solicitud + aprobación + presupuesto + pago manual de seña + info-sena")
    void flujoCompleto_conPagoManual_endToEnd() throws Exception {
        // 1) Crear solicitud pública
        String bodySolicitud = """
            {
              "clienteNombre": "Carlos Gomez",
              "clienteTelefono": "+549351000000",
              "clienteEmail": "carlos@test.com",
              "tipoUnidad": "MOTOR",
              "marca": "Ford",
              "modelo": "Focus",
              "nroMotor": "MTR-123",
              "descripcion": "Ruidos extraños en el motor",
              "tipoConsulta": "COTIZACION"
            }
            """;

        MvcResult crearRes = mvc.perform(post("/public/presupuestos/solicitud")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodySolicitud))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andReturn();

        String json = crearRes.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(json);
        long solicitudId = root.get("id").asLong();

        // 2) Aprobar la solicitud como admin
        String notaSolicitud = """
            { "nota": "OK para presupuesto y seña" }
            """;

        mvc.perform(put("/admin/presupuestos/solicitudes/{id}/aprobar", solicitudId)
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notaSolicitud))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(solicitudId))
                .andExpect(jsonPath("$.message").value("Aprobada"));

        // 3) Generar presupuesto (ya hay tarifas para este servicio)
        String bodyPresupuesto = """
            {
              "solicitudId": %d,
              "vehiculoTipo": "CONVENCIONAL",
              "servicios": ["Rectificación de cilindros"],
              "piezaTipo": "MOTOR",
              "extras": []
            }
            """.formatted(solicitudId);

        MvcResult genRes = mvc.perform(post("/presupuestos/generar")
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyPresupuesto))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andReturn();

        String genJson = genRes.getResponse().getContentAsString();
        JsonNode genRoot = objectMapper.readTree(genJson);
        long presupuestoId = genRoot.get("id").asLong();

        // 3.5) Aprobar el presupuesto antes de registrar el pago manual
        String notaPresu = """
            { "nota": "OK, tomar seña" }
            """;

        mvc.perform(put("/admin/presupuestos/{id}/aprobar", presupuestoId)
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notaPresu))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(presupuestoId))
                .andExpect(jsonPath("$.message").value("Aprobado"));

        // 4) Registrar pago manual de seña
        // total = 50000 → seña esperada = 15000 (30%)
        String bodyPago = """
            {
              "presupuestoId": %d,
              "tipo": "SENA",
              "monto": 15000.0,
              "medio": "EFECTIVO",
              "referencia": "REC-001",
              "fechaPago": "2025-11-19",
              "nota": "Seña en efectivo"
            }
            """.formatted(presupuestoId);

        mvc.perform(post("/admin/pagos-manuales/registrar")
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyPago))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.presupuestoId").value(presupuestoId));

        // 5) Consultar info-sena público y verificar campos de pago
        mvc.perform(get("/public/pagos/api/info-sena/{id}", presupuestoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.presupuestoId").value(presupuestoId))
                .andExpect(jsonPath("$.senaEstado").value("ACREDITADA"))
                .andExpect(jsonPath("$.senaPaymentStatus").value("manual"))
                .andExpect(jsonPath("$.montoSena").value(15000))
                .andExpect(jsonPath("$.total").value(50000))
                .andExpect(jsonPath("$.saldoRestante").value(35000));
    }
}