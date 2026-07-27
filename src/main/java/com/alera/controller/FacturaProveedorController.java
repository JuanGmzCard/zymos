package com.alera.controller;

import com.alera.config.TenantContext;
import com.alera.dto.FacturaFormDto;
import com.alera.model.Equipo;
import com.alera.model.FacturaHistorialEstado;
import com.alera.model.FacturaItem;
import com.alera.model.FacturaProveedor;
import com.alera.model.InsumoInventario;
import com.alera.model.Tenant;
import com.alera.model.enums.EstadoEquipo;
import com.alera.model.enums.EstadoFactura;
import com.alera.model.enums.TipoItemFactura;
import com.alera.repository.EquipoRepository;
import com.alera.repository.FacturaItemRepository;
import com.alera.repository.InsumoInventarioRepository;
import com.alera.service.CategoriaEquipoService;
import com.alera.service.CategoriaInsumoService;
import com.alera.service.EquipoService;
import com.alera.service.ExcelExportService;
import com.alera.service.FacturaProveedorService;
import com.alera.service.InsumoInventarioService;
import com.alera.service.ProveedorService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/facturas")
public class FacturaProveedorController {

    private static final Logger log = LoggerFactory.getLogger(FacturaProveedorController.class);

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    private final FacturaProveedorService service;
    private final ProveedorService proveedorService;
    private final InsumoInventarioRepository insumoRepo;
    private final EquipoRepository equipoRepo;
    private final InsumoInventarioService insumoService;
    private final EquipoService equipoService;
    private final ExcelExportService excelService;
    private final CategoriaInsumoService categoriaInsumoService;
    private final CategoriaEquipoService categoriaEquipoService;
    private final FacturaItemRepository facturaItemRepo;

    public FacturaProveedorController(FacturaProveedorService service,
                                       ProveedorService proveedorService,
                                       InsumoInventarioRepository insumoRepo,
                                       EquipoRepository equipoRepo,
                                       InsumoInventarioService insumoService,
                                       EquipoService equipoService,
                                       ExcelExportService excelService,
                                       CategoriaInsumoService categoriaInsumoService,
                                       CategoriaEquipoService categoriaEquipoService,
                                       FacturaItemRepository facturaItemRepo) {
        this.service = service;
        this.proveedorService = proveedorService;
        this.insumoRepo = insumoRepo;
        this.equipoRepo = equipoRepo;
        this.insumoService = insumoService;
        this.equipoService = equipoService;
        this.excelService = excelService;
        this.categoriaInsumoService = categoriaInsumoService;
        this.categoriaEquipoService = categoriaEquipoService;
        this.facturaItemRepo = facturaItemRepo;
    }

