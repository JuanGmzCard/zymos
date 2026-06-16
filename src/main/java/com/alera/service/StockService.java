package com.alera.service;

import com.alera.dto.StockLoteDto;
import com.alera.model.LoteCerveza;
import com.alera.model.StockAjuste;
import com.alera.repository.LoteCervezaRepository;
import com.alera.repository.StockAjusteRepository;
import com.alera.repository.VentaItemRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class StockService {

    private static final Pattern DESTINO_PATTERN =
            Pattern.compile("^(\\d+(?:[.,]\\d+)?)\\s*[×x]\\s*(.+)$",
                    Pattern.CASE_INSENSITIVE);

    private final LoteCervezaRepository   loteRepo;
    private final VentaItemRepository     ventaItemRepo;
    private final StockAjusteRepository   ajusteRepo;

    public StockService(LoteCervezaRepository loteRepo,
                        VentaItemRepository ventaItemRepo,
                        StockAjusteRepository ajusteRepo) {
        this.loteRepo     = loteRepo;
        this.ventaItemRepo = ventaItemRepo;
        this.ajusteRepo   = ajusteRepo;
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    public List<StockLoteDto> listarStock() {
        List<LoteCerveza> lotes = loteRepo.findAllCompletados(PageRequest.of(0, 500));
        List<StockLoteDto> result = new ArrayList<>(lotes.size());
        for (LoteCerveza lote : lotes) {
            result.add(construirDto(lote));
        }
        return result;
    }

    public BigDecimal getTotalDisponibleLitros() {
        return listarStock().stream()
                .filter(StockLoteDto::conStock)
                .map(StockLoteDto::disponible)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public long countLotesConStock() {
        return listarStock().stream().filter(StockLoteDto::conStock).count();
    }

    public long countLotesAgotados() {
        return listarStock().stream().filter(StockLoteDto::agotado).count();
    }

    public List<StockAjuste> listarAjustesPorLote(Long loteId) {
        return ajusteRepo.findByLoteIdOrderByFechaDesc(loteId);
    }

    // ── Mutaciones ────────────────────────────────────────────────────────────

    @Transactional
    public void registrarAjuste(Long loteId, BigDecimal cantidad, String unidad,
                                 String motivo, LocalDate fecha, String usuario) {
        LoteCerveza lote = loteRepo.findById(loteId)
                .orElseThrow(() -> new RuntimeException("Lote no encontrado: " + loteId));
        StockAjuste ajuste = new StockAjuste();
        ajuste.setLote(lote);
        ajuste.setCantidad(cantidad);
        ajuste.setUnidad(unidad != null && !unidad.isBlank() ? unidad : "L");
        ajuste.setMotivo(motivo);
        ajuste.setFecha(fecha);
        ajuste.setCreatedBy(usuario);
        ajusteRepo.save(ajuste);
    }

    @Transactional
    public void eliminarAjuste(Long ajusteId) {
        ajusteRepo.deleteById(ajusteId);
    }

    // ── Helpers privados ──────────────────────────────────────────────────────

    private StockLoteDto construirDto(LoteCerveza lote) {
        BigDecimal vendido  = ventaItemRepo.sumCantidadActivaByLote(lote.getId(), null);
        BigDecimal ajustado = ajusteRepo.sumCantidadByLoteId(lote.getId());

        // Determinar producido y unidad desde carbDestino o litrosFinales
        String     carbDestino = lote.getCarbDestino();
        BigDecimal producido;
        String     unidad;

        List<DestinoEntry> entradas = parseDestino(carbDestino);
        if (!entradas.isEmpty()) {
            producido = entradas.stream()
                    .map(DestinoEntry::cantidad)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            unidad = entradas.size() == 1 ? entradas.get(0).formato() : "uds";
        } else {
            producido = lote.getLitrosFinales();
            unidad = "L";
        }

        BigDecimal disponible = producido != null
                ? producido.subtract(vendido).add(ajustado)
                : null;

        return new StockLoteDto(
                lote.getId(),
                lote.getCodigoLote(),
                lote.getEstilo(),
                lote.getCarbFechaFinal(),
                producido,
                carbDestino,
                vendido,
                ajustado,
                disponible,
                unidad
        );
    }

    private record DestinoEntry(BigDecimal cantidad, String formato) {}

    private List<DestinoEntry> parseDestino(String carbDestino) {
        if (carbDestino == null || carbDestino.isBlank()) return List.of();
        var result = new ArrayList<DestinoEntry>();
        for (String parte : carbDestino.split("\\s*\\|\\s*")) {
            parte = parte.trim();
            if (parte.isEmpty()) continue;
            var m = DESTINO_PATTERN.matcher(parte);
            if (m.matches()) result.add(new DestinoEntry(
                    new BigDecimal(m.group(1).replace(',', '.')), m.group(2).trim()));
        }
        return result;
    }
}
