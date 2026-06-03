package com.alera.service;

import com.alera.dto.InsumoDto;
import com.alera.dto.LoteFormDto;
import com.alera.dto.LoteGuardadoResult;
import com.alera.mapper.LoteMapper;
import com.alera.model.LoteCerveza;
import com.alera.repository.EquipoRepository;
import com.alera.repository.FacturaItemRepository;
import com.alera.repository.HistorialLoteRepository;
import com.alera.repository.LoteCervezaRepository;
import com.alera.repository.RecetaRepository;
import com.alera.repository.TenantRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrazabilidadService")
class TrazabilidadServiceTest {

    @Mock private LoteCervezaRepository loteRepo;
    @Mock private EquipoRepository equipoRepo;
    @Mock private RecetaRepository recetaRepo;
    @Mock private FacturaItemRepository facturaItemRepo;
    @Mock private HistorialLoteRepository historialRepo;
    @Mock private InsumoInventarioService insumoService;
    @Mock private LoteMapper loteMapper;
    @Mock private EntityManager em;
    @Mock private TenantRepository tenantRepo;

    @InjectMocks
    private TrazabilidadService service;

    private LoteFormDto loteFormBasico;

    @BeforeEach
    void setUp() {
        loteFormBasico = new LoteFormDto();
        loteFormBasico.setEstilo("IPA");

        InsumoDto malta = new InsumoDto("Swaen Ale", "5000", "gr");
        loteFormBasico.setMaltas(List.of(malta));
        loteFormBasico.setLupulos(List.of());
        loteFormBasico.setLevaduras(List.of());
        loteFormBasico.setClarificantes(List.of());

        // por defecto, sin límite de plan
        lenient().when(tenantRepo.findById(any())).thenReturn(Optional.empty());
    }

    // ── Generación de código ────────────────────────────────────────

