package com.alera.controller;

import com.alera.dto.LoteFormDto;
import com.alera.dto.InsumoDto;
import com.alera.model.FacturaItem;
import com.alera.model.LoteCerveza;
import com.alera.model.RecetaIngrediente;
import com.alera.model.Tenant;
import com.alera.model.enums.EstadoPlanificacion;
import com.alera.service.PdfExportService;
import com.alera.service.PlanificacionService;
import com.alera.service.VentaService;
import com.alera.model.EvaluacionSensorial;
import com.alera.model.LecturaFermentacion;
import com.alera.service.EvaluacionSensorialService;
import com.alera.service.LecturaFermentacionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.alera.repository.FacturaItemRepository;
import com.alera.repository.InsumoInventarioRepository;
import com.alera.repository.LoteCervezaRepository;
import com.alera.repository.TipoCervezaRepository;
import com.alera.service.EquipoService;
import com.alera.service.RecetaService;
import com.alera.service.TrazabilidadService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/")
public class TrazabilidadController {

    private final TrazabilidadService service;
    private final EquipoService equipoService;
    private final RecetaService recetaService;
    private final InsumoInventarioRepository insumoRepo;
    private final TipoCervezaRepository tipoCervezaRepo;
    private final FacturaItemRepository facturaItemRepo;
    private final PdfExportService pdfExportService;
    private final LecturaFermentacionService lecturaService;
    private final EvaluacionSensorialService evaluacionService;
    private final PlanificacionService planificacionService;
    private final VentaService ventaService;
    private final LoteCervezaRepository loteRepo;

    public TrazabilidadController(TrazabilidadService service,
                                   EquipoService equipoService,
                                   RecetaService recetaService,
                                   InsumoInventarioRepository insumoRepo,
                                   TipoCervezaRepository tipoCervezaRepo,
                                   FacturaItemRepository facturaItemRepo,
                                   PdfExportService pdfExportService,
                                   LecturaFermentacionService lecturaService,
                                   EvaluacionSensorialService evaluacionService,
                                   PlanificacionService planificacionService,
                                   VentaService ventaService,
                                   LoteCervezaRepository loteRepo) {
        this.service = service;
        this.equipoService = equipoService;
        this.recetaService = recetaService;
        this.insumoRepo = insumoRepo;
        this.tipoCervezaRepo = tipoCervezaRepo;
        this.facturaItemRepo = facturaItemRepo;
        this.pdfExportService = pdfExportService;
        this.lecturaService = lecturaService;
        this.evaluacionService = evaluacionService;
        this.planificacionService = planificacionService;
        this.ventaService = ventaService;
        this.loteRepo = loteRepo;
    }

    @GetMapping
    public String index(
            @RequestParam(defaultValue = "") String estilo,
            @RequestParam(defaultValue = "") String fase,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        var paginaLotes = service.listarPaginado(estilo, fase, desde, hasta, page);
        model.addAttribute("lotes",        paginaLotes.getContent());
        model.addAttribute("paginaActual", page);
        model.addAttribute("totalPaginas", paginaLotes.getTotalPages());
        model.addAttribute("totalLotes",   paginaLotes.getTotalElements());
        model.addAttribute("estiloFiltro", estilo);
        model.addAttribute("faseFiltro",   fase);
        model.addAttribute("desdeFiltro",  desde);
        model.addAttribute("hastaFiltro",  hasta);
        model.addAttribute("statsTotalLotes",  loteRepo.count());
        model.addAttribute("statsEnProceso",   loteRepo.countEnProceso());
        model.addAttribute("statsCompletados", loteRepo.countCompletados());
        model.addAttribute("statsEstilos",     loteRepo.countDistinctEstilos());
        return "trazabilidad/index";
    }

