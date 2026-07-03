package com.alera.service;

import com.alera.dto.StockLoteDto;
import com.alera.model.LoteCerveza;
import com.alera.model.StockAjuste;
import com.alera.repository.LoteCervezaRepository;
import com.alera.repository.StockAjusteRepository;
import com.alera.repository.VentaItemRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockService")
class StockServiceTest {

    @Mock private LoteCervezaRepository loteRepo;
    @Mock private VentaItemRepository   ventaItemRepo;
    @Mock private StockAjusteRepository ajusteRepo;

    @InjectMocks
    private StockService service;

    // ── helpers ───────────────────────────────────────────────────────────────

    private LoteCerveza lote(Long id, BigDecimal litros, String carbDestino) {
        LoteCerveza l = new LoteCerveza();
        ReflectionTestUtils.setField(l, "id", id);
        l.setCodigoLote("LOT-00" + id);
        l.setEstilo("IPA");
        l.setLitrosFinales(litros);
        l.setCarbDestino(carbDestino);
        return l;
    }

    private void mockVentasAjuste(Long id, BigDecimal desp, BigDecimal res, BigDecimal ajus) {
        when(ventaItemRepo.sumCantidadDespachadadaByLote(id)).thenReturn(desp);
        when(ventaItemRepo.sumCantidadReservadaByLote(id)).thenReturn(res);
        when(ajusteRepo.sumCantidadByLoteId(id)).thenReturn(ajus);
    }

    // ── construirDto / listarStock ────────────────────────────────────────────

