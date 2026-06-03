package com.alera.controller;

import com.alera.model.FacturaItem;
import com.alera.model.InsumoInventario;
import com.alera.model.enums.TipoMovimiento;
import com.alera.repository.FacturaItemRepository;
import com.alera.service.CategoriaInsumoService;
import com.alera.service.ExcelExportService;
import com.alera.service.InsumoInventarioService;
import com.alera.service.ProveedorService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/inventario")
public class InsumoInventarioController {

    private final InsumoInventarioService service;
    private final FacturaItemRepository facturaItemRepo;
    private final ExcelExportService excelService;
    private final ProveedorService proveedorService;
    private final CategoriaInsumoService categoriaInsumoService;

    public InsumoInventarioController(InsumoInventarioService service,
                                       FacturaItemRepository facturaItemRepo,
                                       ExcelExportService excelService,
                                       ProveedorService proveedorService,
                                       CategoriaInsumoService categoriaInsumoService) {
        this.service                = service;
        this.facturaItemRepo        = facturaItemRepo;
        this.excelService           = excelService;
        this.proveedorService       = proveedorService;
        this.categoriaInsumoService = categoriaInsumoService;
    }

    @GetMapping
    public String lista(
            @RequestParam(defaultValue = "") String nombre,
            @RequestParam(required = false) String tipo,
            @RequestParam(defaultValue = "false") boolean filtroBajoStock,
            @RequestParam(defaultValue = "false") boolean filtroPorVencer,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        List<InsumoInventario> insumos;
        long totalInsumos;
        int totalPaginas;

        if (filtroBajoStock) {
            insumos = service.listarBajoStock();
            totalInsumos = insumos.size();
            totalPaginas = 1;
            page = 0;
        } else if (filtroPorVencer) {
            insumos = service.listarProximosAVencer(30);
            totalInsumos = insumos.size();
            totalPaginas = 1;
            page = 0;
        } else {
            var pagina = service.listarPaginado(nombre, tipo, page);
            insumos = pagina.getContent();
            totalInsumos = pagina.getTotalElements();
            totalPaginas = pagina.getTotalPages();
        }

        model.addAttribute("insumos",          insumos);
        model.addAttribute("paginaActual",     page);
        model.addAttribute("totalPaginas",     totalPaginas);
        model.addAttribute("totalInsumos",     totalInsumos);
        model.addAttribute("nombreFiltro",     nombre);
        model.addAttribute("tipoFiltro",       tipo);
        model.addAttribute("tiposInsumo",      categoriaInsumoService.listarNombresActivos());
        model.addAttribute("bajoStock",        service.listarBajoStock());
        model.addAttribute("proximosVencer",   service.listarProximosAVencer(30));
        model.addAttribute("filtroBajoStock",  filtroBajoStock);
        model.addAttribute("filtroPorVencer",  filtroPorVencer);
        return "inventario/lista";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTARIO', 'SUPERADMIN')")
    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("insumo", new InsumoInventario());
        model.addAttribute("tiposInsumo", categoriaInsumoService.listarNombresActivos());
        model.addAttribute("proveedores", proveedorService.listarActivos());
        return "inventario/formulario";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTARIO', 'SUPERADMIN')")
    @PostMapping("/guardar")
    public String guardar(@ModelAttribute InsumoInventario insumo, RedirectAttributes ra) {
        try {
            service.guardar(insumo);
            ra.addFlashAttribute("mensaje", "Insumo guardado");
            ra.addFlashAttribute("tipoMensaje", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("mensaje", "Error: " + e.getMessage());
            ra.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/inventario";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTARIO', 'SUPERADMIN')")
    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes ra) {
        var insumo = service.buscarPorId(id).orElse(null);
        if (insumo == null) {
            ra.addFlashAttribute("mensaje", "El insumo no existe o fue eliminado");
            ra.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/inventario";
        }
        model.addAttribute("insumo", insumo);
        model.addAttribute("tiposInsumo", categoriaInsumoService.listarNombresActivos());
        model.addAttribute("proveedores", proveedorService.listarActivos());
        return "inventario/formulario";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTARIO', 'SUPERADMIN')")
    @PostMapping("/actualizar/{id}")
    public String actualizar(@PathVariable Long id, @ModelAttribute InsumoInventario insumo, RedirectAttributes ra) {
        insumo.setId(id);
        try {
            service.guardar(insumo);
            ra.addFlashAttribute("mensaje", "Insumo actualizado");
            ra.addFlashAttribute("tipoMensaje", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("mensaje", "Error: " + e.getMessage());
            ra.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/inventario";
    }

    @GetMapping(value = "/suggest", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<Map<String, Object>> suggest(
            @RequestParam(defaultValue = "") String nombre,
            @RequestParam(required = false) String tipo) {
        if (nombre.isBlank() || nombre.trim().length() < 2) return List.of();
        return service.listarPaginado(nombre.trim(), tipo, 0)
            .getContent().stream()
            .limit(6)
            .map(i -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id",        i.getId());
                m.put("nombre",    i.getNombre());
                m.put("tipoNombre",i.getTipo() != null ? i.getTipo() : "");
                m.put("colorTipo", i.getColorTipo());
                m.put("bajoStock", i.isBajoStock());
                m.put("url",       "/inventario/editar/" + i.getId());
                return m;
            }).toList();
    }

    @PostMapping("/guardar-rapido")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> guardarRapido(
            @RequestParam String nombre,
            @RequestParam(defaultValue = "Otro") String tipo,
            @RequestParam(defaultValue = "gr") String unidad) {
        Map<String, Object> resp = new LinkedHashMap<>();
        try {
            if (service.buscarPorNombreExacto(nombre.trim()).isPresent()) {
                resp.put("success", false);
                resp.put("error", "Ya existe un insumo con ese nombre");
                return ResponseEntity.badRequest().body(resp);
            }
            InsumoInventario ins = new InsumoInventario();
            ins.setNombre(nombre.trim());
            ins.setTipo(tipo.isBlank() ? "Otro" : tipo);
            ins.setUnidad(unidad);
            ins.setCantidad(BigDecimal.ZERO);
            ins.setStockMinimo(BigDecimal.ZERO);
            InsumoInventario saved = service.guardar(ins);
            resp.put("success", true);
            resp.put("id", saved.getId());
            resp.put("nombre", saved.getNombre());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            resp.put("success", false);
            resp.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(resp);
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTARIO', 'SUPERADMIN')")
    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        service.eliminar(id);
        ra.addFlashAttribute("mensaje", "Insumo eliminado");
        ra.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/inventario";
    }

    // ── Exportar a Excel ─────────────────────────────────────────────────

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(defaultValue = "") String nombre,
            @RequestParam(required = false) String tipo,
            @RequestParam(defaultValue = "false") boolean filtroBajoStock,
            @RequestParam(defaultValue = "false") boolean filtroPorVencer,
            HttpServletRequest request) {
        List<InsumoInventario> insumos;
        if (filtroBajoStock) {
            insumos = service.listarBajoStock();
        } else if (filtroPorVencer) {
            insumos = service.listarProximosAVencer(30);
        } else {
            insumos = (nombre.isBlank() && (tipo == null || tipo.isBlank()))
                    ? service.listarTodos()
                    : service.listarPaginado(nombre, tipo, 0).getContent();
        }
        com.alera.model.Tenant tenant = (com.alera.model.Tenant) request.getAttribute("currentTenant");
        com.alera.config.ExportBranding branding = com.alera.config.ExportBranding.from(tenant);
        byte[] bytes = excelService.generarExcelInventario(insumos, branding);
        String filename = "inventario-" + java.time.LocalDate.now() + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(bytes.length)
                .body(bytes);
    }

    // ── Ajuste rápido de stock ────────────────────────────────────────────

    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTARIO', 'SUPERADMIN')")
    @PostMapping("/{id}/ajuste")
    public String ajustarStock(@PathVariable Long id,
                                @RequestParam TipoMovimiento tipo,
                                @RequestParam BigDecimal cantidad,
                                @RequestParam(defaultValue = "") String motivo,
                                RedirectAttributes ra) {
        try {
            service.ajustar(id, tipo, cantidad, motivo.isBlank() ? null : motivo.trim());
            ra.addFlashAttribute("mensaje", "Stock ajustado correctamente");
            ra.addFlashAttribute("tipoMensaje", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("mensaje", "Error: " + e.getMessage());
            ra.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/inventario";
    }

    // ── Historial de movimientos ──────────────────────────────────────────

    @GetMapping("/{id}/historial")
    public String historialMovimientos(@PathVariable Long id,
                                        @RequestParam(defaultValue = "0") int page,
                                        Model model, RedirectAttributes ra) {
        InsumoInventario insumo = service.buscarPorId(id).orElse(null);
        if (insumo == null) {
            ra.addFlashAttribute("mensaje", "El insumo no existe o fue eliminado");
            ra.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/inventario";
        }
        var pagina = service.listarMovimientos(id, page);
        model.addAttribute("insumo",       insumo);
        model.addAttribute("movimientos",  pagina.getContent());
        model.addAttribute("paginaActual", page);
        model.addAttribute("totalPaginas", pagina.getTotalPages());
        model.addAttribute("baseUrl",      "/inventario/" + id + "/historial");
        model.addAttribute("extraParams",  "");
        return "inventario/historial";
    }

    // ── Historial de precios ──────────────────────────────────────────────

    @GetMapping("/precios")
    public String historialPrecios(@RequestParam(required = false) String nombre,
                                    Model model) {
        // Datalist con todos los nombres de ítems de facturas
        model.addAttribute("nombresFactura", facturaItemRepo.findNombresDistintos());
        // Insumos del inventario para el selector rápido
        model.addAttribute("insumosInventario", service.listarBajoStock().isEmpty()
                ? Collections.emptyList() : service.listarBajoStock()); // placeholder — se llena abajo

        if (nombre != null && !nombre.isBlank()) {
            String nombreTrimmed = nombre.trim();
            List<FacturaItem> historial = facturaItemRepo.findHistorialPreciosPorNombre(nombreTrimmed);
            model.addAttribute("nombre",   nombreTrimmed);
            model.addAttribute("historial", historial);

            if (!historial.isEmpty()) {
                // Estadísticas sobre valorUnitario
                List<BigDecimal> precios = historial.stream()
                        .filter(fi -> fi.getValorUnitario() != null
                                      && fi.getValorUnitario().compareTo(BigDecimal.ZERO) > 0)
                        .map(FacturaItem::getValorUnitario)
                        .collect(Collectors.toList());

                if (!precios.isEmpty()) {
                    BigDecimal suma  = precios.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal prom  = suma.divide(BigDecimal.valueOf(precios.size()), 2, RoundingMode.HALF_UP);
                    BigDecimal min   = precios.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
                    BigDecimal max   = precios.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
                    BigDecimal ultimo = precios.get(0); // historial está ordenado DESC (más reciente primero)

                    model.addAttribute("precioUltimo",  ultimo);
                    model.addAttribute("precioPromedio",prom);
                    model.addAttribute("precioMinimo",  min);
                    model.addAttribute("precioMaximo",  max);

                    BigDecimal variacion = precios.size() > 1
                            ? ultimo.subtract(precios.get(precios.size() - 1)) : BigDecimal.ZERO;
                    model.addAttribute("variacion", variacion);
                }

                long nProveedores = historial.stream()
                        .map(fi -> fi.getFactura().getProveedor())
                        .filter(Objects::nonNull)
                        .distinct().count();
                model.addAttribute("nProveedores",  nProveedores);
                model.addAttribute("nCompras",       historial.size());

                // Datos para Chart.js — orden cronológico (más antiguo primero)
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yy");
                List<FacturaItem> cronologico = new ArrayList<>(historial);
                Collections.reverse(cronologico);

                model.addAttribute("chartFechas", cronologico.stream()
                        .filter(fi -> fi.getFactura().getFechaFactura() != null)
                        .map(fi -> fi.getFactura().getFechaFactura().format(fmt))
                        .collect(Collectors.toList()));
                model.addAttribute("chartPrecios", cronologico.stream()
                        .filter(fi -> fi.getValorUnitario() != null)
                        .map(fi -> fi.getValorUnitario().doubleValue())
                        .collect(Collectors.toList()));
                model.addAttribute("chartProveedores", cronologico.stream()
                        .map(fi -> fi.getFactura().getProveedor() != null
                                   ? fi.getFactura().getProveedor() : "—")
                        .collect(Collectors.toList()));
            }
        }
        return "inventario/precios";
    }
}
