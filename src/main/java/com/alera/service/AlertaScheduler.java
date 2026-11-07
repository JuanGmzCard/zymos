package com.alera.service;

import com.alera.config.TenantContext;
import com.alera.model.Equipo;
import com.alera.model.FacturaProveedor;
import com.alera.model.InsumoInventario;
import com.alera.model.LoteCerveza;
import com.alera.model.Tarea;
import com.alera.model.Tenant;
import com.alera.repository.LoteCervezaRepository;
import com.alera.repository.TenantRepository;
import com.alera.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class AlertaScheduler {

    private static final Logger log = LoggerFactory.getLogger(AlertaScheduler.class);
    private static final int UMBRAL_WARN = 3;

    private final TenantRepository        tenantRepo;
    private final InsumoInventarioService  insumoService;
    private final EquipoService            equipoService;
    private final EmailService             emailService;
    private final TenantService            tenantService;
    private final NotificacionService      notificacionService;
    private final FacturaProveedorService  facturaService;
    private final VentaService             ventaService;
    private final LoteCervezaRepository    loteCervezaRepo;
    private final UsuarioRepository        usuarioRepo;
    private final TareaService             tareaService;
    private final WhatsAppService          whatsAppService;

    @Value("${app.alert.vencimiento-dias:30}")
    private int vencimientoDias;

    @Value("${app.facturas.alerta-dias:30}")
    private int facturaAlertaDias;

    public AlertaScheduler(TenantRepository tenantRepo,
                            InsumoInventarioService insumoService,
                            EquipoService equipoService,
                            EmailService emailService,
                            TenantService tenantService,
                            NotificacionService notificacionService,
                            FacturaProveedorService facturaService,
                            VentaService ventaService,
                            LoteCervezaRepository loteCervezaRepo,
                            UsuarioRepository usuarioRepo,
                            TareaService tareaService,
                            WhatsAppService whatsAppService) {
        this.tenantRepo          = tenantRepo;
        this.insumoService       = insumoService;
        this.equipoService       = equipoService;
        this.emailService        = emailService;
        this.tenantService       = tenantService;
        this.notificacionService = notificacionService;
        this.facturaService      = facturaService;
        this.ventaService        = ventaService;
        this.loteCervezaRepo     = loteCervezaRepo;
        this.usuarioRepo         = usuarioRepo;
        this.tareaService        = tareaService;
        this.whatsAppService     = whatsAppService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void inicializarNotificaciones() {
        log.info("Inicializando notificaciones in-app al arrancar...");
        enviarAlertasDiarias();
    }

    @Scheduled(cron = "${app.alert.cron:0 0 8 * * MON-FRI}")
    public void enviarAlertasDiarias() {
        List<Tenant> tenants = tenantRepo.findAll().stream()
                .filter(Tenant::isActive)
                .toList();

        if (tenants.isEmpty()) return;

        int notifs   = 0;
        int enviados = 0;

        for (Tenant tenant : tenants) {
            try {
                TenantContext.setCurrentTenant(tenant.getSubdomain());

                List<InsumoInventario> bajoStock       = insumoService.listarBajoStock();
                List<InsumoInventario> proximosAVencer = insumoService.listarProximosAVencer(vencimientoDias);
                List<Equipo>           mantenimiento   = equipoService.listarMantenimientoPendiente();

                // Notificaciones in-app — siempre, independiente de SMTP
                notifs += notificacionService.crearAlertas(bajoStock, proximosAVencer, mantenimiento);

                List<FacturaProveedor> sinProcesar = facturaService.listarSinProcesar(facturaAlertaDias);
                notificacionService.crearAlertaFacturas(sinProcesar, facturaAlertaDias);

                // Estado del plan: vencimiento y límites de uso (lotes/usuarios)
                long totalLotes    = loteCervezaRepo.count();
                long totalUsuarios = usuarioRepo.countByTenantId(tenant.getSubdomain());
                notificacionService.crearAlertaPlan(tenant, totalLotes, totalUsuarios);

                // Tareas que vencen mañana
                List<Tarea> tareasVencen = tareaService.listarProximasAVencer(LocalDate.now().plusDays(1));
                notificacionService.crearAlertaTareaVencimiento(tareasVencen);

                // Lotes con fecha ideal de fase superada
                List<LoteCerveza> fasesVencidas = loteCervezaRepo.findFasesVencidas(LocalDate.now());
                notificacionService.crearAlertaFasesVencidas(fasesVencidas);

                // Expirar cotizaciones vencidas
                int expiradas = ventaService.expirarCotizaciones();
                if (expiradas > 0) {
                    log.info("Tenant '{}': {} cotización(es) expirada(s)", tenant.getSubdomain(), expiradas);
                }

                // WhatsApp — bajo stock, vencimientos, mantenimiento
                whatsAppService.notificarBajoStock(tenant.getSubdomain(), bajoStock);
                whatsAppService.notificarVencimiento(tenant.getSubdomain(), proximosAVencer);
                whatsAppService.notificarMantenimiento(tenant.getSubdomain(), mantenimiento);
                if (tenant.isPlanPorVencer() || tenant.isPlanVencido()) {
                    whatsAppService.notificarPlanVencimiento(tenant.getSubdomain(), tenant.getPlanFin());
                }

                // Email — solo si SMTP configurado y tenant tiene email
                boolean tieneEmail = tenant.getEmailAdmin() != null && !tenant.getEmailAdmin().isBlank();
                if (emailService.mailConfigurado() && tieneEmail) {
                    if (tenant.getAlertasIntentosFallidos() >= UMBRAL_WARN) {
                        log.warn("Tenant '{}' lleva {} intentos fallidos consecutivos — revisar SMTP.",
                                tenant.getSubdomain(), tenant.getAlertasIntentosFallidos());
                    }
                    boolean enviado = emailService.enviarAlertasDiarias(
                            tenant, bajoStock, proximosAVencer, mantenimiento);
                    if (enviado) {
                        tenantService.registrarEnvioExitoso(tenant.getSubdomain());
                        enviados++;
                    }
                }
            } catch (Exception e) {
                log.error("Error procesando alertas para tenant '{}': {}", tenant.getSubdomain(), e.getMessage());
                boolean tieneEmail = tenant.getEmailAdmin() != null && !tenant.getEmailAdmin().isBlank();
                if (emailService.mailConfigurado() && tieneEmail) {
                    tenantService.registrarEnvioFallido(tenant.getSubdomain());
                }
            } finally {
                TenantContext.clear();
            }
        }

        log.info("Alertas diarias: {} notificación(es) in-app creada(s), {} email(s) enviado(s) de {} tenant(s)",
                notifs, enviados, tenants.size());
    }

    /**
     * Emails de onboarding (días 1/3/7 desde planInicio) y recordatorios de expiración
     * (4 y 1 días antes de planFin). Se salta el tenant "demo".
     */
    @Scheduled(cron = "${app.onboarding.cron:0 30 9 * * *}")
    public void enviarEmailsOnboarding() {
        if (!emailService.mailConfigurado()) return;

        List<Tenant> tenants = tenantRepo.findAll().stream()
                .filter(t -> t.isActive()
                          && t.getEmailAdmin() != null
                          && !t.getEmailAdmin().isBlank()
                          && !"demo".equals(t.getSubdomain()))
                .toList();

        int onboarding = 0;
        int expiracion = 0;

        for (Tenant tenant : tenants) {
            try {
                LocalDate hoy = LocalDate.now();

                if (tenant.getPlanInicio() != null) {
                    long diasDesdeInicio = ChronoUnit.DAYS.between(tenant.getPlanInicio(), hoy);
                    if (diasDesdeInicio == 1 || diasDesdeInicio == 3 || diasDesdeInicio == 7) {
                        if (emailService.enviarOnboarding(tenant, (int) diasDesdeInicio)) onboarding++;
                    }
                }

                if (tenant.getPlanFin() != null && !tenant.isPlanVencido()) {
                    long diasHastaFin = ChronoUnit.DAYS.between(hoy, tenant.getPlanFin());
                    if (diasHastaFin == 4 || diasHastaFin == 1) {
                        if (emailService.enviarExpiracionTrial(tenant, diasHastaFin)) expiracion++;
                    }
                }
            } catch (Exception e) {
                log.error("Error en emails onboarding para tenant '{}': {}",
                        tenant.getSubdomain(), e.getMessage());
            }
        }

        log.info("Emails onboarding: {} onboarding, {} expiración enviados de {} tenants activos",
                onboarding, expiracion, tenants.size());
    }

    @Scheduled(cron = "${app.alert.monthly-cron:0 0 8 1 * *}")
    public void enviarResumenMensual() {
        java.time.LocalDate ayer = java.time.LocalDate.now().minusDays(1); // ayer = último día del mes anterior
        int anio = ayer.getYear();
        int mes  = ayer.getMonthValue();
        java.time.LocalDate inicio = java.time.LocalDate.of(anio, mes, 1);
        java.time.LocalDate fin    = ayer.withDayOfMonth(ayer.lengthOfMonth());

        List<Tenant> tenants = tenantRepo.findAll().stream()
                .filter(Tenant::isActive)
                .toList();

        int enviados = 0;
        for (Tenant tenant : tenants) {
            try {
                TenantContext.setCurrentTenant(tenant.getSubdomain());
                List<com.alera.model.LoteCerveza> lotesMes = loteCervezaRepo.findByPeriodo(inicio, fin);
                boolean tieneEmail = tenant.getEmailAdmin() != null && !tenant.getEmailAdmin().isBlank();
                if (emailService.mailConfigurado() && tieneEmail) {
                    boolean ok = emailService.enviarResumenMensual(tenant, lotesMes, anio, mes);
                    if (ok) enviados++;
                }
                long completados = lotesMes.stream()
                        .filter(com.alera.model.LoteCerveza::isCompletado).count();
                String nombreTenant = tenant.getName() != null && !tenant.getName().isBlank()
                        ? tenant.getName() : tenant.getSubdomain();
                whatsAppService.enviarResumenMensual(tenant.getSubdomain(), nombreTenant,
                        lotesMes.size(), (int) completados, anio, mes);
            } catch (Exception e) {
                log.error("Error en resumen mensual para tenant '{}': {}", tenant.getSubdomain(), e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
        log.info("Resumen mensual {}/{}: {} email(s) enviado(s) de {} tenant(s)", mes, anio, enviados, tenants.size());
    }
}