    @GetMapping(value = "/suggest", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<Map<String, Object>> suggest(@RequestParam(defaultValue = "") String q) {
        return service.suggest(q);
    }

    @GetMapping(value = "/suggest-items", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<Map<String, Object>> suggestItems(@RequestParam(defaultValue = "") String q,
                                                    @RequestParam(defaultValue = "") String tipo) {
        var pagina = facturaItemRepo.search(q.trim(), tipo.trim(), org.springframework.data.domain.PageRequest.of(0, 30));
        return pagina.getContent().stream().map(this::mapFacturaItem).toList();
    }

    @GetMapping(value = "/suggest-items-por-nombre", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Map<String, Object>> suggestItemsPorNombre(@RequestParam(required = false) List<String> nombres) {
        if (nombres == null || nombres.isEmpty()) return Map.of();
        var nombresNorm = nombres.stream().map(n -> n.toLowerCase().trim()).filter(n -> !n.isEmpty()).distinct().toList();
        if (nombresNorm.isEmpty()) return Map.of();
        var result = new LinkedHashMap<String, Map<String, Object>>();
        facturaItemRepo.findByNombresIn(nombresNorm).forEach(item -> {
            String key = item.getNombre().toLowerCase().trim();
            result.putIfAbsent(key, mapFacturaItem(item));
        });
        return result;
    }

    @GetMapping("/kanban")
    public String kanban(Model model) {
        var lotes = service.listarParaKanban();
        model.addAttribute("sinIniciar",       lotes.stream().filter(l -> l.getFermFechaInicial() == null).toList());
        model.addAttribute("fermentacion",     lotes.stream().filter(l -> l.getFermFechaInicial() != null && l.getAcondFechaInicial() == null && !l.isCompletado()).toList());
        model.addAttribute("acondicionamiento",lotes.stream().filter(l -> l.getAcondFechaInicial() != null && l.getMadurFechaInicial() == null && !l.isCompletado()).toList());
        model.addAttribute("maduracion",       lotes.stream().filter(l -> l.getMadurFechaInicial() != null && l.getCarbFechaInicial() == null && !l.isCompletado()).toList());
        model.addAttribute("carbonatacion",    lotes.stream().filter(l -> l.getCarbFechaInicial() != null && !l.isCompletado()).toList());
        model.addAttribute("completados",      lotes.stream().filter(l -> l.isCompletado()).toList());
        return "trazabilidad/kanban";
    }

    @GetMapping("/nuevo")
    public String nuevo(@RequestParam(required = false) Long planId, Model model) {
        LoteFormDto dto = LoteFormDto.empty();
        if (planId != null) {
            planificacionService.buscarConRecetaEIngredientes(planId).ifPresent(plan -> {
                dto.setEstilo(plan.getNombreElaboracion());
                dto.setFechaElaboracion(plan.getFechaPlaneada());
                if (plan.getVolumenEstimado() != null) {
                    dto.setLitrosFinales(plan.getVolumenEstimado());
                }
                if (plan.getReceta() != null) {
                    var receta = plan.getReceta();
                    dto.setRecetaId(receta.getId());
                    if (receta.getOgObjetivo() != null) dto.setDensidadInicial(receta.getOgObjetivo());
                    if (receta.getFgObjetivo() != null)  dto.setDensidadFinal(receta.getFgObjetivo());
                    var maltas = receta.getMaltas();
                    var lupulos = receta.getLupulos();
                    var levaduras = receta.getLevaduras();
                    var clarificantes = receta.getClarificantes();
                    if (!maltas.isEmpty())       dto.setMaltas(toInsumoDtoList(maltas));
                    if (!lupulos.isEmpty())      dto.setLupulos(toInsumoDtoList(lupulos));
                    if (!levaduras.isEmpty())    dto.setLevaduras(toInsumoDtoList(levaduras));
                    if (!clarificantes.isEmpty()) dto.setClarificantes(toInsumoDtoList(clarificantes));
                }
                planificacionService.cambiarEstado(planId, EstadoPlanificacion.EN_PROCESO);
            });
        }
        model.addAttribute("loteForm", dto);
        agregarInventarioAlModelo(model);
        return "trazabilidad/formulario";
    }

    private List<InsumoDto> toInsumoDtoList(List<RecetaIngrediente> ingredientes) {
        return ingredientes.stream().map(ri -> {
            String c = ri.getCantidad();
            if (c == null || c.isBlank()) return new InsumoDto(ri.getNombre(), "", "gr");
            int idx = c.indexOf(' ');
            if (idx > 0) return new InsumoDto(ri.getNombre(), c.substring(0, idx), c.substring(idx + 1));
            return new InsumoDto(ri.getNombre(), c, "gr");
        }).collect(Collectors.toList());
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("loteForm") LoteFormDto dto,
                          BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            agregarInventarioAlModelo(model);
            return "trazabilidad/formulario";
        }
        if (dto.getFermFechaInicial() != null && dto.getEquipoFermentadorId() == null) {
            result.rejectValue("equipoFermentadorId", "required",
                    "Seleccione un fermentador para registrar la fecha de inicio de fermentación.");
            agregarInventarioAlModelo(model);
            return "trazabilidad/formulario";
        }
        try {
            var resultado = service.guardar(dto);
            // Primer registro en curva de fermentación: OG + temperatura del tab de fermentación
            if (dto.getDensidadInicial() != null) {
                LocalDate fechaLectura = dto.getFermFechaInicial() != null
                        ? dto.getFermFechaInicial() : dto.getFechaElaboracion();
                if (fechaLectura != null) {
                    lecturaService.agregar(resultado.getLote().getId(), fechaLectura,
                            dto.getDensidadInicial(), dto.getFermTemperatura(), null);
                }
            }
            if (resultado.tieneAdvertencias()) {
                ra.addFlashAttribute("mensaje", "Lote creado. ⚠️ " + resultado.getMensajeAdvertencias());
                ra.addFlashAttribute("tipoMensaje", "warning");
            } else {
                ra.addFlashAttribute("mensaje", "Lote creado correctamente");
                ra.addFlashAttribute("tipoMensaje", "success");
            }
        } catch (Exception e) {
            ra.addFlashAttribute("mensaje", "Error al crear lote: " + e.getMessage());
            ra.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        var lote = service.buscarPorId(id);
        model.addAttribute("loteForm", service.toLoteFormDto(lote));
        model.addAttribute("loteId", id);
        model.addAttribute("codigoLote", lote.getCodigoLote());
        agregarInventarioAlModelo(model);
        // Si el lote tiene fermentador asignado y no está en la lista (en uso por este lote),
        // lo agregamos para que aparezca seleccionado en el formulario
        if (lote.getEquipoFermentador() != null) {
            @SuppressWarnings("unchecked")
            var fermentadores = (java.util.List<com.alera.model.Equipo>) model.getAttribute("fermentadores");
            boolean yaEsta = fermentadores != null &&
                fermentadores.stream().anyMatch(f -> f.getId().equals(lote.getEquipoFermentador().getId()));
            if (!yaEsta && fermentadores != null) {
                fermentadores.add(0, lote.getEquipoFermentador());
            }
        }
        return "trazabilidad/formulario";
    }

    @PostMapping("/actualizar/{id}")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("loteForm") LoteFormDto dto,
                             BindingResult result, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            agregarInventarioAlModelo(model);
            model.addAttribute("loteId", id);
            return "trazabilidad/formulario";
        }
        if (dto.getFermFechaInicial() != null && dto.getEquipoFermentadorId() == null) {
            result.rejectValue("equipoFermentadorId", "required",
                    "Seleccione un fermentador para registrar la fecha de inicio de fermentación.");
            agregarInventarioAlModelo(model);
            model.addAttribute("loteId", id);
            return "trazabilidad/formulario";
        }
        try {
            var resultado = service.actualizar(id, dto);
            if (resultado.tieneAdvertencias()) {
                ra.addFlashAttribute("mensaje", "Lote actualizado. ⚠️ " + resultado.getMensajeAdvertencias());
                ra.addFlashAttribute("tipoMensaje", "warning");
            } else {
                ra.addFlashAttribute("mensaje", "Lote actualizado correctamente");
                ra.addFlashAttribute("tipoMensaje", "success");
            }
        } catch (Exception e) {
            ra.addFlashAttribute("mensaje", "Error al actualizar: " + e.getMessage());
            ra.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/";
    }

    @GetMapping("/ver/{id}")
    public String ver(@PathVariable Long id, Model model) {
        var lote     = service.buscarPorId(id);
        List<LecturaFermentacion> lecturas = lecturaService.listarPorLote(id);
        List<EvaluacionSensorial> evaluaciones = evaluacionService.listarPorLote(id);

        // Datos para Chart.js — arrays paralelos de fecha, densidad y temperatura
        model.addAttribute("chartFechas",
                lecturas.stream().map(l -> l.getFecha().toString()).collect(Collectors.toList()));
        model.addAttribute("chartDensidad",
                lecturas.stream().map(LecturaFermentacion::getDensidad).collect(Collectors.toList()));
        model.addAttribute("chartTemp",
                lecturas.stream().map(LecturaFermentacion::getTemperatura).collect(Collectors.toList()));

        model.addAttribute("lote",     lote);
        model.addAttribute("historial", service.obtenerHistorial(id));
        model.addAttribute("lecturas",  lecturas);
        model.addAttribute("evaluaciones", evaluaciones);
        model.addAttribute("promedioEvaluacion",
                evaluaciones.isEmpty() ? null : evaluacionService.calcularPromedio(evaluaciones));
        model.addAttribute("ventasLote", ventaService.listarPorLote(id));
        return "trazabilidad/detalle";
    }

    @PostMapping("/ver/{id}/lecturas/agregar")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public String agregarLectura(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) Integer densidad,
            @RequestParam(required = false) BigDecimal temperatura,
            @RequestParam(required = false) String notas,
            RedirectAttributes ra) {
        lecturaService.agregar(id, fecha, densidad, temperatura, notas);
        ra.addFlashAttribute("mensaje", "Lectura registrada correctamente");
        ra.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/ver/" + id;
    }

    @PostMapping("/ver/{id}/lecturas/{lecturaId}/eliminar")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public String eliminarLectura(@PathVariable Long id,
                                   @PathVariable Long lecturaId,
                                   RedirectAttributes ra) {
        lecturaService.eliminar(lecturaId);
        ra.addFlashAttribute("mensaje", "Lectura eliminada");
        ra.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/ver/" + id;
    }

    @PostMapping("/ver/{id}/evaluaciones/agregar")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public String agregarEvaluacion(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) String catador,
            @RequestParam(required = false) Integer aroma,
            @RequestParam(required = false) Integer apariencia,
            @RequestParam(required = false) Integer sabor,
            @RequestParam(required = false) Integer sensacionBoca,
            @RequestParam(required = false) Integer impresionGeneral,
            @RequestParam(required = false) String notas,
            RedirectAttributes ra) {
        evaluacionService.agregar(id, fecha, catador, aroma, apariencia, sabor,
                sensacionBoca, impresionGeneral, notas);
        ra.addFlashAttribute("mensaje", "Evaluación sensorial registrada");
        ra.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/ver/" + id;
    }

