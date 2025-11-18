package ar.edu.utn.tfi.service;

import ar.edu.utn.tfi.domain.FacturaMock;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.util.Locale;

@Service
public class MailService {
    private final JavaMailSender mail;

    public MailService(JavaMailSender mail) {
        this.mail = mail;
    }

    // ========== Helpers ==========
    private void enviarCorreo(String to, String asunto, String cuerpo) {
        if (to == null || to.isBlank()) {
            System.out.println("ℹ️ Mail NO enviado (destinatario vacío). Asunto: " + asunto);
            return;
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom("no-reply@rectificadora.test");
        msg.setTo(to.trim());
        msg.setSubject(asunto);
        msg.setText(cuerpo);

        mail.send(msg);
        System.out.println("✅ Mail enviado a " + to + " | " + asunto);
    }

    private String buildPublicFacturaUrl(Long solicitudId) {
        return "http://localhost:8080/public/facturas/pdf/by-solicitud/" + solicitudId;
    }

    private String moneyAr(Number n) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
        return nf.format(n);
    }

    // ========== SOLO: email de factura emitida ==========
    public void enviarFacturaEmitida(FacturaMock f) {
        String to = (f.getClienteEmail() == null || f.getClienteEmail().isBlank())
                ? null : f.getClienteEmail().trim();
        if (to == null) {
            System.out.println("ℹ️ Factura " + f.getNumero() + " sin email de cliente. No se envía aviso.");
            return;
        }

        String asunto = "Factura emitida - " + f.getNumero();
        String linkDescarga = buildPublicFacturaUrl(f.getPresupuesto().getSolicitudId());

        String cuerpo = """
                Hola %s,
                
                Tu factura %s (tipo %s) ha sido emitida por un total de %s.
                
                Podés descargarla desde el siguiente enlace:
                %s
                
                ¡Gracias por confiar en nosotros!
                """.formatted(
                f.getClienteNombre() == null ? "" : f.getClienteNombre(),
                f.getNumero(),
                f.getTipo(),
                moneyAr(f.getTotal()),
                linkDescarga
        );

        enviarCorreo(to, asunto, cuerpo);
    }
}