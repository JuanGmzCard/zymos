package com.alera.controller;

import com.alera.repository.InsumoInventarioRepository;
import com.alera.repository.LoteCervezaRepository;
import com.alera.repository.RecetaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/buscar")
public class BusquedaController {

    private final LoteCervezaRepository loteRepo;
    private final RecetaRepository recetaRepo;
    private final InsumoInventarioRepository insumoRepo;

    public BusquedaController(LoteCervezaRepository loteRepo,
                               RecetaRepository recetaRepo,
                               InsumoInventarioRepository insumoRepo) {
        this.loteRepo = loteRepo;
        this.recetaRepo = recetaRepo;
        this.insumoRepo = insumoRepo;
    }

    @GetMapping(value = "/suggest", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> suggest(@RequestParam(defaultValue = "") String q) {
        if (q.isBlank() || q.trim().length() < 2) {
            return Map.of("lotes", List.of(), "recetas", List.of(), "insumos", List.of());
        }
        String t = q.trim();

        List<Map<String, Object>> lotes = loteRepo.search(t, PageRequest.of(0, 4)).stream()
            .map(l -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("titulo", l.getCodigoLote());
                m.put("sub",    l.getEstilo() != null ? l.getEstilo() : "");
                m.put("url",    "/ver/" + l.getId());
                return m;
            }).toList();

        List<Map<String, Object>> recetas = recetaRepo.search(t, PageRequest.of(0, 4)).stream()
            .map(r -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("titulo", r.getNombre());
                m.put("sub",    r.getEstilo() != null ? r.getEstilo() : "");
                m.put("url",    "/recetas/ver/" + r.getId());
                return m;
            }).toList();

        List<Map<String, Object>> insumos = insumoRepo.findByFiltros(t, null, PageRequest.of(0, 4))
            .getContent().stream()
            .map(i -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("titulo", i.getNombre());
                m.put("sub",    i.getTipo() != null ? i.getTipo().name() : "");
                m.put("url",    "/inventario/editar/" + i.getId());
                return m;
            }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("lotes",   lotes);
        result.put("recetas", recetas);
        result.put("insumos", insumos);
        return result;
    }

    @GetMapping
    public String buscar(@RequestParam(defaultValue = "") String q, Model model) {
        if (q.isBlank()) return "redirect:/";

        String t = q.trim();
        var lotes    = loteRepo.search(t, PageRequest.of(0, 6));
        var recetas  = recetaRepo.search(t, PageRequest.of(0, 6));
        var insumos  = insumoRepo.findByFiltros(t, null, PageRequest.of(0, 6)).getContent();

        model.addAttribute("q",       q);
        model.addAttribute("lotes",   lotes);
        model.addAttribute("recetas", recetas);
        model.addAttribute("insumos", insumos);
        model.addAttribute("total",   lotes.size() + recetas.size() + insumos.size());
        return "busqueda";
    }
}
