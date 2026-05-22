package com.alera.service;

import com.alera.config.TenantContext;
import com.alera.model.Equipo;
import com.alera.model.InsumoInventario;
import com.alera.model.Tenant;
import com.alera.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AlertaScheduler {

    private static final Logger log = LoggerFactory.getLogger(AlertaScheduler.class);
    // A partir de este umbral se emite WARN en cada intento fallido adicional
    private static final int UMBRAL_WARN = 3;

    private final TenantRepository tenantRepo;
    private final InsumoInventarioService insumoService;
    private final EquipoService equipoService;
    private final EmailService emailService;
    private final TenantService tenantService;

    @Value("${app.alert.vencimiento-dias:30}")
    private int vencimientoDias;

    public AlertaScheduler(TenantRepository tenantRepo,
                            InsumoInventarioService insumoService,
                            EquipoService equipoService,
                            EmailService emailService,
                            TenantService tenantService) {
        this.tenantRepo    = tenantRepo;
        this.insumoService = insumoService;
        this.equipoService = equipoService;
        this.emailService  = emailService;
        this.tenantService = tenantService;
    }

    @Scheduled(cron = "${app.alert.cron:0 0 8 * * MON-FRI}")
    public void enviarAlertasDiarias() {
        if (!emailService.mailConfigurado()) {
            log.debug("SMTP no configurado — alertas por email deshabilitadas");
            return;
        }

        List<Tenant> tenants = tenantRepo.findAll().stream()
                .filter(Tenant::isActive)
                .filter(t -> t.getEmailAdmin() != null && !t.getEmailAdmin().isBlank())
                .toList();

        if (tenants.isEmpty()) {
            log.debug("Ningún tenant activo con email configurado");
            return;
        }

        int enviados = 0;
        for (Tenant tenant : tenants) {
            if (tenant.getAlertasIntentosFallidos() >= UMBRAL_WARN) {
                log.warn("Tenant '{}' lleva {} intentos fallidos consecutivos en alertas — revisar SMTP o email_admin.",
                         tenant.getSubdomain(), tenant.getAlertasIntentosFallidos());
            }
            try {
                TenantContext.setCurrentTenant(tenant.getSubdomain());

                List<InsumoInventario> bajoStock       = insumoService.listarBajoStock();
                List<InsumoInventario> proximosAVencer = insumoService.listarProximosAVencer(vencimientoDias);
                List<Equipo>           mantenimiento   = equipoService.listarMantenimientoPendiente();

                boolean enviado = emailService.enviarAlertasDiarias(
                        tenant, bajoStock, proximosAVencer, mantenimiento);
                if (enviado) {
                    tenantService.registrarEnvioExitoso(tenant.getSubdomain());
                    enviados++;
                }
            } catch (Exception e) {
                log.error("Error enviando alertas al tenant '{}': {}", tenant.getSubdomain(), e.getMessage());
                tenantService.registrarEnvioFallido(tenant.getSubdomain());
            } finally {
                TenantContext.clear();
            }
        }

        log.info("Alertas diarias procesadas: {} email(s) enviado(s) de {} tenant(s)", enviados, tenants.size());
    }
}