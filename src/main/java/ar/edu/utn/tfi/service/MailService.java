package ar.edu.utn.tfi.service;
import ar.edu.utn.tfi.domain.SolicitudPresupuesto;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {
    private final JavaMailSender mail;

    public MailService(JavaMailSender mail) {
        this.mail = mail;
    }

    public void enviarDecisionSolicitud(SolicitudPresupuesto s) {
        // si no tenemos email del cliente, no enviamos (evita fallos)
        String to = (s.getClienteEmail() == null || s.getClienteEmail().isBlank())
                ? null
                : s.getClienteEmail().trim();

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

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom("no-reply@rectificadora.test");
        msg.setTo(to);
        msg.setSubject(asunto);
        msg.setText(cuerpo);

        mail.send(msg);
        System.out.println("✅ Enviado mail de decisión a " + to + " (solicitud " + s.getId() + ")");
    }
}
