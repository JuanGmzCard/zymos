package com.alera.config;

import com.alera.model.CategoriaEquipo;
import com.alera.model.CategoriaInsumo;
import com.alera.model.SuperAdmin;
import com.alera.model.Tenant;
import com.alera.model.TipoCerveza;
import com.alera.model.enums.PlanNivel;
import com.alera.model.enums.RolUsuario;
import com.alera.repository.CategoriaEquipoRepository;
import com.alera.repository.CategoriaInsumoRepository;
import com.alera.repository.SuperAdminRepository;
import com.alera.repository.TenantRepository;
import com.alera.repository.TipoCervezaRepository;
import com.alera.repository.UsuarioRepository;
import com.alera.service.RolTenantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UsuarioRepository usuarioRepo;
    private final TipoCervezaRepository tipoCervezaRepo;
    private final TenantRepository tenantRepo;
    private final BrandingProperties branding;
    private final PasswordEncoder encoder;
    private final SuperAdminRepository superAdminRepo;
    private final CategoriaInsumoRepository categoriaInsumoRepo;
    private final CategoriaEquipoRepository categoriaEquipoRepo;
    private final RolTenantService rolTenantService;
    private final DemoDataSeeder demoDataSeeder;

    @Value("${app.default-subdomain:default}")
    private String defaultSubdomain;

    @Value("${ADMIN_USERNAME:admin}")        private String adminUsername;
    @Value("${ADMIN_PASSWORD:alera2024}")    private String adminPassword;
    @Value("${PRODUCCION_USERNAME:}")        private String produccionUsername;
    @Value("${PRODUCCION_PASSWORD:}")        private String produccionPassword;
    @Value("${INVENTARIO_USERNAME:}")        private String inventarioUsername;
    @Value("${INVENTARIO_PASSWORD:}")        private String inventarioPassword;
    @Value("${FACTURACION_USERNAME:}")       private String facturacionUsername;
    @Value("${FACTURACION_PASSWORD:}")       private String facturacionPassword;
    @Value("${EQUIPOS_USERNAME:}")           private String equiposUsername;
    @Value("${EQUIPOS_PASSWORD:}")           private String equiposPassword;

    @Value("${SUPERADMIN_USERNAME:}") private String superadminUsername;
    @Value("${SUPERADMIN_PASSWORD:}") private String superadminPassword;

    public DataInitializer(UsuarioRepository usuarioRepo,
                            TipoCervezaRepository tipoCervezaRepo,
                            TenantRepository tenantRepo,
                            BrandingProperties branding,
                            PasswordEncoder encoder,
                            SuperAdminRepository superAdminRepo,
                            CategoriaInsumoRepository categoriaInsumoRepo,
                            CategoriaEquipoRepository categoriaEquipoRepo,
                            RolTenantService rolTenantService,
                            DemoDataSeeder demoDataSeeder) {
        this.usuarioRepo         = usuarioRepo;
        this.tipoCervezaRepo     = tipoCervezaRepo;
        this.tenantRepo          = tenantRepo;
        this.branding            = branding;
        this.encoder             = encoder;
        this.superAdminRepo      = superAdminRepo;
        this.categoriaInsumoRepo = categoriaInsumoRepo;
        this.categoriaEquipoRepo = categoriaEquipoRepo;
        this.rolTenantService    = rolTenantService;
        this.demoDataSeeder      = demoDataSeeder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        crearSuperAdmin();
        crearTenantDefault();
        crearTenantDemo();

        // Inicializar todos los tenants existentes (no solo el default)
        for (Tenant tenant : tenantRepo.findAll()) {
            TenantContext.setCurrentTenant(tenant.getSubdomain());
            try {
                crearUsuariosSiNoTiene(tenant.getSubdomain());
                crearTiposCerveza(tenant.getSubdomain());
                crearCategoriasSiNoTiene(tenant.getSubdomain());
                rolTenantService.crearRolesPorDefectoSiNoTiene(tenant.getSubdomain());
            } finally {
                TenantContext.clear();
            }
        }

        // Sembrar datos demo (usa JdbcTemplate — no necesita TenantContext activo)
        demoDataSeeder.seed();
    }

    private void crearSuperAdmin() {
        if (superadminUsername == null || superadminUsername.isBlank() ||
            superadminPassword == null || superadminPassword.isBlank()) {
            log.warn("SUPERADMIN_USERNAME/SUPERADMIN_PASSWORD no definidos — super-admin no creado automáticamente");
            return;
        }
        if (superAdminRepo.existsByUsername(superadminUsername)) return;
        SuperAdmin sa = new SuperAdmin();
        sa.setUsername(superadminUsername);
        sa.setPassword(encoder.encode(superadminPassword));
        sa.setActivo(true);
        superAdminRepo.save(sa);
        log.info("Super-admin '{}' creado", superadminUsername);
    }

    private void crearTenantDefault() {
        if (tenantRepo.existsById(defaultSubdomain)) return;

        Tenant t = new Tenant();
        t.setSubdomain(defaultSubdomain);
        t.setName(branding.getName());
        t.setTagline(branding.getTagline());
        t.setLogoUrl(branding.getLogoUrl());
        t.setColorNavbar(branding.getColorNavbar());
        t.setColorPrimary(branding.getColorPrimary());
        t.setColorAccent(branding.getColorAccent());
        t.setColorAccentHover(branding.getColorAccentHover());
        t.setColorCream(branding.getColorCream());
        t.setColorBodyBg(branding.getColorBodyBg());
        t.setFontHeadings(branding.getFontHeadings());
        t.setFontBody(branding.getFontBody());
        t.setSimboloMoneda(branding.getSimboloMoneda());
        t.setUnidadTemperatura(branding.getUnidadTemperatura());
        t.setActive(true);
        tenantRepo.save(t);
        log.info("Tenant '{}' creado con branding '{}'", defaultSubdomain, branding.getName());
    }

    private void crearTenantDemo() {
        if (tenantRepo.existsById("demo")) {
            // Asegura que el plan esté definido si el tenant ya existía antes de V91
            tenantRepo.findById("demo").ifPresent(t -> {
                if (t.getPlanNivel() == null) {
                    t.setPlanNivel(PlanNivel.FERMENTACION);
                    tenantRepo.save(t);
                    log.info("Tenant demo actualizado a plan FERMENTACION");
                }
            });
            return;
        }
        Tenant demo = new Tenant();
        demo.setSubdomain("demo");
        demo.setName("Cervecería Demo");
        demo.setTagline("Explora Zymos · Datos de demostración");
        demo.setColorNavbar(branding.getColorNavbar());
        demo.setColorPrimary(branding.getColorPrimary());
        demo.setColorAccent(branding.getColorAccent());
        demo.setColorAccentHover(branding.getColorAccentHover());
        demo.setColorCream(branding.getColorCream());
        demo.setColorBodyBg(branding.getColorBodyBg());
        demo.setFontHeadings(branding.getFontHeadings());
        demo.setFontBody(branding.getFontBody());
        demo.setSimboloMoneda(branding.getSimboloMoneda());
        demo.setUnidadTemperatura(branding.getUnidadTemperatura());
        demo.setPlanNivel(PlanNivel.FERMENTACION);
        demo.setActive(true);
        tenantRepo.save(demo);
        log.info("Tenant 'demo' creado con plan FERMENTACION");
    }

    // Solo crea usuarios si el tenant no tiene ninguno — no sobreescribe configuraciones existentes
    private void crearUsuariosSiNoTiene(String subdomain) {
        // El tenant demo tiene su propio usuario (demo/demo) gestionado por DemoDataSeeder
        if ("demo".equals(subdomain)) return;
        if (usuarioRepo.findAllByTenantId(subdomain).isEmpty()) {
            crearUsuarioSiNoExiste(adminUsername,       adminPassword,       RolUsuario.ADMIN,       subdomain);
            crearUsuarioSiNoExiste(produccionUsername,  produccionPassword,  RolUsuario.PRODUCCION,  subdomain);
            crearUsuarioSiNoExiste(inventarioUsername,  inventarioPassword,  RolUsuario.INVENTARIO,  subdomain);
            crearUsuarioSiNoExiste(facturacionUsername, facturacionPassword, RolUsuario.FACTURACION, subdomain);
            crearUsuarioSiNoExiste(equiposUsername,     equiposPassword,     RolUsuario.EQUIPOS,     subdomain);
        }
    }

    private void crearUsuarioSiNoExiste(String username, String password, RolUsuario rol, String subdomain) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) return;
        usuarioRepo.insertarConTenant(username, encoder.encode(password), rol.name(), subdomain);
        log.info("Usuario '{}' ({}) creado en tenant '{}'", username, rol, subdomain);
    }

    private void crearTiposCerveza(String subdomain) {
        String[] tipos = {"APA", "IPA", "Stout", "Helles", "Porter", "Wheat", "Saison", "Lager", "Otro"};
        int creados = 0;
        for (String nombre : tipos) {
            if (!tipoCervezaRepo.existsByNombreIgnoreCase(nombre)) {
                TipoCerveza t = new TipoCerveza();
                t.setNombre(nombre);
                t.setActivo(true);
                tipoCervezaRepo.save(t);
                creados++;
            }
        }
        if (creados > 0) log.info("{} tipos de cerveza inicializados en tenant '{}'", creados, subdomain);
    }

    private void crearCategoriasSiNoTiene(String subdomain) {
        if (categoriaInsumoRepo.findAllByOrderByNombreAsc().isEmpty()) {
            String[] insumoCats = {
                "Malta", "Lúpulo", "Levadura", "Clarificante",
                "Agente de Carbonatación", "Agua", "Químico", "Envase", "Otro"
            };
            for (String nombre : insumoCats) {
                CategoriaInsumo cat = new CategoriaInsumo();
                cat.setNombre(nombre);
                cat.setActivo(true);
                categoriaInsumoRepo.save(cat);
            }
            log.info("{} categorías de insumo inicializadas en tenant '{}'", insumoCats.length, subdomain);
        }
        if (categoriaEquipoRepo.findAllByOrderByNombreAsc().isEmpty()) {
            String[] equipoCats = {
                "Fermentador", "Olla de Macerado", "Olla de Hervor", "Enfriador",
                "Bomba", "Filtro", "Medidor de pH", "Densímetro", "Báscula", "Compresor", "Otro"
            };
            for (String nombre : equipoCats) {
                CategoriaEquipo cat = new CategoriaEquipo();
                cat.setNombre(nombre);
                cat.setActivo(true);
                categoriaEquipoRepo.save(cat);
            }
            log.info("{} categorías de equipo inicializadas en tenant '{}'", equipoCats.length, subdomain);
        }
    }
}
