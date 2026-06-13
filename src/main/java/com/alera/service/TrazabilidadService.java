package com.alera.service;

import com.alera.exception.LoteNoEncontradoException;
import com.alera.config.TenantContext;
import com.alera.config.UnidadUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import com.alera.dto.InsumoDto;
import com.alera.dto.LoteFormDto;
import com.alera.dto.LoteGuardadoResult;
import com.alera.mapper.LoteMapper;
import com.alera.model.*;
import com.alera.model.enums.TipoIngrediente;
import com.alera.repository.*;
import org.slf4j.Logger;
import org.springframework.security.core.context.SecurityContextHolder;
import org.slf4j.LoggerFactory;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class TrazabilidadService {

    private static final Logger log = LoggerFactory.getLogger(TrazabilidadService.class);

    private final LoteCervezaRepository loteRepo;
    private final EquipoRepository equipoRepo;
    private final RecetaRepository recetaRepo;
    private final FacturaItemRepository facturaItemRepo;
    private final HistorialLoteRepository historialRepo;
    private final InsumoInventarioService insumoService;
    private final LoteMapper loteMapper;
    private final EntityManager em;
    private final com.alera.repository.TenantRepository tenantRepo;

    public TrazabilidadService(LoteCervezaRepository loteRepo,
                                EquipoRepository equipoRepo,
                                RecetaRepository recetaRepo,
                                FacturaItemRepository facturaItemRepo,
                                HistorialLoteRepository historialRepo,
                                InsumoInventarioService insumoService,
                                LoteMapper loteMapper,
                                EntityManager em,
                                com.alera.repository.TenantRepository tenantRepo) {
        this.loteRepo = loteRepo;
        this.equipoRepo = equipoRepo;
        this.recetaRepo = recetaRepo;
        this.facturaItemRepo = facturaItemRepo;
        this.historialRepo = historialRepo;
        this.insumoService = insumoService;
        this.loteMapper = loteMapper;
        this.em = em;
        this.tenantRepo = tenantRepo;
    }

    public List<HistorialLote> obtenerHistorial(Long loteId) {
        return historialRepo.findByLoteIdOrderByFechaDesc(loteId);
    }

    @Transactional(readOnly = true)
    public List<LoteCerveza> listarParaKanban() {
        return loteRepo.findParaKanban(java.time.LocalDate.now().minusDays(7));
    }

    @Value("${app.page-size:15}")
    private int pageSize;

    public List<LoteCerveza> listarTodos() {
        return loteRepo.findAllOrderByCreatedAtDesc();
    }

    // Fix 5+6: paginación + filtros
    public Page<LoteCerveza> listarPaginado(String estilo, String fase, int page) {
        return listarPaginado(estilo, fase, null, null, page);
    }

    public Page<LoteCerveza> listarPaginado(String estilo, String fase,
                                             java.time.LocalDate desde, java.time.LocalDate hasta,
                                             int page) {
        String estiloParam = (estilo != null && !estilo.isBlank()) ? estilo.trim() : "";
        String faseParam   = (fase   != null && !fase.isBlank())   ? fase.trim()   : "";
        return loteRepo.findByFiltros(estiloParam, faseParam, desde, hasta, PageRequest.of(page, pageSize));
    }

    public LoteCerveza buscarPorId(Long id) {
        return loteRepo.findByIdWithIngredientes(id)
                .orElseThrow(() -> new LoteNoEncontradoException(id));
    }

    @Transactional(readOnly = true)
    public java.util.Optional<LoteCerveza> buscarPorIdOpcional(Long id) {
        return loteRepo.findById(id);
    }

    @Caching(evict = {
        @CacheEvict(value = "dashboard-stats",      allEntries = true),
        @CacheEvict(value = "dashboard-litros-mes", allEntries = true),
        @CacheEvict(value = "dashboard-estilos",    allEntries = true)
    })
    public LoteGuardadoResult guardar(LoteFormDto dto) {
        verificarLimiteLotes();
        LoteCerveza lote = new LoteCerveza();
        mapearDto(lote, dto);
        lote.setCodigoLote(generarCodigo(dto.getEstilo()));
        agregarIngredientes(lote, dto);
        loteRepo.save(lote);
        historialRepo.save(HistorialLote.of(lote.getId(), lote.getCodigoLote(),
                "CREADO", currentUser(), lote.getEstilo()));
        List<String> advertencias = descontarInventario(lote.getIngredientes(), lote.getCodigoLote());
        if (advertencias.isEmpty()) {
            log.info("Lote creado: {} | estilo={} | ingredientes={}",
                    lote.getCodigoLote(), lote.getEstilo(), lote.getIngredientes().size());
        } else {
            log.warn("Lote creado con stock insuficiente: {} | sin stock={}",
                    lote.getCodigoLote(), advertencias);
        }
        return new LoteGuardadoResult(lote, advertencias);
    }

    @Caching(evict = {
        @CacheEvict(value = "dashboard-stats",      allEntries = true),
        @CacheEvict(value = "dashboard-litros-mes", allEntries = true),
        @CacheEvict(value = "dashboard-estilos",    allEntries = true)
    })
    public LoteGuardadoResult actualizar(Long id, LoteFormDto dto) {
        LoteCerveza lote = loteRepo.findByIdWithIngredientes(id)
                .orElseThrow(() -> new LoteNoEncontradoException(id));
        log.info("Actualizando lote: {}", lote.getCodigoLote());

        // Copia la lista vieja ANTES de limpiarla para poder comparar luego.
        List<Ingrediente> ingredientesAntes = new ArrayList<>(lote.getIngredientes());

        lote.getIngredientes().clear();
        mapearDto(lote, dto);
        agregarIngredientes(lote, dto);
        loteRepo.save(lote);
        historialRepo.save(HistorialLote.of(lote.getId(), lote.getCodigoLote(),
                "EDITADO", currentUser(), null));

        // Solo ajusta inventario si los ingredientes cambiaron efectivamente.
        // Si el usuario editó fechas, notas u otros campos sin tocar ingredientes,
        // no se generan movimientos innecesarios en movimientos_inventario.
        List<String> advertencias = List.of();
        if (ingredientesModificados(ingredientesAntes, lote.getIngredientes())) {
            restaurarInventario(ingredientesAntes, lote.getCodigoLote());
            advertencias = descontarInventario(lote.getIngredientes(), lote.getCodigoLote());
        }

        log.info("Lote actualizado: {}", lote.getCodigoLote());
        return new LoteGuardadoResult(lote, advertencias);
    }

    private boolean ingredientesModificados(List<Ingrediente> antes, List<Ingrediente> despues) {
        if (antes.size() != despues.size()) return true;
        List<String> keysBefore = antes.stream()
                .map(i -> i.getNombre() + "|" + i.getCantidad())
                .sorted()
                .toList();
        List<String> keysAfter = despues.stream()
                .map(i -> i.getNombre() + "|" + i.getCantidad())
                .sorted()
                .toList();
        return !keysBefore.equals(keysAfter);
    }

    @CacheEvict(value = "dashboard-stats", allEntries = true)
    public void moverFase(Long id, String fase) {
        LoteCerveza lote = loteRepo.findById(id)
                .orElseThrow(() -> new LoteNoEncontradoException(id));
        LocalDate hoy = LocalDate.now();
        switch (fase) {
            case "sinIniciar" -> {
                lote.setFermFechaInicial(null);
                lote.setFermFechaFinal(null);
                lote.setAcondFechaInicial(null);
                lote.setAcondFechaFinal(null);
                lote.setMadurFechaInicial(null);
                lote.setMadurFechaFinal(null);
                lote.setCarbFechaInicial(null);
                lote.setCarbFechaFinal(null);
            }
            case "fermentacion" -> {
                if (lote.getEquipoFermentador() == null)
                    throw new IllegalStateException(
                        "El lote no tiene fermentador asignado. Editá el lote y asigná un fermentador antes de iniciar la fermentación.");
                if (lote.getFermFechaInicial() == null) lote.setFermFechaInicial(hoy);
                lote.setFermFechaFinal(null);
                lote.setAcondFechaInicial(null);
                lote.setAcondFechaFinal(null);
                lote.setMadurFechaInicial(null);
                lote.setMadurFechaFinal(null);
                lote.setCarbFechaInicial(null);
                lote.setCarbFechaFinal(null);
            }
            case "acondicionamiento" -> {
                if (lote.getFermFechaInicial() == null) lote.setFermFechaInicial(hoy);
                if (lote.getFermFechaFinal() == null) lote.setFermFechaFinal(hoy);
                if (lote.getAcondFechaInicial() == null) lote.setAcondFechaInicial(hoy);
                lote.setAcondFechaFinal(null);
                lote.setMadurFechaInicial(null);
                lote.setMadurFechaFinal(null);
                lote.setCarbFechaInicial(null);
                lote.setCarbFechaFinal(null);
            }
            case "maduracion" -> {
                if (lote.getFermFechaInicial() == null) lote.setFermFechaInicial(hoy);
                if (lote.getFermFechaFinal() == null) lote.setFermFechaFinal(hoy);
                if (lote.getAcondFechaInicial() == null) lote.setAcondFechaInicial(hoy);
                if (lote.getAcondFechaFinal() == null) lote.setAcondFechaFinal(hoy);
                if (lote.getMadurFechaInicial() == null) lote.setMadurFechaInicial(hoy);
                lote.setMadurFechaFinal(null);
                lote.setCarbFechaInicial(null);
                lote.setCarbFechaFinal(null);
            }
            case "carbonatacion" -> {
                if (lote.getFermFechaInicial() == null) lote.setFermFechaInicial(hoy);
                if (lote.getFermFechaFinal() == null) lote.setFermFechaFinal(hoy);
                if (lote.getAcondFechaInicial() == null) lote.setAcondFechaInicial(hoy);
                if (lote.getAcondFechaFinal() == null) lote.setAcondFechaFinal(hoy);
                if (lote.getMadurFechaInicial() == null) lote.setMadurFechaInicial(hoy);
                if (lote.getMadurFechaFinal() == null) lote.setMadurFechaFinal(hoy);
                if (lote.getCarbFechaInicial() == null) lote.setCarbFechaInicial(hoy);
                lote.setCarbFechaFinal(null);
            }
            case "completados" -> {
                if (lote.getFermFechaInicial() == null) lote.setFermFechaInicial(hoy);
                if (lote.getFermFechaFinal() == null) lote.setFermFechaFinal(hoy);
                if (lote.getAcondFechaInicial() == null) lote.setAcondFechaInicial(hoy);
                if (lote.getAcondFechaFinal() == null) lote.setAcondFechaFinal(hoy);
                if (lote.getMadurFechaInicial() == null) lote.setMadurFechaInicial(hoy);
                if (lote.getMadurFechaFinal() == null) lote.setMadurFechaFinal(hoy);
                if (lote.getCarbFechaInicial() == null) lote.setCarbFechaInicial(hoy);
                if (lote.getCarbFechaFinal() == null) lote.setCarbFechaFinal(hoy);
            }
            default -> throw new IllegalArgumentException("Fase inválida: " + fase);
        }
        loteRepo.save(lote);
        historialRepo.save(HistorialLote.of(lote.getId(), lote.getCodigoLote(),
                "EDITADO", currentUser(), "Fase → " + fase));
        log.info("Lote {} movido a fase: {}", lote.getCodigoLote(), fase);
    }

    @Caching(evict = {
        @CacheEvict(value = "dashboard-stats",      allEntries = true),
        @CacheEvict(value = "dashboard-litros-mes", allEntries = true),
        @CacheEvict(value = "dashboard-estilos",    allEntries = true)
    })
    public void eliminar(Long id) {
        LoteCerveza lote = loteRepo.findByIdWithIngredientes(id)
                .orElseThrow(() -> new LoteNoEncontradoException(id));
        restaurarInventario(lote.getIngredientes(), lote.getCodigoLote());
        historialRepo.save(HistorialLote.of(lote.getId(), lote.getCodigoLote(),
                "ARCHIVADO", currentUser(), null));
        lote.setDeletedAt(java.time.LocalDateTime.now());
        loteRepo.save(lote);
        log.info("Lote archivado: {} | inventario restaurado", lote.getCodigoLote());
    }

    private void mapearDto(LoteCerveza lote, LoteFormDto dto) {
        lote.setEstilo(dto.getEstilo());
        lote.setFechaElaboracion(dto.getFechaElaboracion());
        lote.setAguaUtilizada(dto.getAguaUtilizada());
        lote.setPhAgua(dto.getPhAgua());
        lote.setLitrosFinales(dto.getLitrosFinales());
        lote.setClarificante(dto.getClarificante());
        lote.setDensidadInicial(dto.getDensidadInicial());
        lote.setDensidadFinal(dto.getDensidadFinal());
        lote.setDensidadFinalFecha(dto.getDensidadFinalFecha());
        lote.setOgBrix(dto.getOgBrix());
        lote.setFgBrix(dto.getFgBrix());
        lote.setFermFechaInicial(dto.getFermFechaInicial());
        lote.setFermFechaFinalIdeal(dto.getFermFechaFinalIdeal());
        lote.setFermTemperatura(dto.getFermTemperatura());
        lote.setFermFechaFinal(dto.getFermFechaFinal());
        lote.setAcondFechaInicial(dto.getAcondFechaInicial());
        lote.setAcondFechaFinalIdeal(dto.getAcondFechaFinalIdeal());
        lote.setAcondTemperatura(dto.getAcondTemperatura());
        lote.setAcondFechaFinal(dto.getAcondFechaFinal());
        lote.setMadurFechaInicial(dto.getMadurFechaInicial());
        lote.setMadurFechaFinalIdeal(dto.getMadurFechaFinalIdeal());
        lote.setMadurTemperatura(dto.getMadurTemperatura());
        lote.setMadurFechaFinal(dto.getMadurFechaFinal());
        lote.setCarbFechaInicial(dto.getCarbFechaInicial());
        lote.setCarbFechaFinalIdeal(dto.getCarbFechaFinalIdeal());
        lote.setCarbTemperatura(dto.getCarbTemperatura());
        lote.setCarbFechaFinal(dto.getCarbFechaFinal());
        lote.setCarbMetodo(dto.getCarbMetodo());
        lote.setCarbCo2Objetivo(dto.getCarbCo2Objetivo());
        lote.setCarbCo2Real(dto.getCarbCo2Real());
        lote.setCarbAzucarTipo(dto.getCarbAzucarTipo());
        lote.setCarbAzucarGramos(dto.getCarbAzucarGramos());
        lote.setCarbPresionPsi(dto.getCarbPresionPsi());
        lote.setCarbTiempoHoras(dto.getCarbTiempoHoras());
        lote.setCarbTecnica(dto.getCarbTecnica());
        lote.setCarbValidacion(dto.getCarbValidacion());
        lote.setCarbDestino(dto.getCarbDestino());
        lote.setObservaciones(dto.getObservaciones());
        lote.setNotasCata(dto.getNotasCata());

        lote.getItemsFactura().clear();
        if (lote.getId() != null) {
            em.flush(); // fuerza los DELETE de orphans antes de los INSERT nuevos (solo en actualizar)
        }
        List<Long> itemsIds = dto.getItemsIds();
        List<java.math.BigDecimal> itemsCantidades = dto.getItemsCantidades();
        if (itemsIds != null) {
            for (int i = 0; i < itemsIds.size(); i++) {
                Long itemId = itemsIds.get(i);
                java.math.BigDecimal cant = (itemsCantidades != null && i < itemsCantidades.size())
                        ? itemsCantidades.get(i) : null;
                if (itemId == null || cant == null || cant.compareTo(java.math.BigDecimal.ZERO) < 0) continue;
                final java.math.BigDecimal cantFinal = cant;
                facturaItemRepo.findById(itemId).ifPresent(item -> {
                    LoteItemFactura lif = new LoteItemFactura();
                    lif.setLote(lote);
                    lif.setItem(item);
                    lif.setCantidadAsignada(cantFinal);
                    lote.getItemsFactura().add(lif);
                });
            }
        }

        if (dto.getRecetaId() != null) {
            recetaRepo.findById(dto.getRecetaId()).ifPresent(lote::setReceta);
        } else {
            lote.setReceta(null);
        }

        if (dto.getEquipoFermentadorId() != null) {
            equipoRepo.findById(dto.getEquipoFermentadorId()).ifPresent(lote::setEquipoFermentador);
        } else {
            lote.setEquipoFermentador(null);
        }
    }

    private String generarCodigo(String estilo) {
        if (estilo == null || estilo.isBlank()) estilo = "LOT";
        String prefix = estilo.trim().toUpperCase().replaceAll("[^A-Z]", "");
        prefix = prefix.length() >= 3 ? prefix.substring(0, 3) : prefix;
        String tenantId = TenantContext.getCurrentTenant();
        // Native queries no disparan flush automático en Hibernate; forzarlo para ver
        // inserts previos de la misma transacción (ej: dos lotes del mismo estilo seguidos).
        em.flush();
        Integer max = loteRepo.findMaxConsecutivoPorPrefix(prefix, tenantId);
        int siguiente = (max == null ? 0 : max) + 1;
        return String.format("%s-%03d", prefix, siguiente);
    }

    private void agregarIngredientes(LoteCerveza lote, LoteFormDto dto) {
        agregarLista(lote, dto.getMaltas(), TipoIngrediente.MALTA);
        agregarLista(lote, dto.getLupulos(), TipoIngrediente.LUPULO);
        agregarLista(lote, dto.getLevaduras(), TipoIngrediente.LEVADURA);
        agregarLista(lote, dto.getClarificantes(), TipoIngrediente.CLARIFICANTE);
    }

    private void agregarLista(LoteCerveza lote, List<InsumoDto> lista, TipoIngrediente tipo) {
        if (lista == null) return;
        for (InsumoDto dto : lista) {
            if (dto.isEmpty()) continue;
            // DRY: delegado a UnidadUtils centralizado
            String cantNorm = UnidadUtils.normalizarParaAlmacenamiento(dto.getCantidad(), dto.getUnidad());
            Ingrediente ing = new Ingrediente(tipo, dto.getNombre(), cantNorm, lote);
            lote.getIngredientes().add(ing);
        }
    }

    /**
     * Descuenta todos los ingredientes del inventario dentro de la misma transacción
     * que el @Transactional de guardar/actualizar (propagation=REQUIRED por defecto).
     * Si cualquier operación falla, el rollback deshace TODOS los descuentos.
     *
     * @return nombres de ingredientes donde el stock era insuficiente (advertencias, no errores)
     */
    private List<String> descontarInventario(List<Ingrediente> ingredientes) {
        return descontarInventario(ingredientes, null);
    }

    private List<String> descontarInventario(List<Ingrediente> ingredientes, String codigoLote) {
        List<String> insuficientes = new ArrayList<>();
        for (Ingrediente ing : ingredientes) {
            String advertencia = insumoService.descontarIngrediente(ing.getNombre(), ing.getCantidad(), codigoLote);
            if (advertencia != null) {
                insuficientes.add(advertencia);
            }
        }
        return insuficientes;
    }

    private void restaurarInventario(List<Ingrediente> ingredientes) {
        restaurarInventario(ingredientes, null);
    }

    private void restaurarInventario(List<Ingrediente> ingredientes, String codigoLote) {
        for (Ingrediente ing : ingredientes) {
            insumoService.restaurarIngrediente(ing.getNombre(), ing.getCantidad(), codigoLote);
        }
    }

    public LoteFormDto toLoteFormDto(LoteCerveza lote) {
        return loteMapper.toLoteFormDto(lote);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> suggest(String q) {
        if (q == null || q.isBlank() || q.trim().length() < 2) return List.of();
        return loteRepo.search(q.trim(), PageRequest.of(0, 6)).stream()
            .map(l -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id",         l.getId());
                m.put("codigoLote", l.getCodigoLote());
                m.put("estilo",     l.getEstilo() != null ? l.getEstilo() : "");
                m.put("fase",       l.isCompletado() ? "Completado" : l.getFaseActual());
                m.put("completado", l.isCompletado());
                m.put("url",        "/ver/" + l.getId());
                m.put("carbDestino",  l.getCarbDestino()   != null ? l.getCarbDestino()   : "");
                m.put("litrosFinales", l.getLitrosFinales() != null ? l.getLitrosFinales() : "");
                return m;
            }).toList();
    }

    private String currentUser() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            return auth != null ? auth.getName() : "sistema";
        } catch (Exception e) {
            return "sistema";
        }
    }

    private void verificarLimiteLotes() {
        String tenantId = TenantContext.getCurrentTenant();
        tenantRepo.findById(tenantId).ifPresent(t -> {
            if (t.getMaxLotes() != null && loteRepo.count() >= t.getMaxLotes()) {
                throw new RuntimeException(
                    "Límite de lotes alcanzado para este plan (" + t.getMaxLotes() + " máx.). " +
                    "Contacta al administrador para ampliar tu plan.");
            }
        });
    }
}
