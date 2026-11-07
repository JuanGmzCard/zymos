package com.alera.controller;

import com.alera.model.CheckoutPendiente;
import com.alera.model.enums.PlanNivel;
import com.alera.payment.MercadoPagoGateway;
import com.alera.repository.CheckoutPendienteRepository;
import com.alera.repository.TenantRepository;
import com.alera.service.TenantProvisioningService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;

@Controller
public class CheckoutController {

    private static final Logger log = LoggerFactory.getLogger(CheckoutController.class);
    private static final Pattern SUBDOMINIO_RE = Pattern.compile("^[a-z0-9]{3,30}$");
    private static final Set<String> RESERVADOS = Set.of(
            "demo", "admin", "www", "api", "app", "mail", "default", "zymos",
            "test", "staging", "dev", "support", "help", "billing", "pay", "checkout");

    private final TenantRepository            tenantRepo;
    private final CheckoutPendienteRepository checkoutRepo;
    private final MercadoPagoGateway          pasarela;
    private final TenantProvisioningService   provisioning;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    public CheckoutController(TenantRepository tenantRepo,
                               CheckoutPendienteRepository checkoutRepo,
                               MercadoPagoGateway pasarela,
                               TenantProvisioningService provisioning) {
        this.tenantRepo    = tenantRepo;
        this.checkoutRepo  = checkoutRepo;
        this.pasarela      = pasarela;
        this.provisioning  = provisioning;
    }

    // ─── Página de planes ──────────────────────────────────────────────────────

    @GetMapping("/planes")
    public String planes(Model model) {
        model.addAttribute("planes", PlanNivel.values());
        model.addAttribute("mpHabilitado", pasarela.isHabilitado());
        return "plan/planes";
    }

    // ─── Verificación de subdomain disponible (AJAX) ───────────────────────────

    @GetMapping("/checkout/verificar-subdominio")
    @ResponseBody
    public Map<String, Object> verificarSubdominio(@RequestParam String s) {
        String sub = s.toLowerCase(Locale.ROOT).trim();
        Map<String, Object> resp = new LinkedHashMap<>();

        if (!SUBDOMINIO_RE.matcher(sub).matches()) {
            resp.put("disponible", false);
            resp.put("mensaje", "Solo letras y números (3–30 caracteres, sin guiones)");
            return resp;
        }
        if (RESERVADOS.contains(sub)) {
            resp.put("disponible", false);
            resp.put("mensaje", "\"" + sub + "\" es una palabra reservada");
            return resp;
        }
        boolean libre = !tenantRepo.existsById(sub)
                && checkoutRepo.findBySubdominioAndEstado(sub, "PENDIENTE").isEmpty();
        resp.put("disponible", libre);
        resp.put("mensaje", libre ? "¡Disponible!" : "El subdominio ya está en uso");
        return resp;
    }

    // ─── Iniciar checkout ──────────────────────────────────────────────────────

