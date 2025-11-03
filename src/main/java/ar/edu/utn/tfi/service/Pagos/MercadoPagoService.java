// src/main/java/ar/edu/utn/tfi/service/Pagos/MercadoPagoService.java
package ar.edu.utn.tfi.service.Pagos;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferencePayerRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preference.Preference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;


// MercadoPagoService.java
@Service
public class MercadoPagoService {

    @Value("${mp.api.access-token}")
    private String accessToken;


    @Value("${app.public-base-url}")  // << más coherente con tu yml
    private String baseUrl;

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

        PreferenceItemRequest item = PreferenceItemRequest.builder()
                .title(descripcion)
                .quantity(1)
                .currencyId("ARS")
                .unitPrice(montoSena)
                .build();

        // usa SIEMPRE tu dominio público
        String retUrl = baseUrl + "/estado-solicitud.html?id=" + presupuestoId;

        PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                .success(retUrl)
                .failure(retUrl)
                .pending(retUrl)
                .build();

        PreferenceRequest.PreferenceRequestBuilder builder = PreferenceRequest.builder()
                .items(List.of(item))
                .externalReference("PRESUPUESTO-" + presupuestoId)
                .backUrls(backUrls)
        .notificationUrl(baseUrl + "/pagos/webhook-mp"); // si ya lo querés dejar activado

        if (emailCliente != null && !emailCliente.isBlank()) {
            builder.payer(PreferencePayerRequest.builder().email(emailCliente.trim()).build());
        }

        PreferenceClient client = new PreferenceClient();
        Preference pref = client.create(builder.build());

        // Log de diagnóstico por si vuelve null
        System.out.println("[MP PREF] id=" + pref.getId()
                + " init=" + pref.getInitPoint()
                + " sandbox=" + pref.getSandboxInitPoint());

        return pref;
    }

    public String linkDePreferencia(String preferenceId) {
        return null; // no reusamos por id
    }
}