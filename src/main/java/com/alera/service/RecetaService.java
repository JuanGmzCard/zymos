package com.alera.service;

import com.alera.config.UnidadUtils;
import com.alera.dto.InsumoDto;
import com.alera.dto.RecetaFormDto;
import com.alera.model.AdicionHervor;
import com.alera.model.EscalonMacerado;
import com.alera.model.Receta;
import com.alera.model.RecetaIngrediente;
import com.alera.model.enums.TipoIngrediente;
import com.alera.repository.RecetaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class RecetaService {

    private static final Logger log = LoggerFactory.getLogger(RecetaService.class);

    @Value("${app.page-size:15}")
    private int pageSize;

    private final RecetaRepository repo;

    public RecetaService(RecetaRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<Receta> listarActivas() {
        return repo.findAllByActivaTrueOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public List<Receta> listarTodas() {
        return repo.findAllByOrderByActivaDescNombreAsc();
    }

    @Transactional(readOnly = true)
    public Page<Receta> listarPaginado(Boolean activa, int page) {
        var pageable = PageRequest.of(page, pageSize);
        if (activa != null) {
            return repo.findByActivaOrderByNombreAsc(activa, pageable);
        }
        return repo.findAllByOrderByActivaDescNombreAsc(pageable);
    }

    @Transactional(readOnly = true)
    public Receta buscarPorId(Long id) {
        return repo.findById(id).orElseThrow(() -> new RuntimeException("Receta no encontrada: " + id));
    }

    public Receta guardar(RecetaFormDto dto) {
        Receta receta = new Receta();
        mapDtoToEntity(dto, receta);
        Receta saved = repo.save(receta);
        log.info("Receta creada: {} ({})", saved.getNombre(), saved.getId());
        return saved;
    }

    public Receta actualizar(Long id, RecetaFormDto dto) {
        Receta receta = buscarPorId(id);
        receta.getIngredientes().clear();
        receta.getEscalones().clear();
        receta.getAdicionesHervor().clear();
        mapDtoToEntity(dto, receta);
        receta.setVersion((receta.getVersion() == null ? 1 : receta.getVersion()) + 1);
        Receta saved = repo.save(receta);
        log.info("Receta actualizada: {} ({}) — v{}", saved.getNombre(), saved.getId(), saved.getVersion());
        return saved;
    }

    public void eliminar(Long id) {
        repo.findById(id).ifPresent(r -> {
            r.setDeletedAt(java.time.LocalDateTime.now());
            repo.save(r);
            log.info("Receta archivada: {} ({})", r.getNombre(), id);
        });
    }

    public RecetaFormDto duplicarComoFormDto(Long id) {
        Receta r = buscarPorId(id);
        RecetaFormDto dto = toFormDto(r);
        dto.setId(null);
        dto.setNombre(r.getNombre() + " (Copia)");
        return dto;
    }

    public RecetaFormDto toFormDto(Receta r) {
        RecetaFormDto dto = new RecetaFormDto();
        dto.setId(r.getId());
        dto.setNombre(r.getNombre());
        dto.setEstilo(r.getEstilo());
        dto.setDescripcion(r.getDescripcion());
        dto.setActiva(r.isActiva());
        dto.setAguaMacerado(r.getAguaMacerado());
        dto.setUnidadAguaMacerado(r.getUnidadAguaMacerado());
        dto.setAguaSparge(r.getAguaSparge());
        dto.setUnidadAguaSparge(r.getUnidadAguaSparge());
        dto.setTiempoHervorMinutos(r.getTiempoHervorMinutos());
        dto.setOgObjetivo(r.getOgObjetivo());
        dto.setFgObjetivo(r.getFgObjetivo());
        dto.setVolumenBase(r.getVolumenBase());
        dto.setNotas(r.getNotas());

        r.getMaltas().forEach(i      -> dto.getMaltas().add(parseInsumoDto(i)));
        r.getLupulos().forEach(i     -> dto.getLupulos().add(parseInsumoDto(i)));
        r.getLevaduras().forEach(i   -> dto.getLevaduras().add(parseInsumoDto(i)));
        r.getClarificantes().forEach(i -> dto.getClarificantes().add(parseInsumoDto(i)));

        if (dto.getMaltas().isEmpty())       dto.getMaltas().add(new InsumoDto());
        if (dto.getLupulos().isEmpty())      dto.getLupulos().add(new InsumoDto());
        if (dto.getLevaduras().isEmpty())    dto.getLevaduras().add(new InsumoDto());
        if (dto.getClarificantes().isEmpty()) dto.getClarificantes().add(new InsumoDto());

        r.getEscalones().forEach(e -> {
            RecetaFormDto.EscalonDto ed = new RecetaFormDto.EscalonDto();
            ed.setNombre(e.getNombre());
            ed.setDuracionMinutos(e.getDuracionMinutos());
            ed.setTemperaturaC(e.getTemperaturaC());
            dto.getEscalones().add(ed);
        });

        r.getAdicionesHervor().forEach(a -> {
            RecetaFormDto.AdicionHervorDto ad = new RecetaFormDto.AdicionHervorDto();
            ad.setNombre(a.getNombre());
            ad.setMinutosRestantes(a.getMinutosRestantes());
            ad.setCantidad(a.getCantidad());
            ad.setUnidad(a.getUnidad());
            dto.getAdicionesHervor().add(ad);
        });

        return dto;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> suggest(String q, Boolean activa) {
        if (q == null || q.isBlank() || q.trim().length() < 2) return List.of();
        return repo.search(q.trim(), PageRequest.of(0, 10)).stream()
            .filter(r -> activa == null || r.isActiva() == activa)
            .limit(6)
            .map(r -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("nombre", r.getNombre());
                m.put("estilo", r.getEstilo() != null ? r.getEstilo() : "");
                m.put("activa", r.isActiva());
                m.put("url",    "/recetas/ver/" + r.getId());
                return m;
            }).toList();
    }

    private void mapDtoToEntity(RecetaFormDto dto, Receta receta) {
        receta.setNombre(dto.getNombre());
        receta.setEstilo(dto.getEstilo());
        receta.setDescripcion(dto.getDescripcion());
        receta.setActiva(dto.isActiva());
        receta.setAguaMacerado(dto.getAguaMacerado());
        receta.setUnidadAguaMacerado(dto.getUnidadAguaMacerado());
        receta.setAguaSparge(dto.getAguaSparge());
        receta.setUnidadAguaSparge(dto.getUnidadAguaSparge());
        receta.setTiempoHervorMinutos(dto.getTiempoHervorMinutos());
        receta.setOgObjetivo(dto.getOgObjetivo());
        receta.setFgObjetivo(dto.getFgObjetivo());
        receta.setVolumenBase(dto.getVolumenBase());
        receta.setNotas(dto.getNotas());

        addIngredientes(dto.getMaltas(),       TipoIngrediente.MALTA,        receta);
        addIngredientes(dto.getLupulos(),      TipoIngrediente.LUPULO,       receta);
        addIngredientes(dto.getLevaduras(),    TipoIngrediente.LEVADURA,     receta);
        addIngredientes(dto.getClarificantes(), TipoIngrediente.CLARIFICANTE, receta);

        if (dto.getEscalones() != null) {
            for (int i = 0; i < dto.getEscalones().size(); i++) {
                RecetaFormDto.EscalonDto ed = dto.getEscalones().get(i);
                if (ed.getNombre() == null || ed.getNombre().isBlank()) continue;
                EscalonMacerado esc = new EscalonMacerado();
                esc.setReceta(receta);
                esc.setNombre(ed.getNombre().trim());
                esc.setDuracionMinutos(ed.getDuracionMinutos());
                esc.setTemperaturaC(ed.getTemperaturaC());
                esc.setOrden(i);
                receta.getEscalones().add(esc);
            }
        }

        if (dto.getAdicionesHervor() != null) {
            for (int i = 0; i < dto.getAdicionesHervor().size(); i++) {
                RecetaFormDto.AdicionHervorDto ad = dto.getAdicionesHervor().get(i);
                if (ad.isEmpty()) continue;
                AdicionHervor adicion = new AdicionHervor();
                adicion.setReceta(receta);
                adicion.setNombre(ad.getNombre() != null ? ad.getNombre().trim() : "");
                adicion.setMinutosRestantes(ad.getMinutosRestantes());
                adicion.setCantidad(ad.getCantidad());
                adicion.setUnidad(ad.getUnidad());
                adicion.setOrden(i);
                receta.getAdicionesHervor().add(adicion);
            }
        }
    }

    private void addIngredientes(List<InsumoDto> items, TipoIngrediente tipo, Receta receta) {
        if (items == null) return;
        for (InsumoDto dto : items) {
            if (dto.isEmpty()) continue;
            RecetaIngrediente ri = new RecetaIngrediente();
            ri.setReceta(receta);
            ri.setTipo(tipo);
            ri.setNombre(dto.getNombre().trim());
            ri.setCantidad(UnidadUtils.normalizarParaAlmacenamiento(dto.getCantidad(), dto.getUnidad()));
            receta.getIngredientes().add(ri);
        }
    }

    private InsumoDto parseInsumoDto(RecetaIngrediente ri) {
        InsumoDto dto = new InsumoDto();
        dto.setNombre(ri.getNombre());
        String cant = ri.getCantidad();
        if (cant != null && !cant.isBlank()) {
            String[] parts = cant.trim().split("\\s+");
            dto.setCantidad(parts[0]);
            dto.setUnidad(parts.length > 1 ? parts[1] : "gr");
        }
        return dto;
    }
}
