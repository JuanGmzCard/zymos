package com.alera.controller;

import com.alera.config.UnidadUtils;
import com.alera.dto.RecetaFormDto;
import com.alera.model.FacturaItem;
import com.alera.model.Receta;
import com.alera.model.RecetaIngrediente;
import com.alera.repository.FacturaItemRepository;
import com.alera.repository.LoteCervezaRepository;
import com.alera.service.InsumoInventarioService;
import com.alera.service.PdfExportService;
import com.alera.service.RecetaService;
import com.alera.service.TipoCervezaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/recetas")
public class RecetaController {

    private final RecetaService service;
    private final LoteCervezaRepository loteRepo;
    private final InsumoInventarioService insumoService;
    private final TipoCervezaService tipoCervezaService;
    private final FacturaItemRepository facturaItemRepo;
    private final PdfExportService pdfService;

    public RecetaController(RecetaService service, LoteCervezaRepository loteRepo,
                            InsumoInventarioService insumoService,
                            TipoCervezaService tipoCervezaService,
                            FacturaItemRepository facturaItemRepo,
                            PdfExportService pdfService) {
        this.service = service;
        this.loteRepo = loteRepo;
        this.insumoService = insumoService;
        this.tipoCervezaService = tipoCervezaService;
        this.facturaItemRepo = facturaItemRepo;
        this.pdfService = pdfService;
    }

    @GetMapping
    public String lista(@RequestParam(required = false) Boolean activa,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        var pagina = service.listarPaginado(activa, page);
        model.addAttribute("recetas",      pagina.getContent());
        model.addAttribute("paginaActual", page);
        model.addAttribute("totalPaginas", pagina.getTotalPages());
        model.addAttribute("totalRecetas", pagina.getTotalElements());
        model.addAttribute("activaFiltro", activa);
        model.addAttribute("baseUrl",      "/recetas");
        model.addAttribute("extraParams",  activa != null ? "&activa=" + activa : "");
        Map<Long, Long> lotesCount = new HashMap<>();
        loteRepo.countPorReceta().forEach(row -> lotesCount.put((Long) row[0], (Long) row[1]));
        model.addAttribute("lotesCountMap", lotesCount);
        return "recetas/lista";
    }

