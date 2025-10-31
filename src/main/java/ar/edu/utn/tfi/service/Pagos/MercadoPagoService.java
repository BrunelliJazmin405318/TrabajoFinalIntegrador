// src/main/java/ar/edu/utn/tfi/service/Pagos/MercadoPagoService.java
package ar.edu.utn.tfi.service.Pagos;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferencePayerRequest;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preference.Preference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class MercadoPagoService {

    @Value("${MP_ACCESS_TOKEN}")
    private String accessToken;

    private void init() {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException("MP_ACCESS_TOKEN no configurado");
        }
        MercadoPagoConfig.setAccessToken(accessToken);
    }

    public Preference crearPreferenciaSena(
            Long presupuestoId,
            BigDecimal montoSena,
            String descripcion,
            String emailCliente
    ) throws Exception {
        init();

        try {
            PreferenceItemRequest item = PreferenceItemRequest.builder()
                    .title(descripcion)
                    .quantity(1)
                    .currencyId("ARS")
                    .unitPrice(montoSena)
                    .build();

            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success("http://localhost:8080/estado-solicitud.html?id=" + presupuestoId)
                    .failure("http://localhost:8080/estado-solicitud.html?id=" + presupuestoId)
                    .pending("http://localhost:8080/estado-solicitud.html?id=" + presupuestoId)
                    .build();

            PreferenceRequest.PreferenceRequestBuilder builder = PreferenceRequest.builder()
                    .items(List.of(item))
                    .externalReference("PRESUPUESTO-" + presupuestoId)
                    .backUrls(backUrls);
            // Si tenés webhook público, podés agregar:
            // builder.notificationUrl("https://tu-dominio-o-ngrok/pagos/webhook-mp");

            if (emailCliente != null && !emailCliente.isBlank()) {
                builder.payer(PreferencePayerRequest.builder().email(emailCliente.trim()).build());
            }

            PreferenceClient client = new PreferenceClient();
            return client.create(builder.build());

        } catch (MPApiException apiEx) {
            int status = apiEx.getStatusCode();
            String body = apiEx.getApiResponse() != null ? apiEx.getApiResponse().getContent() : "(sin cuerpo)";
            System.err.println("[MPApiException] status=" + status + " body=" + body);
            throw new IllegalStateException("MercadoPago API error (status " + status + "): " + body);
        } catch (MPException sdkEx) {
            System.err.println("[MPException] " + sdkEx.getMessage());
            throw new IllegalStateException("MercadoPago client error: " + sdkEx.getMessage());
        }
    }

    public String linkDePreferencia(String preferenceId) {
        return null; // por ahora no reusamos init_point por id
    }
}