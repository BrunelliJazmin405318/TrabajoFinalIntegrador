package ar.edu.utn.tfi.web.dto;

public record PagoApiReq(String token,
                         String paymentMethodId,
                         Integer installments,
                         String issuerId,
                         String payerEmail) {
}
