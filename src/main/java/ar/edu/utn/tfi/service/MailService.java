package ar.edu.utn.tfi.service;

import ar.edu.utn.tfi.domain.FacturaMock;
import ar.edu.utn.tfi.domain.SolicitudPresupuesto;
import ar.edu.utn.tfi.domain.Presupuesto;
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
        // Ajustá el dominio y el path si tu endpoint es diferente
        // Ej: si implementaste /public/facturas/pdf/by-solicitud/{id}
        return "http://localhost:8080/public/facturas/pdf/by-solicitud/" + solicitudId;
    }

    private String moneyAr(Number n) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));
        return nf.format(n);
    }

    // ========== Correos existentes ==========

    public void enviarDecisionSolicitud(SolicitudPresupuesto s) {
        String to = (s.getClienteEmail() == null || s.getClienteEmail().isBlank())
                ? null : s.getClienteEmail().trim();

        if (to == null) {
            System.out.println("ℹ️ Solicitud " + s.getId() + " sin email de cliente. No se envía aviso.");
            return;
        }

        String asunto = switch (s.getEstado()) {
            case "APROBADO" -> "Tu solicitud fue APROBADA";
            case "RECHAZADO" -> "Tu solicitud fue RECHAZADA";
            default -> "Actualización de tu solicitud";
        };

        String cuerpo = """
                Hola %s,
                
                Te escribimos de la rectificadora para informarte que tu solicitud N° %d fue %s.
                
                Detalle: %s

                Gracias por contactarnos.
                """.formatted(
                s.getClienteNombre() == null ? "" : s.getClienteNombre(),
                s.getId(),
                s.getEstado(),
                (s.getDecisionMotivo() == null || s.getDecisionMotivo().isBlank()) ? "-" : s.getDecisionMotivo()
        );

        enviarCorreo(to, asunto, cuerpo);
    }

    public void enviarDecisionPresupuesto(Presupuesto p) {
        String to = (p.getClienteEmail() == null || p.getClienteEmail().isBlank())
                ? null : p.getClienteEmail().trim();
        if (to == null) {
            System.out.println("ℹ️ Presupuesto " + p.getId() + " sin email de cliente. No se envía aviso.");
            return;
        }

        String asunto = switch (p.getEstado()) {
            case "APROBADO" -> "Tu presupuesto fue APROBADO";
            case "RECHAZADO" -> "Tu presupuesto fue RECHAZADO";
            default -> "Actualización de tu presupuesto";
        };

        String cuerpo = """
                Hola %s,

                Te informamos que tu presupuesto N° %d fue %s.

                Monto total: %s
                Detalle: %s

                Gracias por contactarnos.
                """.formatted(
                p.getClienteNombre() == null ? "" : p.getClienteNombre(),
                p.getId(),
                p.getEstado(),
                moneyAr(p.getTotal()),
                (p.getDecisionMotivo() == null || p.getDecisionMotivo().isBlank()) ? "-" : p.getDecisionMotivo()
        );

        enviarCorreo(to, asunto, cuerpo);
    }

    // ========== NUEVO: email de factura emitida ==========
    public void enviarFacturaEmitida(FacturaMock f) {
        String to = (f.getClienteEmail() == null || f.getClienteEmail().isBlank())
                ? null : f.getClienteEmail().trim();
        if (to == null) {
            System.out.println("ℹ️ Factura " + f.getNumero() + " sin email de cliente. No se envía aviso.");
            return;
        }

        String asunto = "Factura emitida - " + f.getNumero();

        // Link público para que el cliente descargue por su solicitud.
        // Si no tenés ese endpoint, podés cambiarlo a by-presupuesto o al que hayas creado.
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