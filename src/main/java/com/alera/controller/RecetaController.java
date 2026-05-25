package com.alera.controller;

import com.alera.dto.RecetaFormDto;
import com.alera.model.Receta;
import com.alera.repository.LoteCervezaRepository;
import com.alera.service.InsumoInventarioService;
import com.alera.service.RecetaService;
import com.alera.service.TipoCervezaService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/recetas")
public class RecetaController {

    private final RecetaService service;
    private final LoteCervezaRepository loteRepo;
    private final InsumoInventarioService insumoService;
    private final TipoCervezaService tipoCervezaService;

    public RecetaController(RecetaService service, LoteCervezaRepository loteRepo,
                            InsumoInventarioService insumoService,
                            TipoCervezaService tipoCervezaService) {
        this.service = service;
        this.loteRepo = loteRepo;
        this.insumoService = insumoService;
        this.tipoCervezaService = tipoCervezaService;
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

    @GetMapping("/nueva")
    public String nueva(Model model) {
        model.addAttribute("recetaForm", RecetaFormDto.empty());
        model.addAttribute("insumosInventario", insumoService.listarTodos());
        model.addAttribute("tiposCerveza", tipoCervezaService.listarActivos());
        return "recetas/formulario";
    }

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

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("recetaForm", service.toFormDto(service.buscarPorId(id)));
        model.addAttribute("recetaId", id);
        model.addAttribute("insumosInventario", insumoService.listarTodos());
        model.addAttribute("tiposCerveza", tipoCervezaService.listarActivos());
        return "recetas/formulario";
    }

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
        return "recetas/detalle";
    }

    @GetMapping("/duplicar/{id}")
    public String duplicar(@PathVariable Long id, Model model) {
        model.addAttribute("recetaForm",      service.duplicarComoFormDto(id));
        model.addAttribute("insumosInventario", insumoService.listarTodos());
        model.addAttribute("tiposCerveza",     tipoCervezaService.listarActivos());
        return "recetas/formulario";
    }

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
}