    @GetMapping
    public String lista(@RequestParam(defaultValue = "0") int page,
                        @RequestParam(required = false) EstadoFactura estado,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                        Model model) {
        var pagina = service.listarPaginado(estado, desde, hasta, page);
        model.addAttribute("facturas",      pagina.getContent());
        model.addAttribute("paginaActual",  page);
        model.addAttribute("totalPaginas",  pagina.getTotalPages());
        model.addAttribute("totalFacturas", pagina.getTotalElements());
        model.addAttribute("estadoFiltro",  estado);
        model.addAttribute("desde",         desde);
        model.addAttribute("hasta",         hasta);
        model.addAttribute("estados",       EstadoFactura.values());
        model.addAttribute("baseUrl",       "/facturas");
        StringBuilder extra = new StringBuilder();
        if (estado != null) extra.append("&estado=").append(estado.name());
        if (desde   != null) extra.append("&desde=").append(desde);
        if (hasta   != null) extra.append("&hasta=").append(hasta);
        model.addAttribute("extraParams", extra.toString());

        // Stat-cards
        model.addAttribute("statsTotal",      service.sumTotal(estado, desde, hasta));
        model.addAttribute("statsPendiente",  service.sumPendiente(desde, hasta));
        model.addAttribute("statsCountPend",  service.countPendiente(desde, hasta));

        return "facturas/lista";
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportarExcel(
            @RequestParam(required = false) EstadoFactura estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            HttpServletRequest request, Locale locale) {
        var facturas = service.listarParaExport(estado, desde, hasta);
        Tenant tenant = (Tenant) request.getAttribute("currentTenant");
        com.alera.config.ExportBranding branding = com.alera.config.ExportBranding.from(tenant);
        byte[] bytes = excelService.generarExcelFacturas(facturas, estado, desde, hasta, branding, locale);
        String filename = "facturas-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @GetMapping(value = "/suggest", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<Map<String, Object>> suggest(@RequestParam(defaultValue = "") String q) {
        return service.suggest(q);
    }

    @GetMapping(value = "/ultimo-precio", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> ultimoPrecio(@RequestParam String nombre) {
        if (nombre == null || nombre.isBlank()) return Map.of();
        List<FacturaItem> items = facturaItemRepo.findHistorialPreciosPorNombre(nombre.trim());
        if (items.isEmpty()) return Map.of();
        FacturaItem item = items.get(0);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("valorUnitario", item.getValorUnitario());
        result.put("unidad", item.getUnidad());
        result.put("iva", item.getPorcentajeIvaItem());
        return result;
    }

    @GetMapping("/nueva")
    public String nueva(Model model) {
        model.addAttribute("facturaForm", FacturaFormDto.empty());
        agregarDatosFormulario(model);
        return "facturas/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("facturaForm") FacturaFormDto dto,
                          BindingResult result, Model model, RedirectAttributes ra,
                          @RequestParam(value = "archivo", required = false) MultipartFile archivo) {
        if (result.hasErrors()) {
            agregarDatosFormulario(model);
            return "facturas/formulario";
        }
        try {
            FacturaProveedor saved = service.guardar(dto);
            if (archivo != null && !archivo.isEmpty()) {
                String[] rutaYNombre = guardarArchivo(archivo, saved.getId());
                service.actualizarAdjunto(saved.getId(), rutaYNombre[0], rutaYNombre[1]);
            }
            ra.addFlashAttribute("mensaje", "Factura guardada y inventario actualizado");
            ra.addFlashAttribute("tipoMensaje", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("mensaje", "Error: " + e.getMessage());
            ra.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/facturas";
    }

    @GetMapping("/ver/{id}")
    public String ver(@PathVariable Long id, Model model, RedirectAttributes ra) {
        var factura = service.buscarPorId(id).orElse(null);
        if (factura == null) {
            ra.addFlashAttribute("mensaje", "La factura no existe o fue eliminada");
            ra.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/facturas";
        }
        model.addAttribute("factura",   factura);
        model.addAttribute("historial", service.listarHistorial(id));
        return "facturas/detalle";
    }

    @GetMapping("/duplicar/{id}")
    public String duplicar(@PathVariable Long id, Model model) {
        model.addAttribute("facturaForm", service.duplicarComoFormDto(id));
        agregarDatosFormulario(model);
        return "facturas/formulario";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes ra) {
        var factura = service.buscarPorId(id).orElse(null);
        if (factura == null) {
            ra.addFlashAttribute("mensaje", "La factura no existe o fue eliminada");
            ra.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/facturas";
        }
        model.addAttribute("facturaForm", service.toFormDto(factura));
        model.addAttribute("facturaId", id);
        model.addAttribute("archivoNombreActual", factura.getArchivoNombre());
        agregarDatosFormulario(model);
        return "facturas/formulario";
    }

    @PostMapping("/actualizar/{id}")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("facturaForm") FacturaFormDto dto,
                             BindingResult result, Model model, RedirectAttributes ra,
                             @RequestParam(value = "archivo", required = false) MultipartFile archivo) {
        if (result.hasErrors()) {
            agregarDatosFormulario(model);
            model.addAttribute("facturaId", id);
            return "facturas/formulario";
        }
        try {
            FacturaProveedor saved = service.actualizar(id, dto);
            if (archivo != null && !archivo.isEmpty()) {
                eliminarArchivoFisico(saved.getArchivoAdjunto());
                String[] rutaYNombre = guardarArchivo(archivo, id);
                service.actualizarAdjunto(id, rutaYNombre[0], rutaYNombre[1]);
            }
            ra.addFlashAttribute("mensaje", "Factura actualizada");
            ra.addFlashAttribute("tipoMensaje", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("mensaje", "Error: " + e.getMessage());
            ra.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/facturas";
    }

    @PostMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable Long id,
                                @RequestParam EstadoFactura estado,
                                RedirectAttributes ra) {
        try {
            service.cambiarEstado(id, estado);
            ra.addFlashAttribute("mensaje", "Estado actualizado a: " + estado.getDisplayName());
            ra.addFlashAttribute("tipoMensaje", "success");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("mensaje", e.getMessage());
            ra.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/facturas/ver/" + id;
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        try {
            service.buscarPorId(id).ifPresent(f -> eliminarArchivoFisico(f.getArchivoAdjunto()));
            service.eliminar(id);
            ra.addFlashAttribute("mensaje", "Factura eliminada");
            ra.addFlashAttribute("tipoMensaje", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("mensaje", "Error: " + e.getMessage());
            ra.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/facturas";
    }

    @GetMapping("/{id}/adjunto")
    public ResponseEntity<byte[]> descargarAdjunto(@PathVariable Long id) {
        FacturaProveedor factura = service.buscarPorId(id).orElse(null);
        if (factura == null || factura.getArchivoAdjunto() == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            Path archivo = Paths.get(uploadDir).resolve(factura.getArchivoAdjunto());
            if (!Files.exists(archivo)) return ResponseEntity.notFound().build();
            byte[] bytes = Files.readAllBytes(archivo);
            String contentType = Files.probeContentType(archivo);
            if (contentType == null) contentType = "application/octet-stream";
            String nombre = factura.getArchivoNombre() != null ? factura.getArchivoNombre() : archivo.getFileName().toString();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(bytes);
        } catch (IOException e) {
            log.error("Error al leer adjunto de factura {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ── Quick-create endpoints (accesibles para ADMIN y FACTURACION) ────────

    @PostMapping("/guardar-insumo-rapido")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> guardarInsumoRapido(
            @RequestParam String nombre,
            @RequestParam(defaultValue = "Otro") String tipo,
            @RequestParam(defaultValue = "gr") String unidad) {
        Map<String, Object> resp = new LinkedHashMap<>();
        try {
            String nombreTrim = nombre.trim();
            if (insumoService.buscarPorNombreExacto(nombreTrim).isPresent()) {
                resp.put("success", false);
                resp.put("error", "Ya existe un insumo con ese nombre");
                return ResponseEntity.badRequest().body(resp);
            }
            InsumoInventario ins = new InsumoInventario();
            ins.setNombre(nombreTrim);
            ins.setTipo(tipo.isBlank() ? "Otro" : tipo);
            ins.setUnidad(unidad);
            ins.setCantidad(BigDecimal.ZERO);
            ins.setStockMinimo(BigDecimal.ZERO);
            InsumoInventario saved = insumoService.guardar(ins);
            resp.put("success", true);
            resp.put("id", saved.getId());
            resp.put("nombre", saved.getNombre());
            resp.put("tipo", saved.getTipo());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            resp.put("success", false);
            resp.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(resp);
        }
    }

    @PostMapping("/guardar-equipo-rapido")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> guardarEquipoRapido(
            @RequestParam String nombre,
            @RequestParam(defaultValue = "Otro") String tipo) {
        Map<String, Object> resp = new LinkedHashMap<>();
        try {
            String nombreTrim = nombre.trim();
            Equipo eq = new Equipo();
            eq.setNombre(nombreTrim);
            eq.setTipo(tipo.isBlank() ? "Otro" : tipo);
            eq.setEstado(EstadoEquipo.OPERATIVO);
            Equipo saved = equipoService.guardar(eq);
            resp.put("success", true);
            resp.put("id", saved.getId());
            resp.put("nombre", saved.getNombre());
            resp.put("tipo", saved.getTipo());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            resp.put("success", false);
            resp.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(resp);
        }
    }

    // ── Helpers de adjuntos ──────────────────────────────────────────────────

    private String[] guardarArchivo(MultipartFile file, Long facturaId) throws IOException {
        String tenant = TenantContext.getCurrentTenant();
        if (tenant == null || tenant.isBlank()) tenant = "default";
        Path dir = Paths.get(uploadDir).resolve("facturas").resolve(tenant);
        Files.createDirectories(dir);
        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "adjunto";
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')) : "";
        String nombre = facturaId + "_" + UUID.randomUUID() + ext;
        Files.copy(file.getInputStream(), dir.resolve(nombre));
        String ruta = "facturas/" + tenant + "/" + nombre;
        return new String[]{ruta, original};
    }

    private void eliminarArchivoFisico(String ruta) {
        if (ruta == null || ruta.isBlank()) return;
        try {
            Files.deleteIfExists(Paths.get(uploadDir).resolve(ruta));
        } catch (IOException e) {
            log.warn("No se pudo eliminar adjunto: {}", ruta, e);
        }
    }

    // ────────────────────────────────────────────────────────────────────────

    private void agregarDatosFormulario(Model model) {
        List<String> tiposInsumo = categoriaInsumoService.listarNombresActivos();
        List<String> tiposEquipo = categoriaEquipoService.listarNombresActivos();
        model.addAttribute("tiposInsumo", tiposInsumo);
        model.addAttribute("tiposEquipo", tiposEquipo);
        model.addAttribute("tiposItem",   TipoItemFactura.values());
        model.addAttribute("estados",     EstadoFactura.values());
        model.addAttribute("proveedoresActivos", proveedorService.listarActivos());

        // Clave = nombre de categoría (String), valor = lista de nombres de insumos/equipos de esa categoría
        List<InsumoInventario> todosInsumos = insumoRepo.findAllByOrderByNombreAsc();
        Map<String, List<String>> insumosPorTipo = new LinkedHashMap<>();
        for (String tipo : tiposInsumo) {
            List<String> nombres = todosInsumos.stream()
                    .filter(i -> tipo.equals(i.getTipo()))
                    .map(InsumoInventario::getNombre)
                    .toList();
            insumosPorTipo.put(tipo, nombres);
        }

        List<Equipo> todosEquipos = equipoRepo.findAllByOrderByNombreAsc();
        Map<String, List<String>> equiposPorTipo = new LinkedHashMap<>();
        for (String tipo : tiposEquipo) {
            List<String> nombres = todosEquipos.stream()
                    .filter(e -> tipo.equals(e.getTipo()))
                    .map(Equipo::getNombre)
                    .toList();
            equiposPorTipo.put(tipo, nombres);
        }
        model.addAttribute("insumosPorTipo", insumosPorTipo);
        model.addAttribute("equiposPorTipo", equiposPorTipo);
    }
}