    @Test
    @DisplayName("listarStock — sin carbDestino usa litrosFinales como producido con unidad L")
    void listarStock_sinCarbDestino_usaLitrosFinales() {
        when(loteRepo.findAllCompletados(any(Pageable.class)))
                .thenReturn(List.of(lote(1L, new BigDecimal("100"), null)));
        mockVentasAjuste(1L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        List<StockLoteDto> stock = service.listarStock();

        assertThat(stock).hasSize(1);
        assertThat(stock.get(0).producido()).isEqualByComparingTo("100");
        assertThat(stock.get(0).unidad()).isEqualTo("L");
    }

    @Test
    @DisplayName("listarStock — disponible = litrosFinales - despachado - reservado + ajustado")
    void listarStock_calculaDisponible() {
        when(loteRepo.findAllCompletados(any(Pageable.class)))
                .thenReturn(List.of(lote(1L, new BigDecimal("100"), null)));
        mockVentasAjuste(1L, new BigDecimal("30"), new BigDecimal("20"), new BigDecimal("5"));

        StockLoteDto dto = service.listarStock().get(0);

        // vendido = 30 + 20 = 50; disponible = 100 - 50 + 5 = 55
        assertThat(dto.vendido()).isEqualByComparingTo("50");
        assertThat(dto.despachado()).isEqualByComparingTo("30");
        assertThat(dto.reservado()).isEqualByComparingTo("20");
        assertThat(dto.disponible()).isEqualByComparingTo("55");
    }

    @Test
    @DisplayName("listarStock — carbDestino simple '10 × Botellas 330ml' → producido=10, unidad=Botellas 330ml")
    void listarStock_carbDestinoSimple_parseaCorrecto() {
        when(loteRepo.findAllCompletados(any(Pageable.class)))
                .thenReturn(List.of(lote(1L, new BigDecimal("100"), "10 × Botellas 330ml")));
        mockVentasAjuste(1L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        StockLoteDto dto = service.listarStock().get(0);

        assertThat(dto.producido()).isEqualByComparingTo("10");
        assertThat(dto.unidad()).isEqualTo("Botellas 330ml");
    }

    @Test
    @DisplayName("listarStock — carbDestino con 'x' ASCII funciona igual que ×")
    void listarStock_carbDestinoConX_parseaCorrecto() {
        when(loteRepo.findAllCompletados(any(Pageable.class)))
                .thenReturn(List.of(lote(1L, null, "5x Kegs 20L")));
        mockVentasAjuste(1L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        StockLoteDto dto = service.listarStock().get(0);

        assertThat(dto.producido()).isEqualByComparingTo("5");
        assertThat(dto.unidad()).isEqualTo("Kegs 20L");
    }

    @Test
    @DisplayName("listarStock — carbDestino múltiple suma producido y usa 'uds'")
    void listarStock_carbDestinoMultiple_sumaYUsaUds() {
        when(loteRepo.findAllCompletados(any(Pageable.class)))
                .thenReturn(List.of(lote(1L, null, "5 × Kegs 20L | 10 × Botellas 500ml")));
        mockVentasAjuste(1L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        StockLoteDto dto = service.listarStock().get(0);

        assertThat(dto.producido()).isEqualByComparingTo("15");
        assertThat(dto.unidad()).isEqualTo("uds");
    }

    @Test
    @DisplayName("listarStock — carbDestino con coma decimal '2,5 × Barriles' → producido=2.5")
    void listarStock_carbDestinoConComa_parsea() {
        when(loteRepo.findAllCompletados(any(Pageable.class)))
                .thenReturn(List.of(lote(1L, null, "2,5 × Barriles 30L")));
        mockVentasAjuste(1L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        StockLoteDto dto = service.listarStock().get(0);

        assertThat(dto.producido()).isEqualByComparingTo("2.5");
    }

    @Test
    @DisplayName("listarStock — carbDestino vacío o en blanco cae a litrosFinales")
    void listarStock_carbDestinoBlanco_usaLitros() {
        when(loteRepo.findAllCompletados(any(Pageable.class)))
                .thenReturn(List.of(lote(1L, new BigDecimal("80"), "   ")));
        mockVentasAjuste(1L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        assertThat(service.listarStock().get(0).unidad()).isEqualTo("L");
    }

    @Test
    @DisplayName("listarStock — conStock() true cuando disponible > 0")
    void listarStock_conStock_cuandoDisponiblePositivo() {
        when(loteRepo.findAllCompletados(any(Pageable.class)))
                .thenReturn(List.of(lote(1L, new BigDecimal("100"), null)));
        mockVentasAjuste(1L, new BigDecimal("30"), BigDecimal.ZERO, BigDecimal.ZERO);

        assertThat(service.listarStock().get(0).conStock()).isTrue();
    }

    @Test
    @DisplayName("listarStock — agotado() true cuando disponible <= 0")
    void listarStock_agotado_cuandoDisponibleCero() {
        when(loteRepo.findAllCompletados(any(Pageable.class)))
                .thenReturn(List.of(lote(1L, new BigDecimal("50"), null)));
        mockVentasAjuste(1L, new BigDecimal("40"), new BigDecimal("10"), BigDecimal.ZERO);

        assertThat(service.listarStock().get(0).agotado()).isTrue();
    }

    // ── getTotalDisponibleLitros ──────────────────────────────────────────────

    @Test
    @DisplayName("getTotalDisponibleLitros — suma solo lotes con stock positivo")
    void getTotalDisponibleLitros_ignoraAgotados() {
        LoteCerveza l1 = lote(1L, new BigDecimal("100"), null); // 70 disponibles
        LoteCerveza l2 = lote(2L, new BigDecimal("50"),  null); // 0 disponibles → agotado
        when(loteRepo.findAllCompletados(any(Pageable.class))).thenReturn(List.of(l1, l2));
        mockVentasAjuste(1L, new BigDecimal("30"), BigDecimal.ZERO, BigDecimal.ZERO);
        mockVentasAjuste(2L, new BigDecimal("50"), BigDecimal.ZERO, BigDecimal.ZERO);

        assertThat(service.getTotalDisponibleLitros()).isEqualByComparingTo("70");
    }

    @Test
    @DisplayName("getTotalDisponibleLitros — retorna ZERO cuando no hay lotes")
    void getTotalDisponibleLitros_sinLotes_retornaZero() {
        when(loteRepo.findAllCompletados(any(Pageable.class))).thenReturn(List.of());

        assertThat(service.getTotalDisponibleLitros()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── countLotesConStock / countLotesAgotados ───────────────────────────────

    @Test
    @DisplayName("countLotesConStock y countLotesAgotados — cuentan correctamente entre sí")
    void count_conStockYAgotado() {
        LoteCerveza l1 = lote(1L, new BigDecimal("100"), null); // 70 disp.
        LoteCerveza l2 = lote(2L, new BigDecimal("50"),  null); // 0 disp.
        LoteCerveza l3 = lote(3L, new BigDecimal("30"),  null); // 20 disp.
        when(loteRepo.findAllCompletados(any(Pageable.class))).thenReturn(List.of(l1, l2, l3));
        mockVentasAjuste(1L, new BigDecimal("30"), BigDecimal.ZERO, BigDecimal.ZERO);
        mockVentasAjuste(2L, new BigDecimal("50"), BigDecimal.ZERO, BigDecimal.ZERO);
        mockVentasAjuste(3L, new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO);

        assertThat(service.countLotesConStock()).isEqualTo(2L);
        assertThat(service.countLotesAgotados()).isEqualTo(1L);
    }

    // ── registrarAjuste ───────────────────────────────────────────────────────

    @Test
    @DisplayName("registrarAjuste — persiste ajuste con todos los campos")
    void registrarAjuste_persisteCorrectamente() {
        LoteCerveza l = lote(1L, new BigDecimal("100"), null);
        when(loteRepo.findById(1L)).thenReturn(Optional.of(l));
        when(ajusteRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.registrarAjuste(1L, new BigDecimal("5"), "L", "Merma por filtrado",
                LocalDate.of(2025, 6, 1), "admin");

        ArgumentCaptor<StockAjuste> captor = ArgumentCaptor.forClass(StockAjuste.class);
        verify(ajusteRepo).save(captor.capture());
        StockAjuste ajuste = captor.getValue();
        assertThat(ajuste.getLote()).isEqualTo(l);
        assertThat(ajuste.getCantidad()).isEqualByComparingTo("5");
        assertThat(ajuste.getUnidad()).isEqualTo("L");
        assertThat(ajuste.getMotivo()).isEqualTo("Merma por filtrado");
        assertThat(ajuste.getCreatedBy()).isEqualTo("admin");
        assertThat(ajuste.getFecha()).isEqualTo(LocalDate.of(2025, 6, 1));
    }

    @Test
    @DisplayName("registrarAjuste — unidad null se reemplaza por 'L'")
    void registrarAjuste_unidadNull_defaultL() {
        when(loteRepo.findById(1L)).thenReturn(Optional.of(lote(1L, BigDecimal.TEN, null)));
        when(ajusteRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.registrarAjuste(1L, BigDecimal.ONE, null, "", LocalDate.now(), "admin");

        ArgumentCaptor<StockAjuste> captor = ArgumentCaptor.forClass(StockAjuste.class);
        verify(ajusteRepo).save(captor.capture());
        assertThat(captor.getValue().getUnidad()).isEqualTo("L");
    }

    @Test
    @DisplayName("registrarAjuste — unidad en blanco se reemplaza por 'L'")
    void registrarAjuste_unidadBlanco_defaultL() {
        when(loteRepo.findById(1L)).thenReturn(Optional.of(lote(1L, BigDecimal.TEN, null)));
        when(ajusteRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.registrarAjuste(1L, BigDecimal.ONE, "  ", "", LocalDate.now(), "admin");

        ArgumentCaptor<StockAjuste> captor = ArgumentCaptor.forClass(StockAjuste.class);
        verify(ajusteRepo).save(captor.capture());
        assertThat(captor.getValue().getUnidad()).isEqualTo("L");
    }

    @Test
    @DisplayName("registrarAjuste — lanza RuntimeException si el lote no existe")
    void registrarAjuste_loteNoExiste_lanzaExcepcion() {
        when(loteRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.registrarAjuste(99L, BigDecimal.ONE, "L", "", LocalDate.now(), "admin"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    // ── eliminarAjuste ────────────────────────────────────────────────────────

    @Test
    @DisplayName("eliminarAjuste — delega deleteById al repositorio")
    void eliminarAjuste_delegaAlRepo() {
        service.eliminarAjuste(42L);
        verify(ajusteRepo).deleteById(42L);
    }
}
