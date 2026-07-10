package com.alera.controller;

import com.alera.model.MigracionLog;
import com.alera.model.Tenant;
import com.alera.repository.TenantRepository;
import com.alera.service.MigracionService;
import com.alera.service.MigracionTemplateService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.MessageSource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/admin/migracion")
public class MigracionController {

    private static final String XLSX_MIME =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final MigracionTemplateService templateService;
    private final MigracionService         migracionService;
    private final TenantRepository         tenantRepo;
    private final MessageSource            messageSource;

    public MigracionController(MigracionTemplateService templateService,
                                MigracionService migracionService,
                                TenantRepository tenantRepo,
                                MessageSource messageSource) {
        this.templateService  = templateService;
        this.migracionService = migracionService;
        this.tenantRepo       = tenantRepo;
        this.messageSource    = messageSource;
    }

    // ── Página de detalle por tenant ──────────────────────────────────────────

    @GetMapping("/{subdomain}")
    public String detalle(@PathVariable String subdomain, Model model,
                          RedirectAttributes ra, Locale locale) {
        Tenant tenant = tenantRepo.findById(subdomain).orElse(null);
        if (tenant == null) {
            ra.addFlashAttribute("mensaje", msg("admin.mig.flash.tenant.no.encontrado", locale, subdomain));
            ra.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/admin/tenants";
        }
        List<MigracionLog> historial = migracionService.historial(subdomain);

        long importacionesExitosas  = historial.stream().filter(l -> "EXITOSO".equals(l.getEstado())).count();
        long importacionesParciales = historial.stream().filter(l -> "PARCIAL".equals(l.getEstado())).count();

        model.addAttribute("tenant",                tenant);
        model.addAttribute("historial",             historial);
        model.addAttribute("totalImportaciones",    historial.size());
        model.addAttribute("importacionesExitosas", importacionesExitosas);
        model.addAttribute("importacionesParciales",importacionesParciales);
        return "admin/migracion/detalle";
    }

    // ── Descarga de plantillas ─────────────────────────────────────────────────

    @GetMapping("/{subdomain}/plantilla/{modulo}")
    public ResponseEntity<byte[]> descargarPlantilla(@PathVariable String subdomain,
                                                      @PathVariable String modulo) throws IOException {
        byte[] data = switch (modulo) {
            case "almacen"        -> templateService.plantillaAlmacen();
            case "equipos"        -> templateService.plantillaEquipos();
            case "comercial"      -> templateService.plantillaComercial();
            case "produccion"     -> templateService.plantillaProduccion();
            case "clientes"       -> templateService.plantillaClientes();
            case "ventas"         -> templateService.plantillaVentas();
            case "barriles"       -> templateService.plantillaBarriles();
            case "ordenes"        -> templateService.plantillaOrdenes();
            case "seguimiento"    -> templateService.plantillaSeguimiento();
            case "catalogos"      -> templateService.plantillaCatalogos();
            case "mantenimientos" -> templateService.plantillaMantenimientos();
            default -> throw new IllegalArgumentException("Módulo desconocido: " + modulo);
        };
        String filename = "plantilla-" + modulo + "-" + subdomain + ".xlsx";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(XLSX_MIME))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(data);
    }

    // ── Importación ───────────────────────────────────────────────────────────

    @PostMapping("/{subdomain}/importar/{modulo}")
    public String importar(@PathVariable String subdomain,
                           @PathVariable String modulo,
                           @RequestParam("archivo") MultipartFile archivo,
                           Authentication auth,
                           RedirectAttributes flash,
                           Locale locale) {
        if (archivo.isEmpty()) {
            flash.addFlashAttribute("mensaje",     msg("admin.mig.flash.archivo.vacio", locale));
            flash.addFlashAttribute("tipoMensaje", "warning");
            return "redirect:/admin/migracion/" + subdomain;
        }
        String usuario = auth != null ? auth.getName() : "sistema";
        try {
            MigracionService.Resultado res = switch (modulo) {
                case "almacen"        -> migracionService.importarAlmacen(archivo, subdomain, usuario);
                case "equipos"        -> migracionService.importarEquipos(archivo, subdomain, usuario);
                case "comercial"      -> migracionService.importarComercial(archivo, subdomain, usuario);
                case "produccion"     -> migracionService.importarProduccion(archivo, subdomain, usuario);
                case "clientes"       -> migracionService.importarClientes(archivo, subdomain, usuario);
                case "ventas"         -> migracionService.importarVentas(archivo, subdomain, usuario);
                case "barriles"       -> migracionService.importarBarriles(archivo, subdomain, usuario);
                case "ordenes"        -> migracionService.importarOrdenes(archivo, subdomain, usuario);
                case "seguimiento"    -> migracionService.importarSeguimiento(archivo, subdomain, usuario);
                case "catalogos"      -> migracionService.importarCatalogos(archivo, subdomain, usuario);
                case "mantenimientos" -> migracionService.importarMantenimientos(archivo, subdomain, usuario);
                default -> throw new IllegalArgumentException("Módulo desconocido: " + modulo);
            };
            String tipo = switch (res.estado()) {
                case "EXITOSO" -> "success";
                case "PARCIAL" -> "warning";
                default        -> "danger";
            };
            String mensaje = msgf("admin.mig.flash.resultado", locale,
                    modulo, res.procesadas(), res.exitosas(), res.errores());
            if (!res.mensajes().isEmpty()) {
                mensaje += " " + msgf("admin.mig.flash.primeros.errores", locale,
                        String.join("; ", res.mensajes().stream().limit(3).toList()));
            }
            flash.addFlashAttribute("mensaje",     mensaje);
            flash.addFlashAttribute("tipoMensaje", tipo);
        } catch (Exception e) {
            flash.addFlashAttribute("mensaje",     msgf("admin.mig.flash.error.procesando", locale, e.getMessage()));
            flash.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/admin/migracion/" + subdomain;
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String msg(String key, Locale locale, Object... args) {
        return messageSource.getMessage(key, args, key, locale);
    }

    private String msgf(String key, Locale locale, Object... args) {
        return messageSource.getMessage(key, args, key, locale);
    }
}