    @PostMapping("/ver/{id}/evaluaciones/{evalId}/eliminar")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public String eliminarEvaluacion(@PathVariable Long id,
                                      @PathVariable Long evalId,
                                      RedirectAttributes ra) {
        evaluacionService.eliminar(evalId);
        ra.addFlashAttribute("mensaje", "Evaluación eliminada");
        ra.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/ver/" + id;
    }

    @GetMapping("/ver/{id}/pdf")
    public ResponseEntity<byte[]> verPdf(@PathVariable Long id, HttpServletRequest request) {
        LoteCerveza lote = service.buscarPorId(id);
        Tenant tenant = (Tenant) request.getAttribute("currentTenant");
        com.alera.config.ExportBranding branding = com.alera.config.ExportBranding.from(tenant);
        List<LecturaFermentacion> lecturas = lecturaService.listarPorLote(id);
        byte[] pdf = pdfExportService.generarPdfLote(lote, branding, lecturas);
        String filename = "lote-" + lote.getCodigoLote().toLowerCase() + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdf);
    }

    @GetMapping("/duplicar/{id}")
    public String duplicar(@PathVariable Long id, Model model) {
        var lote = service.buscarPorId(id);
        var dto = service.toLoteFormDto(lote);
        dto.setFechaElaboracion(null);
        dto.setDensidadInicial(null);
        dto.setDensidadFinal(null);
        dto.setDensidadFinalFecha(null);
        dto.setFermFechaInicial(null); dto.setFermFechaFinalIdeal(null); dto.setFermFechaFinal(null);
        dto.setAcondFechaInicial(null); dto.setAcondFechaFinalIdeal(null); dto.setAcondFechaFinal(null);
        dto.setMadurFechaInicial(null); dto.setMadurFechaFinalIdeal(null); dto.setMadurFechaFinal(null);
        dto.setCarbFechaInicial(null); dto.setCarbFechaFinalIdeal(null); dto.setCarbFechaFinal(null);
        dto.setCarbCo2Real(null);
        dto.setCarbValidacion(null);
        dto.setCarbDestino(null);
        dto.setNotasCata(null);
        dto.setObservaciones(null);
        dto.setRecetaId(null);
        dto.setItemsIds(new java.util.ArrayList<>());
        dto.setItemsCantidades(new java.util.ArrayList<>());
        model.addAttribute("loteForm", dto);
        model.addAttribute("duplicadoDe", lote.getCodigoLote());
        agregarInventarioAlModelo(model);
        return "trazabilidad/formulario";
    }

    @PostMapping("/actualizar/{id}/fase")
    @ResponseBody
    public ResponseEntity<java.util.Map<String, Object>> moverFase(
            @PathVariable Long id,
            @RequestParam String fase) {
        try {
            service.moverFase(id, fase);
            return ResponseEntity.ok(java.util.Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        try {
            service.eliminar(id);
            ra.addFlashAttribute("mensaje", "Lote eliminado");
            ra.addFlashAttribute("tipoMensaje", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("mensaje", "Error al eliminar: " + e.getMessage());
            ra.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/";
    }

    private void agregarInventarioAlModelo(Model model) {
        var todosInsumos = insumoRepo.findAll();
        model.addAttribute("maltasInventario", todosInsumos.stream()
                .filter(i -> "Malta".equals(i.getTipo())).toList());
        model.addAttribute("lupulosInventario", todosInsumos.stream()
                .filter(i -> "Lúpulo".equals(i.getTipo())).toList());
        model.addAttribute("levaduraInventario", todosInsumos.stream()
                .filter(i -> "Levadura".equals(i.getTipo())).toList());
        model.addAttribute("clarificantesInventario", todosInsumos.stream()
                .filter(i -> "Clarificante".equals(i.getTipo())).toList());
        model.addAttribute("agentesCarbonatacion", todosInsumos.stream()
                .filter(i -> "Agente de Carbonatación".equals(i.getTipo())).toList());
        var stockList = todosInsumos.stream().map(i -> {
            var ms = new java.util.LinkedHashMap<String, Object>();
            ms.put("nombre", i.getNombre() != null ? i.getNombre().toLowerCase().trim() : "");
            ms.put("tipo", i.getTipo());
            ms.put("cantidad", i.getCantidad());
            ms.put("unidad", i.getUnidad());
            return ms;
        }).collect(java.util.stream.Collectors.toList());
        model.addAttribute("inventarioStock", stockList);
        model.addAttribute("fermentadores", equipoService.listarFermentadoresDisponibles());
        model.addAttribute("tiposCerveza", tipoCervezaRepo.findByActivoTrueOrderByNombreAsc());
        model.addAttribute("recetasActivas", recetaService.listarActivas());
        // Solo precargar los ítems de factura ya asignados a este lote (la búsqueda del resto es vía AJAX)
        var loteForm = (LoteFormDto) model.getAttribute("loteForm");
        var itemsIds = (loteForm != null && loteForm.getItemsIds() != null) ? loteForm.getItemsIds() : List.<Long>of();
        var itemsAsignados = itemsIds.isEmpty()
                ? List.<Map<String, Object>>of()
                : facturaItemRepo.findByIdIn(itemsIds).stream().map(this::mapFacturaItem).toList();
        model.addAttribute("itemsFacturaAsignados", itemsAsignados);
        model.addAttribute("hayItemsFactura", facturaItemRepo.count() > 0);
    }

    private Map<String, Object> mapFacturaItem(FacturaItem item) {
        var f = item.getFactura();
        String numFac = (f.getNumeroFactura() != null && !f.getNumeroFactura().isBlank())
                ? f.getNumeroFactura() : "#" + f.getId();
        String fechaStr = f.getFechaFactura() != null
                ? f.getFechaFactura().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : null;
        var m = new LinkedHashMap<String, Object>();
        m.put("id", item.getId());
        m.put("nombre", item.getNombre());
        m.put("tipoInsumo", item.getTipoInsumo());
        m.put("unidad", item.getUnidad());
        m.put("cantidad", item.getCantidad());
        m.put("valorLinea", item.getValorLinea());
        m.put("facturaId", f.getId());
        m.put("facturaNumero", numFac);
        m.put("proveedor", f.getProveedor());
        m.put("fechaFactura", fechaStr);
        return m;
    }
}
