package ar.edu.utn.tfi.service.Pagos;

import ar.edu.utn.tfi.web.dto.PagoApiReq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings({"rawtypes", "unchecked"})
@ExtendWith(MockitoExtension.class)
class PaymentApiServiceTest {

    @Mock
    WebClient.Builder builder;

    @Mock
    WebClient webClient;

    @Mock
    WebClient.RequestBodyUriSpec postSpec;

    @Mock
    WebClient.RequestHeadersSpec<?> headersSpec;

    @Mock
    WebClient.ResponseSpec responseSpec;

    @Captor
    ArgumentCaptor<Map<String, Object>> bodyCaptor;

    PaymentApiService service;

    @BeforeEach
    void setUp() {
        // cuando hagan builder.baseUrl(...).build() → que devuelva nuestro webClient mock
        when(builder.baseUrl(anyString())).thenReturn(builder);
        when(builder.build()).thenReturn(webClient);

        service = new PaymentApiService(builder);

        // Cadena de WebClient simulada
        when(webClient.post()).thenReturn(postSpec);
        when(postSpec.uri(anyString())).thenReturn(postSpec);
        when(postSpec.header(anyString(), anyString())).thenReturn(postSpec);
        when(postSpec.contentType(any(MediaType.class))).thenReturn(postSpec);
        when(postSpec.bodyValue(any())).thenReturn((WebClient.RequestHeadersSpec) headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    @DisplayName("crearPago arma correctamente el body enviado a MP (payer básico, installments por defecto)")
    void crearPago_armaBodyBasico_ok() {
        // arrange
        Map<String, Object> mpResponse = Map.of(
                "status", "approved",
                "id", 123456,
                "date_approved", "2025-01-01T10:15:30Z"
        );
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(mpResponse));

        Long presupuestoId = 10L;
        BigDecimal monto = new BigDecimal("150000.50");

        // 👇 OJO: orden correcto del constructor PagoApiReq
        PagoApiReq req = new PagoApiReq(
                "tokentest",        // token
                null,               // paymentMethodId
                null,               // installments
                null,               // issuerId
                "cliente@test.com", // payerEmail
                null,               // payerDocType
                null                // payerDocNumber
        );

        // act
        Map<String, Object> resp = service.crearPago(presupuestoId, monto, req);

        // assert
        assertEquals("approved", resp.get("status"));
        assertEquals(123456, resp.get("id"));

        verify(postSpec).bodyValue(bodyCaptor.capture());
        Map<String, Object> body = bodyCaptor.getValue();

        assertEquals(monto, body.get("transaction_amount"));
        assertEquals("tokentest", body.get("token"));
        assertEquals("Seña Presupuesto #" + presupuestoId, body.get("description"));
        assertEquals(1, body.get("installments")); // default

        assertEquals("PRESUPUESTO-" + presupuestoId, body.get("external_reference"));

        Map<String, Object> metadata = (Map<String, Object>) body.get("metadata");
        assertNotNull(metadata);
        assertEquals(presupuestoId, metadata.get("presupuesto_id"));

        Map<String, Object> payer = (Map<String, Object>) body.get("payer");
        assertNotNull(payer);
        assertEquals("cliente@test.com", payer.get("email"));

        verify(postSpec).header(eq(HttpHeaders.AUTHORIZATION), startsWith("Bearer "));
    }

    @Test
    @DisplayName("crearPago incluye identificación del payer y respeta installments > 1")
    void crearPago_conDatosPayer_eInstallments_ok() {
        // arrange
        Map<String, Object> mpResponse = Map.of("status", "in_process");
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(mpResponse));

        Long presupuestoId = 20L;
        BigDecimal monto = new BigDecimal("50000");

        // 👇 con 3 cuotas, método de pago y doc
        PagoApiReq req = new PagoApiReq(
                "token-123",      // token
                "visa",           // paymentMethodId
                3,                // installments
                "123",            // issuerId
                "user@mail.com",  // payerEmail
                "DNI",            // payerDocType
                "30111222"        // payerDocNumber
        );

        // act
        service.crearPago(presupuestoId, monto, req);

        // assert
        verify(postSpec).bodyValue(bodyCaptor.capture());
        Map<String, Object> body = bodyCaptor.getValue();

        assertEquals(3, body.get("installments"));
        assertEquals("visa", body.get("payment_method_id"));
        assertEquals("123", body.get("issuer_id"));

        Map<String, Object> payer = (Map<String, Object>) body.get("payer");
        assertNotNull(payer);
        assertEquals("user@mail.com", payer.get("email"));

        Map<String, Object> ident = (Map<String, Object>) payer.get("identification");
        assertNotNull(ident);
        assertEquals("DNI", ident.get("type"));
        assertEquals("30111222", ident.get("number"));
    }

    @Test
    @DisplayName("crearPago mapea WebClientResponseException a IllegalArgumentException con mensaje de MP")
    void crearPago_errorDeMP_lanzaIllegalArgumentException() {
        // arrange
        String bodyError = "{\"message\":\"tarjeta rechazada\"}";

        WebClientResponseException ex = WebClientResponseException.create(
                400,
                "Bad Request",
                HttpHeaders.EMPTY,
                bodyError.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.error(ex));

        Long presupuestoId = 30L;
        BigDecimal monto = new BigDecimal("1000");

        PagoApiReq req = new PagoApiReq(
                "tokentest",  // token
                null,         // paymentMethodId
                1,            // installments
                null,         // issuerId
                null,         // payerEmail
                null,         // payerDocType
                null          // payerDocNumber
        );

        // act + assert
        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> service.crearPago(presupuestoId, monto, req)
        );

        assertTrue(thrown.getMessage().contains("Mercado Pago rechazó la solicitud"));
        assertTrue(thrown.getMessage().contains("tarjeta rechazada"));
    }
}