    @PostMapping("/checkout/iniciar")
    public String iniciarCheckout(@RequestParam String planNivel,
                                   @RequestParam String nombreCerveceria,
                                   @RequestParam String subdominio,
                                   @RequestParam String email,
                                   Model model,
                                   HttpServletRequest request) {
        String sub = subdominio.toLowerCase(Locale.ROOT).trim();

        // Validaciones
        if (!SUBDOMINIO_RE.matcher(sub).matches()) {
            return errorPlanes(model, "El subdominio solo puede contener letras y números (3–30 caracteres, sin guiones ni espacios).", planNivel);
        }
        if (RESERVADOS.contains(sub)) {
            return errorPlanes(model, "\"" + sub + "\" es un nombre reservado.", planNivel);
        }
        if (tenantRepo.existsById(sub)) {
            return errorPlanes(model, "El subdominio \"" + sub + "\" ya está en uso.", planNivel);
        }
        if (checkoutRepo.findBySubdominioAndEstado(sub, "PENDIENTE").isPresent()) {
            return errorPlanes(model, "Ya hay un pago pendiente para ese subdominio. Esperá unos minutos o elegí otro.", planNivel);
        }
        if (email == null || !email.contains("@")) {
            return errorPlanes(model, "El email ingresado no es válido.", planNivel);
        }

        PlanNivel nivel;
        try {
            nivel = PlanNivel.valueOf(planNivel.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return errorPlanes(model, "Plan no reconocido.", null);
        }

        // Crear registro de checkout
        CheckoutPendiente checkout = new CheckoutPendiente();
        checkout.setId(UUID.randomUUID().toString());
        checkout.setSubdominio(sub);
        checkout.setEmail(email.trim().toLowerCase(Locale.ROOT));
        checkout.setNombreCerveceria(nombreCerveceria.trim());
        checkout.setPlanNivel(nivel);
        checkoutRepo.save(checkout);

        // Crear preferencia en MP y redirigir
        try {
            String baseUrl = resolverBaseUrl(request);
            String checkoutUrl = pasarela.crearSesion(checkout, baseUrl);
            return "redirect:" + checkoutUrl;
        } catch (Exception e) {
            log.error("Error creando sesión de pago: {}", e.getMessage(), e);
            checkoutRepo.deleteById(checkout.getId());
            return errorPlanes(model, "No se pudo conectar con la pasarela de pago. Intentá de nuevo.", planNivel);
        }
    }

    // ─── Trial gratuito (14 días, sin pago) ───────────────────────────────────

    @PostMapping("/checkout/trial")
    public String iniciarTrial(@RequestParam String nombreCerveceria,
                                @RequestParam String subdominio,
                                @RequestParam String email,
                                Model model,
                                HttpServletRequest request) {
        String sub = subdominio.toLowerCase(Locale.ROOT).trim();

        if (!SUBDOMINIO_RE.matcher(sub).matches()) {
            return errorPlanes(model, "El subdominio solo puede contener letras y números (3–30 caracteres).", null);
        }
        if (RESERVADOS.contains(sub)) {
            return errorPlanes(model, "\"" + sub + "\" es un nombre reservado.", null);
        }
        if (tenantRepo.existsById(sub)) {
            return errorPlanes(model, "El subdominio \"" + sub + "\" ya está en uso.", null);
        }
        if (checkoutRepo.findBySubdominioAndEstado(sub, "PENDIENTE").isPresent()) {
            return errorPlanes(model, "Ya hay un proceso activo para ese subdominio. Esperá unos minutos.", null);
        }
        if (email == null || !email.contains("@")) {
            return errorPlanes(model, "El email ingresado no es válido.", null);
        }

        String baseUrl = resolverBaseUrl(request);
        TenantProvisioningService.ResultadoProvision resultado = provisioning.provisionar(
                sub, email.trim().toLowerCase(Locale.ROOT),
                nombreCerveceria.trim(), PlanNivel.FERMENTACION, baseUrl, 14);

        if (!resultado.exito() && !resultado.yaExistia()) {
            return errorPlanes(model, "No se pudo crear tu cuenta de prueba. Intentá de nuevo.", null);
        }

        CheckoutPendiente display = new CheckoutPendiente();
        display.setNombreCerveceria(nombreCerveceria.trim());
        display.setEmail(email.trim().toLowerCase(Locale.ROOT));
        display.setPlanNivel(PlanNivel.FERMENTACION);

        String loginUrl = resultado.loginUrl() != null
                ? resultado.loginUrl() : construirLoginUrl(sub);
        model.addAttribute("esTrial", true);
        model.addAttribute("checkout", display);
        model.addAttribute("loginUrl", loginUrl);
        model.addAttribute("trialFin", LocalDate.now().plusDays(14));
        return "plan/exito";
    }

    // ─── Retorno desde MP ──────────────────────────────────────────────────────

    @GetMapping("/checkout/exito")
    public String exito(@RequestParam(value = "external_reference", required = false) String externalRef,
                         @RequestParam(value = "payment_id",        required = false) String paymentId,
                         @RequestParam(value = "collection_status",  required = false) String status,
                         Model model,
                         HttpServletRequest request) {
        if (externalRef == null || paymentId == null) {
            model.addAttribute("error", "Parámetros de pago incompletos.");
            return "plan/fallo";
        }

        Optional<CheckoutPendiente> opt = checkoutRepo.findById(externalRef);
        if (opt.isEmpty()) {
            model.addAttribute("error", "Sesión de compra no encontrada.");
            return "plan/fallo";
        }

        CheckoutPendiente checkout = opt.get();
        if ("PROCESADO".equals(checkout.getEstado())) {
            // Ya fue provisionado (por webhook o por clic doble)
            return mostrarExito(model, checkout);
        }

        boolean aprobado = "approved".equals(status) || pasarela.pagoAprobado(paymentId);
        if (!aprobado) {
            model.addAttribute("error", "El pago no fue aprobado (estado: " + status + ").");
            return "plan/fallo";
        }

        return provisionar(checkout, paymentId, resolverBaseUrl(request), model);
    }

    @GetMapping("/checkout/pendiente")
    public String pendiente(Model model) {
        model.addAttribute("planes", PlanNivel.values());
        return "plan/pendiente";
    }

    @GetMapping("/checkout/fallo")
    public String fallo(Model model) {
        model.addAttribute("planes", PlanNivel.values());
        return "plan/fallo";
    }

    // ─── Webhook MercadoPago ───────────────────────────────────────────────────

    @PostMapping("/webhook/mp")
    @ResponseBody
    public ResponseEntity<Void> webhookMp(
            @RequestBody(required = false) String payload,
            @RequestParam Map<String, String> params,
            @RequestHeader(value = "x-signature",   required = false) String xSignature,
            @RequestHeader(value = "x-request-id",  required = false) String xRequestId) {
        log.debug("Webhook MP recibido — params={}", params.keySet());

        if (!pasarela.verificarFirmaWebhook(xSignature, xRequestId, payload, params)) {
            log.warn("Webhook MP rechazado: firma inválida");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Optional<String> paymentIdOpt = pasarela.resolverPaymentIdDesdeWebhook(payload, params);
            if (paymentIdOpt.isEmpty()) return ResponseEntity.ok().build();

            String paymentId = paymentIdOpt.get();
            if (!pasarela.pagoAprobado(paymentId)) return ResponseEntity.ok().build();

            String extRef = paymentId.startsWith("MOCK-") ? params.get("external_reference")
                                                           : pasarela.obtenerExternalReference(paymentId);
            if (extRef == null) return ResponseEntity.ok().build();

            checkoutRepo.findById(extRef).ifPresent(checkout -> {
                if (!"PROCESADO".equals(checkout.getEstado())) {
                    provisionar(checkout, paymentId, appBaseUrl, null);
                }
            });
        } catch (Exception e) {
            log.error("Error procesando webhook MP: {}", e.getMessage(), e);
        }
        return ResponseEntity.ok().build();
    }

    // ─── Helpers internos ──────────────────────────────────────────────────────

    private String provisionar(CheckoutPendiente checkout, String paymentId,
                                String baseUrl, Model model) {
        TenantProvisioningService.ResultadoProvision resultado = provisioning.provisionar(
                checkout.getSubdominio(),
                checkout.getEmail(),
                checkout.getNombreCerveceria(),
                checkout.getPlanNivel(),
                baseUrl);

        if (!resultado.exito() && !resultado.yaExistia()) {
            if (model != null) {
                model.addAttribute("error", resultado.mensaje());
                return "plan/fallo";
            }
            return "plan/fallo";
        }

        // Marcar como procesado DESPUÉS de confirmar provisión exitosa (o ya existente)
        checkoutRepo.marcarProcesado(checkout.getId(), paymentId, "PROCESADO");
        checkout.setEstado("PROCESADO");

        if (model != null) return mostrarExito(model, checkout);
        return "plan/exito";
    }

    private String mostrarExito(Model model, CheckoutPendiente checkout) {
        model.addAttribute("esTrial", false);
        model.addAttribute("checkout", checkout);
        model.addAttribute("loginUrl", construirLoginUrl(checkout.getSubdominio()));
        return "plan/exito";
    }

    private String errorPlanes(Model model, String mensaje, String planSeleccionado) {
        model.addAttribute("error", mensaje);
        model.addAttribute("planes", PlanNivel.values());
        model.addAttribute("planSeleccionado", planSeleccionado);
        return "plan/planes";
    }

    private String resolverBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String host   = request.getServerName();
        int    port   = request.getServerPort();
        boolean defaultPort = ("https".equals(scheme) && port == 443)
                           || ("http".equals(scheme)  && port == 80);
        return scheme + "://" + host + (defaultPort ? "" : ":" + port);
    }

    private String construirLoginUrl(String subdomain) {
        if (appBaseUrl.contains("localhost") || appBaseUrl.contains("127.0.0.1")) {
            return appBaseUrl;
        }
        return appBaseUrl.replaceFirst("://([^./]+)", "://" + subdomain);
    }
}
