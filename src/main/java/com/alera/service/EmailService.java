package com.alera.service;

import com.alera.model.Equipo;
import com.alera.model.InsumoInventario;
import com.alera.model.Tenant;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // null si SMTP_HOST no está configurado — Spring no crea el bean
    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Value("${app.alert.from:noreply@alera.app}")
    private String fromAddress;

    @Value("${app.alert.base-url:http://localhost:8080}")
    private String baseUrl;

    public boolean mailConfigurado() {
        return mailSender != null;
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
            Context ctx = new Context();
            ctx.setVariable("tenant",                tenant);
            ctx.setVariable("bajoStock",             bajoStock);
            ctx.setVariable("proximosAVencer",       proximosAVencer);
            ctx.setVariable("mantenimientoPendiente",mantenimientoPendiente);
            ctx.setVariable("baseUrl",               baseUrl);
            ctx.setVariable("fecha",                 LocalDate.now().format(FMT));
            ctx.setVariable("hoy",                   LocalDate.now());

            String html = templateEngine.process("emails/alertas", ctx);

            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(tenant.getEmailAdmin());
            helper.setSubject("[" + tenant.getName() + "] Alertas del día — " + LocalDate.now().format(FMT));
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
            Context ctx = new Context();
            ctx.setVariable("tenant",   tenant);
            ctx.setVariable("username", username);
            ctx.setVariable("password", password);
            ctx.setVariable("appUrl",   baseUrl);

            String html = templateEngine.process("emails/bienvenida", ctx);

            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(tenant.getEmailAdmin());
            helper.setSubject("¡Bienvenido a " + tenant.getName() + "! — Tus credenciales de acceso");
            helper.setText(html, true);
            mailSender.send(msg);

            log.info("Email de bienvenida enviado a {} para tenant '{}'", tenant.getEmailAdmin(), tenant.getSubdomain());
            return true;

        } catch (Exception e) {
            log.error("Error al enviar bienvenida al tenant '{}': {}", tenant.getSubdomain(), e.getMessage());
            return false;
        }
    }

    /** Formatea días restantes hasta una fecha de vencimiento. */
    public static long diasHasta(LocalDate fecha) {
        return ChronoUnit.DAYS.between(LocalDate.now(), fecha);
    }
}