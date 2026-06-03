package com.alera.service;

import com.alera.model.Equipo;
import com.alera.model.InsumoInventario;
import com.alera.model.Tenant;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests de EmailService. Usa ReflectionTestUtils para inyectar dependencias de campo
 * (@Autowired) y MimeMessage con sesión null para que MimeMessageHelper opere sin SMTP real.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService")
class EmailServiceTest {

    @Mock JavaMailSender      mailSender;
    @Mock SpringTemplateEngine templateEngine;

    EmailService service;
    EmailService serviceSinSmtp; // sin mailSender — simula SMTP no configurado

    @BeforeEach
    void setUp() {
        service = new EmailService();
        ReflectionTestUtils.setField(service, "mailSender",   mailSender);
        ReflectionTestUtils.setField(service, "templateEngine", templateEngine);
        ReflectionTestUtils.setField(service, "fromAddress",  "noreply@alera.app");
        ReflectionTestUtils.setField(service, "baseUrl",      "http://localhost:8080");

        serviceSinSmtp = new EmailService();
        ReflectionTestUtils.setField(serviceSinSmtp, "templateEngine", templateEngine);
        ReflectionTestUtils.setField(serviceSinSmtp, "fromAddress",  "noreply@alera.app");
        ReflectionTestUtils.setField(serviceSinSmtp, "baseUrl",      "http://localhost:8080");
        // mailSender queda null — simula que SMTP_HOST no está configurado
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private MimeMessage mimeMessageReal() throws Exception {
        return new MimeMessage((jakarta.mail.Session) null);
    }

    private Tenant tenant(String email) {
        Tenant t = new Tenant();
        t.setSubdomain("mosto");
        t.setName("Mosto Cervecería");
        t.setEmailAdmin(email);
        t.setActive(true);
        t.setColorNavbar("#242E0D"); t.setColorPrimary("#364318");
        t.setColorAccent("#C9A028"); t.setColorAccentHover("#E0B840");
        t.setColorCream("#F5EDD0");  t.setColorBodyBg("#F0EDE2");
        return t;
    }

    // ── mailConfigurado ───────────────────────────────────────────────

    @Test
    @DisplayName("mailConfigurado retorna true cuando JavaMailSender está disponible")
    void mailConfigurado_conSmtp_retornaTrue() {
        assertThat(service.mailConfigurado()).isTrue();
    }

    @Test
    @DisplayName("mailConfigurado retorna false cuando JavaMailSender es null")
    void mailConfigurado_sinSmtp_retornaFalse() {
        assertThat(serviceSinSmtp.mailConfigurado()).isFalse();
    }

    // ── enviarAlertasDiarias ──────────────────────────────────────────

    @Test
    @DisplayName("enviarAlertasDiarias retorna false cuando SMTP no está configurado")
    void enviarAlertasDiarias_sinSmtp_retornaFalse() {
        Tenant t = tenant("admin@mosto.com");
        InsumoInventario i = mock(InsumoInventario.class);

        boolean resultado = serviceSinSmtp.enviarAlertasDiarias(t, List.of(i), List.of(), List.of());

        assertThat(resultado).isFalse();
        verifyNoInteractions(mailSender);
    }

    @Test
    @DisplayName("enviarAlertasDiarias retorna false cuando emailAdmin es null")
    void enviarAlertasDiarias_emailAdminNull_retornaFalse() {
        Tenant t = tenant(null);
        InsumoInventario i = mock(InsumoInventario.class);

        assertThat(service.enviarAlertasDiarias(t, List.of(i), List.of(), List.of())).isFalse();
        verifyNoInteractions(mailSender);
    }

    @Test
    @DisplayName("enviarAlertasDiarias retorna false cuando emailAdmin está vacío")
    void enviarAlertasDiarias_emailAdminVacio_retornaFalse() {
        Tenant t = tenant("   ");
        InsumoInventario i = mock(InsumoInventario.class);

        assertThat(service.enviarAlertasDiarias(t, List.of(i), List.of(), List.of())).isFalse();
        verifyNoInteractions(mailSender);
    }

    @Test
    @DisplayName("enviarAlertasDiarias retorna false cuando no hay alertas")
    void enviarAlertasDiarias_sinAlertas_retornaFalse() {
        Tenant t = tenant("admin@mosto.com");

        boolean resultado = service.enviarAlertasDiarias(t, List.of(), List.of(), List.of());

        assertThat(resultado).isFalse();
        verifyNoInteractions(mailSender);
    }

    @Test
    @DisplayName("enviarAlertasDiarias envía email y retorna true cuando hay alertas de bajo stock")
    void enviarAlertasDiarias_conBajoStock_enviaYRetornaTrue() throws Exception {
        Tenant t = tenant("admin@mosto.com");
        when(templateEngine.process(eq("emails/alertas"), any())).thenReturn("<html>alertas</html>");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessageReal());

        boolean resultado = service.enviarAlertasDiarias(
                t, List.of(mock(InsumoInventario.class)), List.of(), List.of());

        assertThat(resultado).isTrue();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("enviarAlertasDiarias envía email cuando solo hay vencimientos próximos")
    void enviarAlertasDiarias_conVencimientos_enviaEmail() throws Exception {
        Tenant t = tenant("admin@mosto.com");
        when(templateEngine.process(eq("emails/alertas"), any())).thenReturn("<html>alertas</html>");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessageReal());

        boolean resultado = service.enviarAlertasDiarias(
                t, List.of(), List.of(mock(InsumoInventario.class)), List.of());

        assertThat(resultado).isTrue();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("enviarAlertasDiarias envía email cuando solo hay mantenimiento pendiente")
    void enviarAlertasDiarias_conMantenimiento_enviaEmail() throws Exception {
        Tenant t = tenant("admin@mosto.com");
        when(templateEngine.process(eq("emails/alertas"), any())).thenReturn("<html>alertas</html>");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessageReal());

        boolean resultado = service.enviarAlertasDiarias(
                t, List.of(), List.of(), List.of(mock(Equipo.class)));

        assertThat(resultado).isTrue();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("enviarAlertasDiarias lanza RuntimeException cuando falla el envío SMTP")
    void enviarAlertasDiarias_falloSmtp_lanzaRuntimeException() throws Exception {
        Tenant t = tenant("admin@mosto.com");
        when(templateEngine.process(eq("emails/alertas"), any())).thenReturn("<html>alertas</html>");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessageReal());
        doThrow(new MailSendException("Connection refused"))
                .when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() ->
                service.enviarAlertasDiarias(t, List.of(mock(InsumoInventario.class)), List.of(), List.of()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Fallo SMTP")
                .hasMessageContaining("mosto");
    }

    @Test
    @DisplayName("enviarAlertasDiarias pasa las variables correctas al templateEngine")
    void enviarAlertasDiarias_pasaVariablesAlTemplate() throws Exception {
        Tenant t = tenant("admin@mosto.com");
        when(templateEngine.process(eq("emails/alertas"), any())).thenReturn("<html>ok</html>");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessageReal());

        service.enviarAlertasDiarias(t, List.of(mock(InsumoInventario.class)), List.of(), List.of());

        verify(templateEngine).process(eq("emails/alertas"), argThat(ctx ->
                ctx.containsVariable("tenant") &&
                ctx.containsVariable("bajoStock") &&
                ctx.containsVariable("proximosAVencer") &&
                ctx.containsVariable("mantenimientoPendiente") &&
                ctx.containsVariable("baseUrl") &&
                ctx.containsVariable("fecha") &&
                ctx.containsVariable("hoy")
        ));
    }

