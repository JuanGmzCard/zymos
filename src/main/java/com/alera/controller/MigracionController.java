package com.alera.controller;

import com.alera.model.MigracionLog;
import com.alera.model.Tenant;
import com.alera.repository.TenantRepository;
import com.alera.service.MigracionService;
import com.alera.service.MigracionTemplateService;
import jakarta.servlet.http.HttpServletRequest;
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

@Controller
@RequestMapping("/admin/migracion")
public class MigracionController {

    private static final String XLSX_MIME =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final MigracionTemplateService templateService;
    private final MigracionService         migracionService;
    private final TenantRepository         tenantRepo;

    public MigracionController(MigracionTemplateService templateService,
                                MigracionService migracionService,
                                TenantRepository tenantRepo) {
        this.templateService  = templateService;
        this.migracionService = migracionService;
        this.tenantRepo       = tenantRepo;
    }

    // ── Página de detalle por tenant ──────────────────────────────────────────

    @GetMapping("/{subdomain}")
    public String detalle(@PathVariable String subdomain, Model model, RedirectAttributes ra) {
        Tenant tenant = tenantRepo.findById(subdomain).orElse(null);
        if (tenant == null) {
            ra.addFlashAttribute("mensaje", "Tenant no encontrado: " + subdomain);
            ra.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/admin/tenants";
        }
        List<MigracionLog> historial = migracionService.historial(subdomain);
        model.addAttribute("tenant",   tenant);
        model.addAttribute("historial", historial);
        return "admin/migracion/detalle";
    }

    // ── Descarga de plantillas ─────────────────────────────────────────────────

    @GetMapping("/{subdomain}/plantilla/{modulo}")
    public ResponseEntity<byte[]> descargarPlantilla(@PathVariable String subdomain,
                                                      @PathVariable String modulo) throws IOException {
        byte[] data = switch (modulo) {
            case "almacen"    -> templateService.plantillaAlmacen();
            case "equipos"    -> templateService.plantillaEquipos();
            case "comercial"  -> templateService.plantillaComercial();
            case "produccion" -> templateService.plantillaProduccion();
            case "clientes"   -> templateService.plantillaClientes();
            case "ventas"     -> templateService.plantillaVentas();
            case "barriles"   -> templateService.plantillaBarriles();
            case "ordenes"    -> templateService.plantillaOrdenes();
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
                           RedirectAttributes flash) {
        if (archivo.isEmpty()) {
            flash.addFlashAttribute("mensaje", "Seleccione un archivo antes de importar.");
            flash.addFlashAttribute("tipoMensaje", "warning");
            return "redirect:/admin/migracion/" + subdomain;
        }
        String usuario = auth != null ? auth.getName() : "sistema";
        try {
            MigracionService.Resultado res = switch (modulo) {
                case "almacen"    -> migracionService.importarAlmacen(archivo, subdomain, usuario);
                case "equipos"    -> migracionService.importarEquipos(archivo, subdomain, usuario);
                case "comercial"  -> migracionService.importarComercial(archivo, subdomain, usuario);
                case "produccion" -> migracionService.importarProduccion(archivo, subdomain, usuario);
                case "clientes"   -> migracionService.importarClientes(archivo, subdomain, usuario);
                case "ventas"     -> migracionService.importarVentas(archivo, subdomain, usuario);
                case "barriles"   -> migracionService.importarBarriles(archivo, subdomain, usuario);
                case "ordenes"    -> migracionService.importarOrdenes(archivo, subdomain, usuario);
                case "seguimiento"    -> migracionService.importarSeguimiento(archivo, subdomain, usuario);
                case "catalogos"      -> migracionService.importarCatalogos(archivo, subdomain, usuario);
                case "mantenimientos" -> migracionService.importarMantenimientos(archivo, subdomain, usuario);
                default -> throw new IllegalArgumentException("Módulo desconocido: " + modulo);
            };
            String tipo = switch (res.estado()) {
                case "EXITOSO"  -> "success";
                case "PARCIAL"  -> "warning";
                default         -> "danger";
            };
            String msg = String.format("Módulo %s: %d filas procesadas, %d exitosas, %d con errores.",
                    modulo, res.procesadas(), res.exitosas(), res.errores());
            if (!res.mensajes().isEmpty()) {
                msg += " Primeros errores: " + String.join("; ", res.mensajes().stream().limit(3).toList());
            }
            flash.addFlashAttribute("mensaje", msg);
            flash.addFlashAttribute("tipoMensaje", tipo);
        } catch (Exception e) {
            flash.addFlashAttribute("mensaje", "Error procesando archivo: " + e.getMessage());
            flash.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/admin/migracion/" + subdomain;
    }
}