    @Test
    @DisplayName("guardar genera código IPA-001 para primer lote IPA")
    void guardar_generaCodigoCorrectoParaPrimerLote() {
        when(loteRepo.findMaxConsecutivoPorPrefix(eq("IPA"), any())).thenReturn(null);
        when(loteRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(insumoService.descontarIngrediente(any(), any(), any())).thenReturn(null);

        LoteGuardadoResult resultado = service.guardar(loteFormBasico);

        assertThat(resultado.getLote().getCodigoLote()).isEqualTo("IPA-001");
    }

    @Test
    @DisplayName("guardar genera código IPA-003 cuando ya existen IPA-001 e IPA-002")
    void guardar_incrementaConsecutivo() {
        when(loteRepo.findMaxConsecutivoPorPrefix(eq("IPA"), any())).thenReturn(2);
        when(loteRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(insumoService.descontarIngrediente(any(), any(), any())).thenReturn(null);

        LoteGuardadoResult resultado = service.guardar(loteFormBasico);

        assertThat(resultado.getLote().getCodigoLote()).isEqualTo("IPA-003");
    }

    // ── Advertencias de stock ───────────────────────────────────────

    @Test
    @DisplayName("guardar retorna resultado sin advertencias cuando hay stock suficiente")
    void guardar_sinAdvertenciasConStockSuficiente() {
        when(loteRepo.findMaxConsecutivoPorPrefix(any(), any())).thenReturn(null);
        when(loteRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(insumoService.descontarIngrediente(any(), any(), any())).thenReturn(null);

        LoteGuardadoResult resultado = service.guardar(loteFormBasico);

        assertThat(resultado.tieneAdvertencias()).isFalse();
        assertThat(resultado.getAdvertencias()).isEmpty();
    }

    @Test
    @DisplayName("guardar retorna advertencias cuando el stock es insuficiente")
    void guardar_conAdvertenciasDeStockInsuficiente() {
        when(loteRepo.findMaxConsecutivoPorPrefix(any(), any())).thenReturn(null);
        when(loteRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        // Simula que "Swaen Ale" tiene stock insuficiente
        when(insumoService.descontarIngrediente(eq("Swaen Ale"), any(), any())).thenReturn("Swaen Ale");

        LoteGuardadoResult resultado = service.guardar(loteFormBasico);

        assertThat(resultado.tieneAdvertencias()).isTrue();
        assertThat(resultado.getAdvertencias()).containsExactly("Swaen Ale");
        assertThat(resultado.getMensajeAdvertencias()).contains("Swaen Ale");
    }

    // ── Normalización de unidades ───────────────────────────────────

    @Test
    @DisplayName("guardar normaliza kg a gr (×1000)")
    void guardar_normalizaKgAGr() {
        loteFormBasico.getMaltas().get(0).setCantidad("2");
        loteFormBasico.getMaltas().get(0).setUnidad("kg");

        when(loteRepo.findMaxConsecutivoPorPrefix(any(), any())).thenReturn(null);
        when(loteRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(insumoService.descontarIngrediente(any(), any(), any())).thenReturn(null);

        service.guardar(loteFormBasico);

        // Verifica que se descuenta con 2000 gr, no 2 kg
        verify(insumoService).descontarIngrediente(eq("Swaen Ale"), contains("2000"), any());
    }

    @Test
    @DisplayName("guardar normaliza galones a mL (×3785.41)")
    void guardar_normalizaGalonesAMl() {
        InsumoDto levadura = new InsumoDto("Levadura Líquida", "1", "gal");
        loteFormBasico.setLevaduras(List.of(levadura));

        when(loteRepo.findMaxConsecutivoPorPrefix(any(), any())).thenReturn(null);
        when(loteRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(insumoService.descontarIngrediente(any(), any(), any())).thenReturn(null);

        service.guardar(loteFormBasico);

        verify(insumoService).descontarIngrediente(eq("Levadura Líquida"), contains("3785"), any());
    }

    // ── Eliminar ────────────────────────────────────────────────────

    @Test
    @DisplayName("eliminar restaura inventario de todos los ingredientes")
    void eliminar_restauraInventario() {
        LoteCerveza lote = new LoteCerveza();
        lote.setCodigoLote("IPA-001");

        InsumoDto malta = new InsumoDto("Swaen Ale", "5000", "gr");
        LoteFormDto dto = new LoteFormDto();
        dto.setEstilo("IPA");
        dto.setMaltas(List.of(malta));
        dto.setLupulos(List.of());
        dto.setLevaduras(List.of());
        dto.setClarificantes(List.of());

        when(loteRepo.findMaxConsecutivoPorPrefix(any(), any())).thenReturn(null);
        when(loteRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(insumoService.descontarIngrediente(any(), any(), any())).thenReturn(null);
        LoteGuardadoResult res = service.guardar(dto);

        when(loteRepo.findByIdWithIngredientes(1L)).thenReturn(Optional.of(res.getLote()));

        service.eliminar(1L);

        verify(insumoService).restaurarIngrediente(eq("Swaen Ale"), any(), any());
        // soft delete: save con deletedAt seteado, no loteRepo.delete()
        verify(loteRepo, never()).delete(any());
        verify(loteRepo, atLeast(2)).save(argThat(l ->
                l == res.getLote() && l.getDeletedAt() != null));
    }

    // ── Límites del plan ────────────────────────────────────────────

    @Test
    @DisplayName("guardar lanza excepción cuando se alcanza el límite de lotes del plan")
    void guardar_limiteLotesAlcanzado_lanzaExcepcion() {
        com.alera.model.Tenant tenant = new com.alera.model.Tenant();
        tenant.setSubdomain("test");
        tenant.setMaxLotes(5);
        when(tenantRepo.findById(any())).thenReturn(Optional.of(tenant));
        when(loteRepo.count()).thenReturn(5L);

        assertThatThrownBy(() -> service.guardar(loteFormBasico))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Límite de lotes")
                .hasMessageContaining("5");
    }

    @Test
    @DisplayName("guardar no lanza excepción cuando hay espacio en el plan")
    void guardar_conEspacioEnPlan_noLanzaExcepcion() {
        com.alera.model.Tenant tenant = new com.alera.model.Tenant();
        tenant.setSubdomain("test");
        tenant.setMaxLotes(10);
        when(tenantRepo.findById(any())).thenReturn(Optional.of(tenant));
        when(loteRepo.count()).thenReturn(5L);
        when(loteRepo.findMaxConsecutivoPorPrefix(any(), any())).thenReturn(null);
        when(loteRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(insumoService.descontarIngrediente(any(), any(), any())).thenReturn(null);

        assertThatCode(() -> service.guardar(loteFormBasico)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("eliminar lanza RuntimeException si el lote no existe")
    void eliminar_loteNoExisteLanzaExcepcion() {
        when(loteRepo.findByIdWithIngredientes(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }
}
