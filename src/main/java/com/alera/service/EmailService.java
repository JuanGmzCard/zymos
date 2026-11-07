package com.alera.service;

import com.alera.model.Equipo;
import com.alera.model.InsumoInventario;
import com.alera.model.LoteCerveza;
import com.alera.model.Tenant;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // null si SMTP_HOST no está configurado — Spring no crea el bean
    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Autowired
    private MessageSource messageSource;

    @Value("${app.alert.from:noreply@alera.app}")
    private String fromAddress;

    @Value("${app.alert.base-url:http://localhost:8080}")
    private String baseUrl;

    public boolean mailConfigurado() {
        return mailSender != null;
    }

    private Locale localeFrom(Tenant tenant) {
        String tag = tenant.getLocale();
        return (tag != null && !tag.isBlank()) ? Locale.forLanguageTag(tag) : Locale.forLanguageTag("es");
    }

    private String t(String key, Locale locale, Object... args) {
        return messageSource.getMessage(key, args.length == 0 ? null : args, key, locale);
    }

    /**
     * Envía el resumen diario de alertas al email del tenant.
     * @return true si el email fue enviado, false si no hay alertas o no hay configuración.
     */
    public boolean enviarAlertasDiarias(Tenant tenant,
                                         List<InsumoInventario> bajoStock,
                                         List<InsumoInventario> proximosAVencer,
                                         List<Equipo> mantenimientoPendiente) {
        if (mailSender == null) return false;
        if (tenant.getEmailAdmin() == null || tenant.getEmailAdmin().isBlank()) return false;
        if (bajoStock.isEmpty() && proximosAVencer.isEmpty() && mantenimientoPendiente.isEmpty()) return false;

        try {
            Locale locale = localeFrom(tenant);
            String fechaStr = LocalDate.now().format(FMT);

            Context ctx = new Context(locale);
            ctx.setVariable("tenant",                tenant);
            ctx.setVariable("bajoStock",             bajoStock);
            ctx.setVariable("proximosAVencer",       proximosAVencer);
            ctx.setVariable("mantenimientoPendiente",mantenimientoPendiente);
            ctx.setVariable("baseUrl",               baseUrl);
            ctx.setVariable("fecha",                 fechaStr);
            ctx.setVariable("hoy",                   LocalDate.now());

            String html = templateEngine.process("emails/alertas", ctx);

            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(tenant.getEmailAdmin());
            helper.setSubject(t("email.subject.alertas", locale, tenant.getName(), fechaStr));
            helper.setText(html, true);
            mailSender.send(msg);

            log.info("Alertas enviadas a {} para tenant '{}'", tenant.getEmailAdmin(), tenant.getSubdomain());
            return true;

        } catch (Exception e) {
            log.error("Error al enviar alertas al tenant '{}': {}", tenant.getSubdomain(), e.getMessage());
            throw new RuntimeException("Fallo SMTP para tenant " + tenant.getSubdomain(), e);
        }
    }

    /** Envía un email de prueba para verificar que SMTP y la dirección están bien configurados. */
    public String enviarEmailPrueba(String destinatario, String tenantName) {
        if (mailSender == null) return "SMTP no configurado en el servidor";
        if (destinatario == null || destinatario.isBlank()) return "Ingresa un email de destino";
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(destinatario);
            helper.setSubject("[" + tenantName + "] Email de prueba — Zymos");
            helper.setText(
                "<div style='font-family:sans-serif;padding:24px;'>" +
                "<h2 style='color:#364318;'>✓ Configuración de email correcta</h2>" +
                "<p>Este es un email de prueba enviado desde <strong>" + tenantName + "</strong>.</p>" +
                "<p style='color:#666;font-size:.9em;'>Si recibes este mensaje, el SMTP está configurado correctamente " +
                "y las alertas diarias llegarán a esta dirección.</p>" +
                "</div>", true);
            mailSender.send(msg);
            log.info("Email de prueba enviado a {} para tenant '{}'", destinatario, tenantName);
            return null; // null = éxito
        } catch (Exception e) {
            log.error("Error al enviar email de prueba a {}: {}", destinatario, e.getMessage());
            return e.getMessage();
        }
    }

    /**
     * Envía el email de bienvenida al crear el primer usuario de un tenant.
     * @return true si se envió, false si SMTP no está configurado o falta emailAdmin.
     */
    public boolean enviarBienvenida(Tenant tenant, String username, String password) {
        if (mailSender == null) return false;
        if (tenant.getEmailAdmin() == null || tenant.getEmailAdmin().isBlank()) return false;

        try {
            Locale locale = localeFrom(tenant);

            Context ctx = new Context(locale);
            ctx.setVariable("tenant",   tenant);
            ctx.setVariable("username", username);
            ctx.setVariable("password", password);
            ctx.setVariable("appUrl",   baseUrl);

            String html = templateEngine.process("emails/bienvenida", ctx);

            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(tenant.getEmailAdmin());
            helper.setSubject(t("email.subject.bienvenida", locale, tenant.getName()));
            helper.setText(html, true);
            mailSender.send(msg);

            log.info("Email de bienvenida enviado a {} para tenant '{}'", tenant.getEmailAdmin(), tenant.getSubdomain());
            return true;

        } catch (Exception e) {
            log.error("Error al enviar bienvenida al tenant '{}': {}", tenant.getSubdomain(), e.getMessage());
            return false;
        }
    }

    /**
     * Variante de bienvenida usada tras una compra online: igual que {@link #enviarBienvenida} pero
     * con la URL de login del subdominio recién provisionado en lugar del {@code baseUrl} global.
     */
    public boolean enviarBienvenidaCompra(Tenant tenant, String username, String password, String loginUrl) {
        if (mailSender == null) return false;
        if (tenant.getEmailAdmin() == null || tenant.getEmailAdmin().isBlank()) return false;

        try {
            Locale locale = localeFrom(tenant);
            Context ctx = new Context(locale);
            ctx.setVariable("tenant",   tenant);
            ctx.setVariable("username", username);
            ctx.setVariable("password", password);
            ctx.setVariable("appUrl",   loginUrl);

            String html = templateEngine.process("emails/bienvenida", ctx);

            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(tenant.getEmailAdmin());
            helper.setSubject(t("email.subject.bienvenida", locale, tenant.getName()));
            helper.setText(html, true);
            mailSender.send(msg);

            log.info("Email de bienvenida (compra) enviado a {} para tenant '{}'", tenant.getEmailAdmin(), tenant.getSubdomain());
            return true;
        } catch (Exception e) {
            log.error("Error al enviar bienvenida (compra) al tenant '{}': {}", tenant.getSubdomain(), e.getMessage());
            return false;
        }
    }

    /**
     * Notifica al admin del tenant cuando un lote cambia de fase.
     * No lanza excepción si SMTP falla — el cambio de fase ya fue persistido.
     *
     * @param evento  código del evento historial, p.ej. "FERM_INICIADA"
     * @param usuario username que realizó el cambio
     */
    public void enviarCambioFaseLote(Tenant tenant, LoteCerveza lote,
                                      String evento, String usuario) {
        if (mailSender == null) return;
        if (tenant.getEmailAdmin() == null || tenant.getEmailAdmin().isBlank()) return;
        if ("EDITADO".equals(evento)) return;

        try {
            Locale locale = localeFrom(tenant);
            String eventoLabel = t("traz.hist.accion." + evento, locale);
            String fecha = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            String detailUrl = baseUrl + "/ver/" + lote.getId();

            Context ctx = new Context(locale);
            ctx.setVariable("tenant",      tenant);
            ctx.setVariable("lote",        lote);
            ctx.setVariable("evento",      evento);
            ctx.setVariable("eventoLabel", eventoLabel);
            ctx.setVariable("usuario",     usuario);
            ctx.setVariable("fecha",       fecha);
            ctx.setVariable("detailUrl",   detailUrl);
            ctx.setVariable("baseUrl",     baseUrl);

            String html = templateEngine.process("emails/cambio-fase", ctx);

            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(tenant.getEmailAdmin());
            helper.setSubject(t("email.subject.fase.lote", locale,
                    tenant.getName(), lote.getCodigoLote(), eventoLabel));
            helper.setText(html, true);
            mailSender.send(msg);

            log.info("Email cambio fase '{}' para lote {} enviado a {}",
                    evento, lote.getCodigoLote(), tenant.getEmailAdmin());
        } catch (Exception e) {
            log.warn("No se pudo enviar email de cambio de fase para lote {}: {}",
                    lote.getCodigoLote(), e.getMessage());
        }
    }

    public boolean enviarResumenMensual(Tenant tenant, List<LoteCerveza> lotesMes,
                                         int anio, int mes) {
        if (mailSender == null) return false;
        if (tenant.getEmailAdmin() == null || tenant.getEmailAdmin().isBlank()) return false;

        try {
            Locale locale = localeFrom(tenant);
            java.time.YearMonth ym = java.time.YearMonth.of(anio, mes);
            String periodo = ym.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", locale));

            long completados = lotesMes.stream().filter(LoteCerveza::isCompletado).count();
            long activos     = lotesMes.stream().filter(l -> !l.isCompletado()).count();
            java.math.BigDecimal litrosTotales = lotesMes.stream()
                    .map(LoteCerveza::getLitrosFinales)
                    .filter(java.util.Objects::nonNull)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

            Context ctx = new Context(locale);
            ctx.setVariable("tenant",        tenant);
            ctx.setVariable("periodo",       periodo);
            ctx.setVariable("lotesMes",      lotesMes);
            ctx.setVariable("completados",   completados);
            ctx.setVariable("activos",       activos);
            ctx.setVariable("litrosTotales", litrosTotales);
            ctx.setVariable("baseUrl",       baseUrl);

            String html = templateEngine.process("emails/resumen-mensual", ctx);

            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(tenant.getEmailAdmin());
            helper.setSubject(t("email.subject.resumen.mensual", locale, tenant.getName(), periodo));
            helper.setText(html, true);
            mailSender.send(msg);

            log.info("Resumen mensual ({}) enviado a {} para tenant '{}'",
                    periodo, tenant.getEmailAdmin(), tenant.getSubdomain());
            return true;
        } catch (Exception e) {
            log.warn("No se pudo enviar resumen mensual para tenant '{}': {}",
                    tenant.getSubdomain(), e.getMessage());
            return false;
        }
    }

    /**
     * Envía el email de onboarding del día 1, 3 o 7 tras el inicio del plan.
     */
    public boolean enviarOnboarding(Tenant tenant, int dia) {
        if (mailSender == null) return false;
        if (tenant.getEmailAdmin() == null || tenant.getEmailAdmin().isBlank()) return false;

        try {
            Locale locale = localeFrom(tenant);
            Context ctx = new Context(locale);
            ctx.setVariable("tenant",  tenant);
            ctx.setVariable("dia",     dia);
            ctx.setVariable("appUrl",  baseUrl);

            String html = templateEngine.process("emails/onboarding", ctx);

            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(tenant.getEmailAdmin());
            helper.setSubject(t("email.subject.onboarding." + dia, locale, tenant.getName()));
            helper.setText(html, true);
            mailSender.send(msg);

            log.info("Email onboarding día {} enviado a {} para tenant '{}'",
                    dia, tenant.getEmailAdmin(), tenant.getSubdomain());
            return true;
        } catch (Exception e) {
            log.warn("No se pudo enviar email onboarding día {} para tenant '{}': {}",
                    dia, tenant.getSubdomain(), e.getMessage());
            return false;
        }
    }

    /**
     * Envía aviso de expiración inminente del plan (días restantes = 4 o 1).
     */
    public boolean enviarExpiracionTrial(Tenant tenant, long diasRestantes) {
        if (mailSender == null) return false;
        if (tenant.getEmailAdmin() == null || tenant.getEmailAdmin().isBlank()) return false;
        if (tenant.getPlanFin() == null) return false;

        try {
            Locale locale = localeFrom(tenant);
            String planFinStr = tenant.getPlanFin().format(FMT);

            Context ctx = new Context(locale);
            ctx.setVariable("tenant",        tenant);
            ctx.setVariable("diasRestantes", diasRestantes);
            ctx.setVariable("planFin",       planFinStr);
            ctx.setVariable("planesUrl",     baseUrl + "/planes");
            ctx.setVariable("appUrl",        baseUrl);

            String html = templateEngine.process("emails/trial-expiracion", ctx);

            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(tenant.getEmailAdmin());
            helper.setSubject(t("email.subject.trial.expira", locale, diasRestantes, tenant.getName()));
            helper.setText(html, true);
            mailSender.send(msg);

            log.info("Email expiración ({} días) enviado a {} para tenant '{}'",
                    diasRestantes, tenant.getEmailAdmin(), tenant.getSubdomain());
            return true;
        } catch (Exception e) {
            log.warn("No se pudo enviar email expiración para tenant '{}': {}",
                    tenant.getSubdomain(), e.getMessage());
            return false;
        }
    }

    /** Formatea días restantes hasta una fecha de vencimiento. */
    public static long diasHasta(LocalDate fecha) {
        return ChronoUnit.DAYS.between(LocalDate.now(), fecha);
    }
}