    // ── enviarBienvenida ──────────────────────────────────────────────

    @Test
    @DisplayName("enviarBienvenida retorna false cuando SMTP no está configurado")
    void enviarBienvenida_sinSmtp_retornaFalse() {
        assertThat(serviceSinSmtp.enviarBienvenida(tenant("admin@mosto.com"), "admin", "pass123")).isFalse();
        verifyNoInteractions(mailSender);
    }

    @Test
    @DisplayName("enviarBienvenida retorna false cuando emailAdmin es null")
    void enviarBienvenida_emailAdminNull_retornaFalse() {
        assertThat(service.enviarBienvenida(tenant(null), "admin", "pass123")).isFalse();
        verifyNoInteractions(mailSender);
    }

    @Test
    @DisplayName("enviarBienvenida retorna false cuando emailAdmin está vacío")
    void enviarBienvenida_emailAdminVacio_retornaFalse() {
        assertThat(service.enviarBienvenida(tenant("  "), "admin", "pass123")).isFalse();
        verifyNoInteractions(mailSender);
    }

    @Test
    @DisplayName("enviarBienvenida envía email y retorna true cuando todo está configurado")
    void enviarBienvenida_exitoso_enviaYRetornaTrue() throws Exception {
        when(templateEngine.process(eq("emails/bienvenida"), any())).thenReturn("<html>bienvenida</html>");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessageReal());

