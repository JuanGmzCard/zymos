package com.alera.service;

import com.alera.config.TenantContext;
import com.alera.model.Equipo;
import com.alera.model.FacturaProveedor;
import com.alera.model.InsumoInventario;
import com.alera.model.Tenant;
import com.alera.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
                            VentaService ventaService) {
        this.tenantRepo          = tenantRepo;
        this.insumoService       = insumoService;
        this.equipoService       = equipoService;
        this.emailService        = emailService;
        this.tenantService       = tenantService;
        this.notificacionService = notificacionService;
        this.facturaService      = facturaService;
        this.ventaService        = ventaService;
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

                // Expirar cotizaciones vencidas
                int expiradas = ventaService.expirarCotizaciones();
                if (expiradas > 0) {
                    log.info("Tenant '{}': {} cotización(es) expirada(s)", tenant.getSubdomain(), expiradas);
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
}
