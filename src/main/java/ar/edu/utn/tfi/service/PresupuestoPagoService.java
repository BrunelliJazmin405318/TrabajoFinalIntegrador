package ar.edu.utn.tfi.service;

import ar.edu.utn.tfi.domain.Presupuesto;
import ar.edu.utn.tfi.repository.PresupuestoRepository;
import ar.edu.utn.tfi.service.Pagos.MercadoPagoService;
import ar.edu.utn.tfi.web.dto.LinkPagoDTO;
import com.mercadopago.resources.preference.Preference;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Lógica de pagos para Presupuesto (seña y confirmación).
 * - Genera link de pago de seña (30% del total)
 * - Evita duplicar preferencias si ya hay una seña "PENDIENTE"
 * - Permite marcar la seña como ACREDITADA (usada por el webhook o un admin)
 */
@Service
public class PresupuestoPagoService {

    private final PresupuestoRepository presupuestoRepo;
    private final MercadoPagoService mercadoPagoService;


    // Si querés parametrizar el porcentaje por config, podés inyectarlo con @Value
    private static final BigDecimal PORCENTAJE_SENA = new BigDecimal("0.30");

    public PresupuestoPagoService(PresupuestoRepository presupuestoRepo,
                                  MercadoPagoService mercadoPagoService) {
        this.presupuestoRepo = presupuestoRepo;
        this.mercadoPagoService = mercadoPagoService;
    }

    /**
     * Genera (o reutiliza) el link de pago para la SEÑA del presupuesto.
     * Requisitos:
     *  - Presupuesto debe estar APROBADO
     *  - Calcula 30% del total
     *  - Si ya existe una seña PENDIENTE con preferenceId, devuelve el mismo link (no crea otro)
     */

    @Transactional
    public LinkPagoDTO generarLinkSena(Long presupuestoId) throws Exception {
        Presupuesto p = presupuestoRepo.findById(presupuestoId)
                .orElseThrow(() -> new EntityNotFoundException("No existe presupuesto: " + presupuestoId));

        // ✅ Validaciones básicas
        if (!"APROBADO".equalsIgnoreCase(safe(p.getEstado()))) {
            throw new IllegalStateException("La seña solo se puede generar si el presupuesto está APROBADO.");
        }
        if (p.getTotal() == null || p.getTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("El presupuesto no tiene un total válido para calcular la seña.");
        }

        // ✅ Calcular 30% con 2 decimales
        BigDecimal montoSena = p.getTotal()
                .multiply(PORCENTAJE_SENA)
                .setScale(2, RoundingMode.HALF_UP);

        // ✅ Si ya hay una seña pendiente con link guardado, reutilizar
        if ("PENDIENTE".equalsIgnoreCase(safe(p.getSenaEstado())) && notBlank(p.getSenaPreferenceId())) {
            if (notBlank(p.getSenaInitPoint())) {
                return new LinkPagoDTO(
                        p.getId(),
                        p.getSenaMonto() != null ? p.getSenaMonto() : montoSena,
                        p.getSenaInitPoint()
                );
            }
            // Intentar recuperar el link si no estaba guardado
            String urlPorId = mercadoPagoService.linkDePreferencia(p.getSenaPreferenceId());
            if (notBlank(urlPorId)) {
                p.setSenaInitPoint(urlPorId);
                if (p.getSenaMonto() == null) p.setSenaMonto(montoSena);
                presupuestoRepo.save(p);
                return new LinkPagoDTO(p.getId(), p.getSenaMonto(), urlPorId);
            }
            // Si no puede recuperarlo, se genera una nueva preferencia
        }

        // ✅ Crear preferencia nueva en Mercado Pago
        Preference pref = mercadoPagoService.crearPreferenciaSena(
                p.getId(),
                montoSena,
                "Seña presupuesto #" + p.getId(),
                p.getClienteEmail()
        );

        System.out.println("[MP PREF] id=" + pref.getId()
                + " init=" + pref.getInitPoint()
                + " sandbox=" + pref.getSandboxInitPoint());

        // ⚠ USAR SIEMPRE SANDBOX PARA TESTEO
        String url = (pref.getSandboxInitPoint() != null && !pref.getSandboxInitPoint().isBlank())
                ? pref.getSandboxInitPoint()
                : pref.getInitPoint();

        if (url == null || url.isBlank()) {
            throw new IllegalStateException("MercadoPago no devolvió ningún link válido para la preferencia " + pref.getId());
        }

        // ✅ Guardar datos de la seña en la entidad
        p.setSenaMonto(montoSena);
        p.setSenaPreferenceId(pref.getId());
        p.setSenaEstado("PENDIENTE");
        p.setSenaInitPoint(url);
        presupuestoRepo.save(p);

        // ✅ Devolver al front
        return new LinkPagoDTO(p.getId(), montoSena, url);
    }
    /**
     * Marca la seña como ACREDITADA dado un preferenceId.
     * Pensado para ser llamado desde el webhook de MP (HU11) o desde un endpoint admin de verificación.
     * No cambia el estado general del Presupuesto (eso se decide en otra lógica).
     */
    @Transactional
    public void marcarSenaAcreditada(String preferenceId) {
        if (!notBlank(preferenceId)) {
            throw new IllegalArgumentException("preferenceId inválido");
        }

        Presupuesto p = presupuestoRepo.findBySenaPreferenceId(preferenceId)
                .orElseThrow(() -> new EntityNotFoundException("No existe presupuesto con esa seña: " + preferenceId));

        // Si ya está acreditada, no hacemos nada idempotente
        if ("ACREDITADA".equalsIgnoreCase(safe(p.getSenaEstado()))) {
            return;
        }

        p.setSenaEstado("ACREDITADA");
        presupuestoRepo.save(p);
    }

    // ─────────────────────────────────────────────────────────────────────────────

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String safe(String s) {
        return Objects.toString(s, "");
    }
}
