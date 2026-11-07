package com.alera.service;

import com.alera.config.BrandingProperties;
import com.alera.config.TenantContext;
import com.alera.model.*;
import com.alera.model.enums.PlanNivel;
import com.alera.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.security.SecureRandom;
import java.time.LocalDate;

/**
 * Crea un tenant nuevo tras un pago exitoso: tenant, usuario admin,
 * datos iniciales (tipos, categorías, roles) y envía el email de bienvenida.
 */
@Service
public class TenantProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(TenantProvisioningService.class);
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom RND = new SecureRandom();

    private final TenantRepository          tenantRepo;
    private final UsuarioRepository         usuarioRepo;
    private final TipoCervezaRepository     tipoCervezaRepo;
    private final CategoriaInsumoRepository categoriaInsumoRepo;
    private final CategoriaEquipoRepository categoriaEquipoRepo;
    private final RolTenantService          rolTenantService;
    private final EmailService              emailService;
    private final BrandingProperties        branding;
    private final PasswordEncoder           encoder;

    public TenantProvisioningService(TenantRepository tenantRepo,
                                     UsuarioRepository usuarioRepo,
                                     TipoCervezaRepository tipoCervezaRepo,
                                     CategoriaInsumoRepository categoriaInsumoRepo,
                                     CategoriaEquipoRepository categoriaEquipoRepo,
                                     RolTenantService rolTenantService,
                                     EmailService emailService,
                                     BrandingProperties branding,
                                     PasswordEncoder encoder) {
        this.tenantRepo          = tenantRepo;
        this.usuarioRepo         = usuarioRepo;
        this.tipoCervezaRepo     = tipoCervezaRepo;
        this.categoriaInsumoRepo = categoriaInsumoRepo;
        this.categoriaEquipoRepo = categoriaEquipoRepo;
        this.rolTenantService    = rolTenantService;
        this.emailService        = emailService;
        this.branding            = branding;
        this.encoder             = encoder;
    }

    public record ResultadoProvision(boolean exito, boolean yaExistia,
                                     String subdominio, String loginUrl, String mensaje) {
        public static ResultadoProvision ok(String sub, String url) {
            return new ResultadoProvision(true, false, sub, url, "Tenant creado correctamente");
        }
        public static ResultadoProvision yaExiste(String sub) {
            return new ResultadoProvision(true, true, sub, null, "Tenant ya existía");
        }
        public static ResultadoProvision error(String msg) {
            return new ResultadoProvision(false, false, null, null, msg);
        }
    }

    /** Provisiona con 30 días (suscripción mensual). */
    @Transactional
    public ResultadoProvision provisionar(String subdominio, String email,
                                          String nombreCerveceria, PlanNivel planNivel,
                                          String appBaseUrl) {
        return provisionar(subdominio, email, nombreCerveceria, planNivel, appBaseUrl, 30);
    }

    /** Provisiona con duración personalizada en días (ej: 14 para trial gratuito). */
    @Transactional
    public ResultadoProvision provisionar(String subdominio,
                                          String email,
                                          String nombreCerveceria,
                                          PlanNivel planNivel,
                                          String appBaseUrl,
                                          int duracionDias) {
        if (tenantRepo.existsById(subdominio)) {
            log.warn("Tenant '{}' ya existe — evitando doble provision", subdominio);
            return ResultadoProvision.yaExiste(subdominio);
        }

        try {
            // 1. Crear tenant
            Tenant tenant = new Tenant();
            tenant.setSubdomain(subdominio);
            tenant.setName(nombreCerveceria);
            tenant.setEmailAdmin(email);
            tenant.setPlanNivel(planNivel);
            tenant.setPlanInicio(LocalDate.now());
            tenant.setPlanFin(LocalDate.now().plusDays(duracionDias));
            tenant.setActive(true);
            copiarBranding(tenant);
            tenantRepo.save(tenant);

            // 2. Crear usuario admin (native SQL — bypasea @TenantId de Hibernate)
            String tempPassword = generarPassword();
            usuarioRepo.insertarConTenant(email, encoder.encode(tempPassword), "ADMIN", subdominio);

            // 3. Datos iniciales requieren TenantContext
            TenantContext.setCurrentTenant(subdominio);
            try {
                crearTiposCerveza(subdominio);
                crearCategorias(subdominio);
                rolTenantService.crearRolesPorDefectoSiNoTiene(subdominio);
            } finally {
                TenantContext.clear();
            }

            // 4. Email de bienvenida
            String loginUrl = construirLoginUrl(appBaseUrl, subdominio);
            emailService.enviarBienvenidaCompra(tenant, email, tempPassword, loginUrl);

            log.info("Tenant '{}' provisionado exitosamente para {}", subdominio, email);
            return ResultadoProvision.ok(subdominio, loginUrl);

        } catch (Exception e) {
            log.error("Error provisionando tenant '{}': {}", subdominio, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResultadoProvision.error("Error al crear la cuenta: " + e.getMessage());
        }
    }

    private void copiarBranding(Tenant t) {
        t.setColorNavbar   (branding.getColorNavbar());
        t.setColorPrimary  (branding.getColorPrimary());
        t.setColorAccent   (branding.getColorAccent());
        t.setColorAccentHover(branding.getColorAccentHover());
        t.setColorCream    (branding.getColorCream());
        t.setColorBodyBg   (branding.getColorBodyBg());
        t.setFontHeadings  (branding.getFontHeadings());
        t.setFontBody      (branding.getFontBody());
    }

    private void crearTiposCerveza(String subdomain) {
        String[] tipos = {"APA", "IPA", "Stout", "Helles", "Porter", "Wheat", "Saison", "Lager", "Otro"};
        for (String nombre : tipos) {
            if (!tipoCervezaRepo.existsByNombreIgnoreCaseAndTenantId(nombre, subdomain)) {
                TipoCerveza t = new TipoCerveza();
                t.setNombre(nombre);
                t.setActivo(true);
                tipoCervezaRepo.save(t);
            }
        }
        log.debug("Tipos de cerveza inicializados en tenant '{}'", subdomain);
    }

    private void crearCategorias(String subdomain) {
        if (categoriaInsumoRepo.findAllByOrderByNombreAsc().isEmpty()) {
            for (String nombre : new String[]{"Malta", "Lúpulo", "Levadura", "Clarificante",
                    "Agente de Carbonatación", "Agua", "Químico", "Envase", "Otro"}) {
                CategoriaInsumo cat = new CategoriaInsumo();
                cat.setNombre(nombre); cat.setActivo(true);
                categoriaInsumoRepo.save(cat);
            }
        }
        if (categoriaEquipoRepo.findAllByOrderByNombreAsc().isEmpty()) {
            for (String nombre : new String[]{"Fermentador", "Olla de Macerado", "Olla de Hervor",
                    "Enfriador", "Bomba", "Filtro", "Medidor de pH", "Densímetro", "Báscula",
                    "Compresor", "Otro"}) {
                CategoriaEquipo cat = new CategoriaEquipo();
                cat.setNombre(nombre); cat.setActivo(true);
                categoriaEquipoRepo.save(cat);
            }
        }
        log.debug("Categorías inicializadas en tenant '{}'", subdomain);
    }

    private String construirLoginUrl(String appBaseUrl, String subdomain) {
        // Si la URL base es https://app.zymos.app → https://subdomain.zymos.app
        // Si es http://localhost:8080 → http://localhost:8080 (single-domain dev)
        if (appBaseUrl.contains("localhost") || appBaseUrl.contains("127.0.0.1")) {
            return appBaseUrl;
        }
        // Reemplaza el primer segmento del host con el subdomain
        return appBaseUrl.replaceFirst("://([^./]+)", "://" + subdomain);
    }

    private String generarPassword() {
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) sb.append(CHARS.charAt(RND.nextInt(CHARS.length())));
        return sb.toString();
    }
}