        boolean resultado = service.enviarBienvenida(tenant("admin@mosto.com"), "admin", "Pass1234");

        assertThat(resultado).isTrue();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("enviarBienvenida pasa username, password y tenant al templateEngine")
    void enviarBienvenida_pasaVariablesAlTemplate() throws Exception {
        when(templateEngine.process(eq("emails/bienvenida"), any())).thenReturn("<html>ok</html>");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessageReal());

        service.enviarBienvenida(tenant("admin@mosto.com"), "juancho", "MiPass1");

        verify(templateEngine).process(eq("emails/bienvenida"), argThat(ctx ->
                ctx.containsVariable("tenant") &&
                ctx.containsVariable("username") &&
                ctx.containsVariable("password") &&
                ctx.containsVariable("appUrl")
        ));
    }

    @Test
    @DisplayName("enviarBienvenida retorna false (no lanza) cuando falla el envío SMTP")
    void enviarBienvenida_falloSmtp_retornaFalse() throws Exception {
        when(templateEngine.process(eq("emails/bienvenida"), any())).thenReturn("<html>ok</html>");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessageReal());
        doThrow(new MailSendException("Timeout")).when(mailSender).send(any(MimeMessage.class));

        assertThat(service.enviarBienvenida(tenant("admin@mosto.com"), "admin", "pass123")).isFalse();
    }

    // ── enviarEmailPrueba ─────────────────────────────────────────────

    @Test
    @DisplayName("enviarEmailPrueba retorna mensaje de error cuando SMTP no está configurado")
    void enviarEmailPrueba_sinSmtp_retornaMensajeError() {
        String resultado = serviceSinSmtp.enviarEmailPrueba("admin@mosto.com", "Mosto");

        assertThat(resultado).isEqualTo("SMTP no configurado en el servidor");
        verifyNoInteractions(mailSender);
    }

    @Test
    @DisplayName("enviarEmailPrueba retorna error cuando destinatario es null")
    void enviarEmailPrueba_destinatarioNull_retornaError() {
        String resultado = service.enviarEmailPrueba(null, "Mosto");

        assertThat(resultado).isEqualTo("Ingresa un email de destino");
        verifyNoInteractions(mailSender);
    }

    @Test
    @DisplayName("enviarEmailPrueba retorna error cuando destinatario está vacío")
    void enviarEmailPrueba_destinatarioVacio_retornaError() {
        String resultado = service.enviarEmailPrueba("   ", "Mosto");

        assertThat(resultado).isEqualTo("Ingresa un email de destino");
        verifyNoInteractions(mailSender);
    }

    @Test
    @DisplayName("enviarEmailPrueba retorna null (éxito) cuando el email se envía correctamente")
    void enviarEmailPrueba_exitoso_retornaNull() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessageReal());

        String resultado = service.enviarEmailPrueba("admin@mosto.com", "Mosto Cervecería");

        assertThat(resultado).isNull();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("enviarEmailPrueba retorna el mensaje de error cuando falla el envío SMTP")
    void enviarEmailPrueba_falloSmtp_retornaMensajeError() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessageReal());
        doThrow(new MailSendException("Timeout"))
                .when(mailSender).send(any(MimeMessage.class));

        String resultado = service.enviarEmailPrueba("admin@mosto.com", "Mosto");

        assertThat(resultado).isNotNull();
        assertThat(resultado).contains("Timeout");
    }

    // ── diasHasta ─────────────────────────────────────────────────────

    @Test
    @DisplayName("diasHasta retorna 0 para la fecha de hoy")
    void diasHasta_hoy_retornaCero() {
        assertThat(EmailService.diasHasta(LocalDate.now())).isZero();
    }

    @Test
    @DisplayName("diasHasta retorna positivo para fechas futuras")
    void diasHasta_futuro_retornaPositivo() {
        assertThat(EmailService.diasHasta(LocalDate.now().plusDays(7))).isEqualTo(7);
    }

    @Test
    @DisplayName("diasHasta retorna negativo para fechas pasadas")
    void diasHasta_pasado_retornaNegativo() {
        assertThat(EmailService.diasHasta(LocalDate.now().minusDays(3))).isEqualTo(-3);
    }
}
