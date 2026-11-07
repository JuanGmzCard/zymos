package com.alera.service;

import com.alera.model.ContactoWhatsApp;
import com.alera.model.Equipo;
import com.alera.model.InsumoInventario;
import com.alera.model.LoteCerveza;
import com.alera.model.enums.TipoAlertaWhatsApp;
import com.alera.repository.ContactoWhatsAppRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppService.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Value("${app.twilio.account-sid:}") private String accountSid;
    @Value("${app.twilio.auth-token:}")  private String authToken;
    @Value("${app.twilio.whatsapp-from:whatsapp:+14155238886}") private String from;

    private final ContactoWhatsAppRepository contactoRepo;
    private final RestTemplate restTemplate;

    public WhatsAppService(ContactoWhatsAppRepository contactoRepo) {
        this.contactoRepo = contactoRepo;
        this.restTemplate = new RestTemplate();
    }

    public boolean isConfigured() {
        return !accountSid.isBlank() && !authToken.isBlank();
    }

    // ── Core send ─────────────────────────────────────────────────────────────

    private void enviar(String telefono, String mensaje) {
        String toNumber = telefono.startsWith("whatsapp:") ? telefono : "whatsapp:" + telefono;
        if (!isConfigured()) {
            log.info("[WhatsApp MOCK] → {} : {}", toNumber, mensaje.replace("\n", " "));
            return;
        }
        try {
            String url = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBasicAuth(accountSid, authToken);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("From", from);
            body.add("To", toNumber);
            body.add("Body", mensaje);

            restTemplate.postForObject(url, new HttpEntity<>(body, headers), String.class);
            log.info("[WhatsApp] Enviado a {}", toNumber);
        } catch (Exception e) {
            log.error("[WhatsApp] Error enviando a {}: {}", toNumber, e.getMessage());
        }
    }

    private void enviarATipo(String tenantId, TipoAlertaWhatsApp tipo, String mensaje) {
        List<ContactoWhatsApp> contactos = contactoRepo.findActivosByTenantIdAndTipo(tenantId, tipo.name());
        if (contactos.isEmpty()) return;
        contactos.forEach(c -> enviar(c.getTelefono(), mensaje));
    }

    // ── Métodos por tipo de alerta ─────────────────────────────────────────────

    public void notificarBajoStock(String tenantId, List<InsumoInventario> items) {
        if (items.isEmpty()) return;
        String lista = items.stream()
                .limit(5)
                .map(i -> "• " + i.getNombre() + ": " +
                        (i.getCantidad() != null ? i.getCantidad().stripTrailingZeros().toPlainString() : "0") +
                        " " + i.getUnidad())
                .reduce("", (a, b) -> a + "\n" + b);
        String msg = "⚠️ *Bajo stock de insumos*\n" + lista.trim() +
                (items.size() > 5 ? "\n_(+" + (items.size() - 5) + " más)_" : "") +
                "\n\n👉 Revisar en Inventario → Bajo stock";
        enviarATipo(tenantId, TipoAlertaWhatsApp.BAJO_STOCK, msg);
    }

    public void notificarVencimiento(String tenantId, List<InsumoInventario> items) {
        if (items.isEmpty()) return;
        String lista = items.stream()
                .limit(5)
                .map(i -> "• " + i.getNombre() + " — vence " +
                        (i.getFechaVencimiento() != null ? i.getFechaVencimiento().format(FMT) : "?"))
                .reduce("", (a, b) -> a + "\n" + b);
        String msg = "⏰ *Vencimientos próximos*\n" + lista.trim() +
                (items.size() > 5 ? "\n_(+" + (items.size() - 5) + " más)_" : "") +
                "\n\n👉 Revisar en Inventario → Por vencer";
        enviarATipo(tenantId, TipoAlertaWhatsApp.VENCIMIENTO, msg);
    }

    public void notificarCambioFase(String tenantId, LoteCerveza lote, String fase) {
        String msg = "🍻 *Cambio de fase*\n" +
                "Lote: *" + lote.getCodigoLote() + "* — " + lote.getEstilo() + "\n" +
                "Nueva fase: *" + fase + "*\n\n" +
                "👉 Ver detalle en Trazabilidad";
        enviarATipo(tenantId, TipoAlertaWhatsApp.LOTE_FASE, msg);
    }

    public void notificarMantenimiento(String tenantId, List<Equipo> equipos) {
        if (equipos.isEmpty()) return;
        String lista = equipos.stream()
                .limit(5)
                .map(e -> "• " + e.getNombre())
                .reduce("", (a, b) -> a + "\n" + b);
        String msg = "🔧 *Mantenimiento pendiente*\n" + lista.trim() +
                (equipos.size() > 5 ? "\n_(+" + (equipos.size() - 5) + " más)_" : "") +
                "\n\n👉 Revisar en Equipos";
        enviarATipo(tenantId, TipoAlertaWhatsApp.MANTENIMIENTO, msg);
    }

    public void notificarBpmSalud(String tenantId, String nombreUsuario) {
        String msg = "🛡️ *Alerta BPM*\n" +
                "*" + nombreUsuario + "* reportó síntomas de salud y requiere autorización del administrador.\n\n" +
                "👉 Revisar en BPM → Autorizaciones de salud";
        enviarATipo(tenantId, TipoAlertaWhatsApp.BPM_SALUD, msg);
    }

    public void notificarTareaAsignada(String tenantId, String titulo, String asignadoA) {
        String msg = "✅ *Nueva tarea asignada*\n" +
                "Tarea: *" + titulo + "*\n" +
                "Asignada a: " + asignadoA + "\n\n" +
                "👉 Ver en módulo Tareas";
        enviarATipo(tenantId, TipoAlertaWhatsApp.TAREA_ASIGNADA, msg);
    }

    public void notificarPlanVencimiento(String tenantId, LocalDate fechaFin) {
        String fecha = fechaFin != null ? fechaFin.format(FMT) : "próximamente";
        String msg = "💳 *Plan por vencer*\n" +
                "El plan de tu cervecería vence el *" + fecha + "*.\n\n" +
                "👉 Renovar en Planes";
        enviarATipo(tenantId, TipoAlertaWhatsApp.PLAN_VENCIMIENTO, msg);
    }

    public void enviarResumenMensual(String tenantId, String nombreTenant,
                                      int totalLotes, int completados, int anio, int mes) {
        String msg = "📊 *Resumen mensual — " + nombreTenant + "*\n" +
                "Período: " + mes + "/" + anio + "\n" +
                "Lotes producidos: *" + totalLotes + "*\n" +
                "Completados: *" + completados + "*\n\n" +
                "👉 Ver reportes completos en Zymos";
        enviarATipo(tenantId, TipoAlertaWhatsApp.RESUMEN_MENSUAL, msg);
    }
}
