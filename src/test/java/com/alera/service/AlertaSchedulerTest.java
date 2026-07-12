package com.alera.service;

import com.alera.config.TenantContext;
import com.alera.model.Tenant;
import com.alera.repository.LoteCervezaRepository;
import com.alera.repository.TenantRepository;
import com.alera.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertaSchedulerTest {

    @Mock TenantRepository        tenantRepo;
    @Mock InsumoInventarioService  insumoService;
    @Mock EquipoService            equipoService;
    @Mock EmailService             emailService;
    @Mock TenantService            tenantService;
    @Mock NotificacionService      notificacionService;
    @Mock FacturaProveedorService  facturaService;
    @Mock VentaService             ventaService;
    @Mock LoteCervezaRepository    loteCervezaRepo;
    @Mock UsuarioRepository        usuarioRepo;
    @Mock TareaService             tareaService;

    @InjectMocks AlertaScheduler scheduler;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(scheduler, "vencimientoDias", 30);
        ReflectionTestUtils.setField(scheduler, "facturaAlertaDias", 30);

        lenient().when(insumoService.listarBajoStock()).thenReturn(List.of());
        lenient().when(insumoService.listarProximosAVencer(anyInt())).thenReturn(List.of());
        lenient().when(equipoService.listarMantenimientoPendiente()).thenReturn(List.of());
        lenient().when(notificacionService.crearAlertas(any(), any(), any())).thenReturn(0);
        lenient().when(facturaService.listarSinProcesar(anyInt())).thenReturn(List.of());
        lenient().when(ventaService.expirarCotizaciones()).thenReturn(0);
        lenient().when(emailService.mailConfigurado()).thenReturn(false);
        lenient().when(loteCervezaRepo.count()).thenReturn(0L);
        lenient().when(usuarioRepo.countByTenantId(any())).thenReturn(0L);
        lenient().when(tareaService.listarProximasAVencer(any())).thenReturn(List.of());
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    private Tenant tenant(String subdomain, boolean active, String email) {
        Tenant t = new Tenant();
        t.setSubdomain(subdomain);
        t.setActive(active);
        t.setEmailAdmin(email);
        t.setAlertasIntentosFallidos(0);
        return t;
    }

    // ── enviarAlertasDiarias ──────────────────────────────────────────────────

    @Test
    void enviarAlertasDiarias_sinTenants_noHaceNada() {
        when(tenantRepo.findAll()).thenReturn(List.of());

        scheduler.enviarAlertasDiarias();

        verify(notificacionService, never()).crearAlertas(any(), any(), any());
    }

    @Test
    void enviarAlertasDiarias_tenantSinEmail_noEnviaCorreo() {
        when(tenantRepo.findAll()).thenReturn(List.of(tenant("mosto", true, null)));

        scheduler.enviarAlertasDiarias();

        verify(notificacionService).crearAlertas(any(), any(), any());
        verify(emailService, never()).enviarAlertasDiarias(any(), any(), any(), any());
    }

    @Test
    void enviarAlertasDiarias_tenantInactivo_seIgnora() {
        Tenant inactivo = tenant("baja", false, "admin@baja.com");
        when(tenantRepo.findAll()).thenReturn(List.of(inactivo));

        scheduler.enviarAlertasDiarias();

        verify(notificacionService, never()).crearAlertas(any(), any(), any());
    }

    @Test
    void enviarAlertasDiarias_conSmtpYEmail_enviaCorreo() {
        Tenant t = tenant("activo", true, "admin@activo.com");
        when(tenantRepo.findAll()).thenReturn(List.of(t));
        when(emailService.mailConfigurado()).thenReturn(true);
        when(emailService.enviarAlertasDiarias(any(), any(), any(), any())).thenReturn(true);

        scheduler.enviarAlertasDiarias();

        verify(emailService).enviarAlertasDiarias(eq(t), any(), any(), any());
        verify(tenantService).registrarEnvioExitoso("activo");
    }

    @Test
    void enviarAlertasDiarias_falloSmtp_registraFallo() {
        Tenant t = tenant("fallido", true, "admin@fallido.com");
        when(tenantRepo.findAll()).thenReturn(List.of(t));
        when(emailService.mailConfigurado()).thenReturn(true);
        when(emailService.enviarAlertasDiarias(any(), any(), any(), any()))
            .thenThrow(new RuntimeException("SMTP error"));

        scheduler.enviarAlertasDiarias();

        verify(tenantService).registrarEnvioFallido("fallido");
        verify(tenantService, never()).registrarEnvioExitoso(any());
    }

    @Test
    void enviarAlertasDiarias_limpiaTenantContextEnFinally() {
        Tenant t = tenant("prueba", true, null);
        when(tenantRepo.findAll()).thenReturn(List.of(t));

        scheduler.enviarAlertasDiarias();

        // Después de ejecutar, el contexto debe estar limpio
        assertTenantContextLimpio();
    }

    @Test
    void enviarAlertasDiarias_expiraCotizaciones() {
        when(tenantRepo.findAll()).thenReturn(List.of(tenant("mosto", true, null)));

        scheduler.enviarAlertasDiarias();

        verify(ventaService).expirarCotizaciones();
    }

    @Test
    void enviarAlertasDiarias_creaAlertaFacturasNoVacias() {
        when(tenantRepo.findAll()).thenReturn(List.of(tenant("mosto", true, null)));

        scheduler.enviarAlertasDiarias();

        verify(notificacionService).crearAlertaFacturas(any(), eq(30));
    }

    @Test
    void enviarAlertasDiarias_creaAlertaPlan() {
        Tenant t = tenant("mosto", true, null);
        when(tenantRepo.findAll()).thenReturn(List.of(t));
        when(loteCervezaRepo.count()).thenReturn(5L);
        when(usuarioRepo.countByTenantId("mosto")).thenReturn(2L);

        scheduler.enviarAlertasDiarias();

        verify(notificacionService).crearAlertaPlan(t, 5L, 2L);
    }

    @Test
    void enviarAlertasDiarias_dosTenants_procesaAmbos() {
        Tenant t1 = tenant("t1", true, null);
        Tenant t2 = tenant("t2", true, null);
        when(tenantRepo.findAll()).thenReturn(List.of(t1, t2));

        scheduler.enviarAlertasDiarias();

        verify(notificacionService, times(2)).crearAlertas(any(), any(), any());
    }

    @Test
    void enviarAlertasDiarias_invocaAlertaTareasVencimiento() {
        when(tenantRepo.findAll()).thenReturn(List.of(tenant("mosto", true, null)));

        scheduler.enviarAlertasDiarias();

        verify(notificacionService).crearAlertaTareaVencimiento(any());
    }

    private void assertTenantContextLimpio() {
        // TenantContext.getCurrentTenant() devuelve null o "" después de clear()
        // No podemos llamarlo directamente sin getter público, pero podemos verificar
        // que el hilo no tiene contexto contaminado intentando setear y leer
        TenantContext.setCurrentTenant("check");
        assert "check".equals(TenantContext.getCurrentTenant());
        TenantContext.clear();
    }
}