    @GetMapping(value = "/suggest", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<Map<String, Object>> suggest(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) Boolean activa) {
        return service.suggest(q, activa);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTARIO', 'SUPERADMIN')")
    @GetMapping("/nueva")
    public String nueva(Model model) {
        model.addAttribute("recetaForm", RecetaFormDto.empty());
        model.addAttribute("insumosInventario", insumoService.listarTodos());
        model.addAttribute("tiposCerveza", tipoCervezaService.listarActivos());
        return "recetas/formulario";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTARIO', 'SUPERADMIN')")
    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("recetaForm") RecetaFormDto dto,
                          BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) return "recetas/formulario";
        try {
            service.guardar(dto);
            ra.addFlashAttribute("mensaje", "Receta guardada correctamente");
            ra.addFlashAttribute("tipoMensaje", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("mensaje", "Error al guardar: " + e.getMessage());
            ra.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/recetas";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTARIO', 'SUPERADMIN')")
    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("recetaForm", service.toFormDto(service.buscarPorId(id)));
        model.addAttribute("recetaId", id);
        model.addAttribute("insumosInventario", insumoService.listarTodos());
        model.addAttribute("tiposCerveza", tipoCervezaService.listarActivos());
        return "recetas/formulario";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTARIO', 'SUPERADMIN')")
    @PostMapping("/actualizar/{id}")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("recetaForm") RecetaFormDto dto,
                             BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("recetaId", id);
            return "recetas/formulario";
        }
        try {
            service.actualizar(id, dto);
            ra.addFlashAttribute("mensaje", "Receta actualizada correctamente");
            ra.addFlashAttribute("tipoMensaje", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("mensaje", "Error al actualizar: " + e.getMessage());
            ra.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/recetas";
    }

    @GetMapping("/ver/{id}")
    public String ver(@PathVariable Long id, Model model) {
        Receta receta = service.buscarPorId(id);
        model.addAttribute("receta", receta);
        model.addAttribute("lotesDeReceta", loteRepo.findByRecetaId(id));
        calcularCostosEstimados(receta, model);
        return "recetas/detalle";
    }

    @GetMapping("/ver/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id, HttpServletRequest request) {
        Receta receta = service.buscarPorId(id);
        com.alera.model.Tenant tenant = (com.alera.model.Tenant) request.getAttribute("currentTenant");
        com.alera.config.ExportBranding branding = com.alera.config.ExportBranding.from(tenant);
        byte[] bytes = pdfService.generarPdfReceta(receta, branding);
        String filename = "receta-" + receta.getNombre().replaceAll("[^a-zA-Z0-9]", "-") + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(bytes.length)
                .body(bytes);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTARIO', 'SUPERADMIN')")
    @GetMapping("/duplicar/{id}")
    public String duplicar(@PathVariable Long id, Model model) {
        model.addAttribute("recetaForm",      service.duplicarComoFormDto(id));
        model.addAttribute("insumosInventario", insumoService.listarTodos());
        model.addAttribute("tiposCerveza",     tipoCervezaService.listarActivos());
        return "recetas/formulario";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTARIO', 'SUPERADMIN')")
    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        try {
            service.eliminar(id);
            ra.addFlashAttribute("mensaje", "Receta eliminada");
            ra.addFlashAttribute("tipoMensaje", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("mensaje", "Error al eliminar: " + e.getMessage());
            ra.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/recetas";
    }

    @GetMapping("/api/{id}")
    @ResponseBody
    public Map<String, Object> apiReceta(@PathVariable Long id) {
        Receta r = service.buscarPorId(id);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("maltas",       toMaps(r.getMaltas()));
        resp.put("lupulos",      toMaps(r.getLupulos()));
        resp.put("levaduras",    toMaps(r.getLevaduras()));
        resp.put("clarificantes", toMaps(r.getClarificantes()));
        resp.put("escalones", r.getEscalones().stream().map(e -> {
            Map<String, Object> em = new LinkedHashMap<>();
            em.put("nombre", e.getNombre());
            em.put("duracionMinutos", e.getDuracionMinutos());
            em.put("temperaturaC", e.getTemperaturaC());
            return em;
        }).toList());
        if (r.getOgObjetivo() != null) resp.put("ogObjetivo", r.getOgObjetivo());
        if (r.getFgObjetivo() != null) resp.put("fgObjetivo", r.getFgObjetivo());
        if (r.getEstilo() != null)     resp.put("estilo", r.getEstilo());
        if (r.getAguaMacerado() != null) resp.put("aguaMacerado", r.getAguaMacerado());
        if (r.getUnidadAguaMacerado() != null && !r.getUnidadAguaMacerado().isBlank())
            resp.put("unidadAguaMacerado", r.getUnidadAguaMacerado());
        if (r.getVolumenBase() != null) resp.put("volumenBase", r.getVolumenBase());
        if (r.getPhAgua() != null)      resp.put("phAgua", r.getPhAgua());
        return resp;
    }

    private List<Map<String, String>> toMaps(List<com.alera.model.RecetaIngrediente> items) {
        return items.stream().map(ri -> {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("nombre", ri.getNombre());
            String cant = ri.getCantidad();
            if (cant != null && !cant.isBlank()) {
                String[] parts = cant.trim().split("\\s+");
                m.put("cantidad", parts[0]);
                m.put("unidad", parts.length > 1 ? parts[1] : "gr");
            } else {
                m.put("cantidad", "");
                m.put("unidad", "gr");
            }
            return m;
        }).toList();
    }

    private void calcularCostosEstimados(Receta receta, Model model) {
        List<RecetaIngrediente> ingredientes = receta.getIngredientes();
        if (ingredientes.isEmpty()) return;

        List<String> nombres = ingredientes.stream()
                .map(ri -> ri.getNombre().toLowerCase().trim())
                .distinct()
                .collect(Collectors.toList());

        List<FacturaItem> historial = facturaItemRepo.findUltimosPrecios(nombres);

        // Keep only the most recent item per name
        Map<String, FacturaItem> ultimoPorNombre = new LinkedHashMap<>();
        for (FacturaItem fi : historial) {
            ultimoPorNombre.putIfAbsent(fi.getNombre().toLowerCase().trim(), fi);
        }

        List<Map<String, Object>> costos = new ArrayList<>();
        BigDecimal totalEstimado = BigDecimal.ZERO;
        boolean hayAlguno = false;

        for (RecetaIngrediente ri : ingredientes) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("nombre", ri.getNombre());
            item.put("cantidad", ri.getCantidad());

            FacturaItem fi = ultimoPorNombre.get(ri.getNombre().toLowerCase().trim());
            if (fi != null) {
                BigDecimal costoEst = estimarCosto(ri.getCantidad(), fi.getValorUnitario(), fi.getUnidad());
                item.put("valorUnitario", fi.getValorUnitario());
                item.put("unidadFactura", fi.getUnidad() != null ? fi.getUnidad() : "");
                item.put("costoEstimado", costoEst);
                item.put("fechaFactura", fi.getFactura().getFechaFactura());
                if (costoEst != null) {
                    totalEstimado = totalEstimado.add(costoEst);
                    hayAlguno = true;
                }
            } else {
                item.put("costoEstimado", null);
            }
            costos.add(item);
        }

        model.addAttribute("costosIngredientes", costos);
        if (hayAlguno) model.addAttribute("totalCostoEstimado", totalEstimado);
    }

    private BigDecimal estimarCosto(String cantidadTexto, BigDecimal valorUnitario, String unidadFactura) {
        if (cantidadTexto == null || cantidadTexto.isBlank() || valorUnitario == null) return null;
        String[] parts = cantidadTexto.trim().split("\\s+");
        BigDecimal cantidad;
        try {
            cantidad = new BigDecimal(parts[0].replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
        String unidadReceta = parts.length > 1 ? parts[1] : "";

        BigDecimal cantBase = UnidadUtils.convertirAUnidadBase(cantidad, unidadReceta);
        String baseReceta  = UnidadUtils.unidadBase(unidadReceta);

        BigDecimal facturaUnitInBase = UnidadUtils.convertirAUnidadBase(BigDecimal.ONE, unidadFactura);
        String baseFactura = UnidadUtils.unidadBase(unidadFactura);

        if (!baseReceta.equalsIgnoreCase(baseFactura)) {
            // Incompatible units — apply valorUnitario as-is
            return valorUnitario.multiply(cantidad).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal precioPorBase = valorUnitario.divide(facturaUnitInBase, 6, RoundingMode.HALF_UP);
        return cantBase.multiply(precioPorBase).setScale(2, RoundingMode.HALF_UP);
    }
}
