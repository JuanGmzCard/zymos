package com.alera.controller;

import com.alera.model.LoteCerveza;
import com.alera.repository.LoteCervezaRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/calendario")
public class CalendarioController {

    private final LoteCervezaRepository loteRepo;

    public CalendarioController(LoteCervezaRepository loteRepo) {
        this.loteRepo = loteRepo;
    }

    @GetMapping
    public String calendario(Model model) {
        return "calendario";
    }

    @GetMapping("/eventos")
    @ResponseBody
    public List<Map<String, Object>> eventos() {
        List<LoteCerveza> lotes = loteRepo.findParaKanban(LocalDate.now().minusDays(30));
        List<Map<String, Object>> eventos = new ArrayList<>();

        for (LoteCerveza l : lotes) {
            String url = "/ver/" + l.getId();
            String base = l.getCodigoLote() + " · ";

            agregarEvento(eventos, base + "Fermentación",  "#2e7d32",
                    l.getFermFechaInicial(),     l.getFermFechaFinal() != null ? l.getFermFechaFinal().plusDays(1) : l.getFermFechaFinalIdeal(), url);

            agregarEvento(eventos, base + "Acondic.",     "#0e6b7a",
                    l.getAcondFechaInicial(),    l.getAcondFechaFinal() != null ? l.getAcondFechaFinal().plusDays(1) : l.getAcondFechaFinalIdeal(), url);

            agregarEvento(eventos, base + "Maduración",   "#a07800",
                    l.getMadurFechaInicial(),    l.getMadurFechaFinal() != null ? l.getMadurFechaFinal().plusDays(1) : l.getMadurFechaFinalIdeal(), url);

            agregarEvento(eventos, base + "Carbonatación","#0d5ea8",
                    l.getCarbFechaInicial(),     l.getCarbFechaFinal() != null ? l.getCarbFechaFinal().plusDays(1) : l.getCarbFechaFinalIdeal(), url);
        }
        return eventos;
    }

    private void agregarEvento(List<Map<String, Object>> lista, String title, String color,
                                LocalDate start, LocalDate end, String url) {
        if (start == null) return;
        Map<String, Object> ev = new LinkedHashMap<>();
        ev.put("title", title);
        ev.put("start", start.toString());
        if (end != null) ev.put("end", end.toString());
        ev.put("color", color);
        ev.put("url", url);
        lista.add(ev);
    }
}
