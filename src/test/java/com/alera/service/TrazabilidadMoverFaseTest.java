package com.alera.service;

import com.alera.exception.LoteNoEncontradoException;
import com.alera.mapper.LoteMapper;
import com.alera.model.Equipo;
import com.alera.model.HistorialLote;
import com.alera.model.LoteCerveza;
import com.alera.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrazabilidadService — moverFase")
class TrazabilidadMoverFaseTest {

    @Mock private LoteCervezaRepository   loteRepo;
    @Mock private EquipoRepository        equipoRepo;
    @Mock private RecetaRepository        recetaRepo;
    @Mock private FacturaItemRepository   facturaItemRepo;
    @Mock private HistorialLoteRepository historialRepo;
    @Mock private InsumoInventarioService insumoService;
    @Mock private LoteMapper              loteMapper;
    @Mock private EntityManager           em;
    @Mock private TenantRepository        tenantRepo;

    @InjectMocks
    private TrazabilidadService service;

    private LoteCerveza lote;

    @BeforeEach
    void setUp() {
        lote = new LoteCerveza();
        lote.setCodigoLote("IPA-001");
        lenient().when(loteRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(historialRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(tenantRepo.findById(any())).thenReturn(Optional.empty());
    }

    private void givenLote() {
        when(loteRepo.findById(1L)).thenReturn(Optional.of(lote));
    }

    // ── sinIniciar ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("sinIniciar — limpia todas las fechas de proceso")
    void sinIniciar_limpiaTodasLasFechas() {
        lote.setFermFechaInicial(LocalDate.now().minusDays(10));
        lote.setFermFechaFinal(LocalDate.now().minusDays(5));
        lote.setAcondFechaInicial(LocalDate.now().minusDays(4));
        lote.setAcondFechaFinal(LocalDate.now().minusDays(2));
        lote.setMadurFechaInicial(LocalDate.now().minusDays(1));
        lote.setCarbFechaInicial(LocalDate.now());
        lote.setCarbFechaFinal(LocalDate.now());
        givenLote();

        service.moverFase(1L, "sinIniciar");

        assertThat(lote.getFermFechaInicial()).isNull();
        assertThat(lote.getFermFechaFinal()).isNull();
        assertThat(lote.getAcondFechaInicial()).isNull();
        assertThat(lote.getAcondFechaFinal()).isNull();
        assertThat(lote.getMadurFechaInicial()).isNull();
        assertThat(lote.getMadurFechaFinal()).isNull();
        assertThat(lote.getCarbFechaInicial()).isNull();
        assertThat(lote.getCarbFechaFinal()).isNull();
    }

    // ── fermentacion ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("fermentacion — setea fermFechaInicial a hoy cuando es null y hay fermentador")
    void fermentacion_setaFechaInicial() {
        lote.setEquipoFermentador(new Equipo());
        givenLote();

        service.moverFase(1L, "fermentacion");

        assertThat(lote.getFermFechaInicial()).isEqualTo(LocalDate.now());
        assertThat(lote.getFermFechaFinal()).isNull();
        assertThat(lote.getCarbFechaInicial()).isNull();
        assertThat(lote.getCarbFechaFinal()).isNull();
    }

    @Test
    @DisplayName("fermentacion — no pisa fermFechaInicial si ya estaba seteada")
    void fermentacion_noPisaFechaExistente() {
        lote.setEquipoFermentador(new Equipo());
        LocalDate ayer = LocalDate.now().minusDays(1);
        lote.setFermFechaInicial(ayer);
        givenLote();

        service.moverFase(1L, "fermentacion");

        assertThat(lote.getFermFechaInicial()).isEqualTo(ayer);
    }

    @Test
    @DisplayName("fermentacion — limpia fases posteriores al mover hacia atrás")
    void fermentacion_limpiaFasesPosteriores() {
        lote.setEquipoFermentador(new Equipo());
        lote.setAcondFechaInicial(LocalDate.now().minusDays(2));
        lote.setMadurFechaInicial(LocalDate.now().minusDays(1));
        lote.setCarbFechaInicial(LocalDate.now());
        givenLote();

        service.moverFase(1L, "fermentacion");

        assertThat(lote.getAcondFechaInicial()).isNull();
        assertThat(lote.getMadurFechaInicial()).isNull();
        assertThat(lote.getCarbFechaInicial()).isNull();
    }

    @Test
    @DisplayName("fermentacion — lanza IllegalStateException cuando no hay fermentador asignado")
    void fermentacion_sinFermentador_lanzaExcepcion() {
        lote.setEquipoFermentador(null);
        givenLote();

        assertThatThrownBy(() -> service.moverFase(1L, "fermentacion"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("fermentador");
    }

    // ── acondicionamiento ─────────────────────────────────────────────────────

    @Test
    @DisplayName("acondicionamiento — setea ferm+acond iniciales, deja acondFechaFinal null")
    void acondicionamiento_setaFechasCorrectamente() {
        givenLote();

        service.moverFase(1L, "acondicionamiento");

        assertThat(lote.getFermFechaInicial()).isNotNull();
        assertThat(lote.getFermFechaFinal()).isNotNull();
        assertThat(lote.getAcondFechaInicial()).isNotNull();
        assertThat(lote.getAcondFechaFinal()).isNull();
        assertThat(lote.getMadurFechaInicial()).isNull();
        assertThat(lote.getCarbFechaInicial()).isNull();
    }

    @Test
    @DisplayName("acondicionamiento — no pisa fechas de ferm que ya existen")
    void acondicionamiento_noPisaFechasExistentes() {
        LocalDate haceSeis = LocalDate.now().minusDays(6);
        LocalDate haceTres = LocalDate.now().minusDays(3);
        lote.setFermFechaInicial(haceSeis);
        lote.setFermFechaFinal(haceTres);
        givenLote();

        service.moverFase(1L, "acondicionamiento");

        assertThat(lote.getFermFechaInicial()).isEqualTo(haceSeis);
        assertThat(lote.getFermFechaFinal()).isEqualTo(haceTres);
    }

    // ── maduracion ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("maduracion — setea ferm+acond+madur, deja madurFechaFinal y carbFechaInicial null")
    void maduracion_setaFechasCorrectamente() {
        givenLote();

        service.moverFase(1L, "maduracion");

        assertThat(lote.getFermFechaInicial()).isNotNull();
        assertThat(lote.getFermFechaFinal()).isNotNull();
        assertThat(lote.getAcondFechaInicial()).isNotNull();
        assertThat(lote.getAcondFechaFinal()).isNotNull();
        assertThat(lote.getMadurFechaInicial()).isNotNull();
        assertThat(lote.getMadurFechaFinal()).isNull();
        assertThat(lote.getCarbFechaInicial()).isNull();
    }

    // ── carbonatacion ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("carbonatacion — setea todas hasta carbFechaInicial, deja carbFechaFinal null")
    void carbonatacion_setaFechasCorrectamente() {
        givenLote();

        service.moverFase(1L, "carbonatacion");

        assertThat(lote.getFermFechaInicial()).isNotNull();
        assertThat(lote.getAcondFechaInicial()).isNotNull();
        assertThat(lote.getMadurFechaInicial()).isNotNull();
        assertThat(lote.getCarbFechaInicial()).isNotNull();
        assertThat(lote.getCarbFechaFinal()).isNull();
    }

    // ── completados ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("completados — setea todas las fechas que falten, incluida carbFechaFinal")
    void completados_setaTodasLasFechas() {
        givenLote();

        service.moverFase(1L, "completados");

        assertThat(lote.getFermFechaInicial()).isNotNull();
        assertThat(lote.getFermFechaFinal()).isNotNull();
        assertThat(lote.getAcondFechaInicial()).isNotNull();
        assertThat(lote.getAcondFechaFinal()).isNotNull();
        assertThat(lote.getMadurFechaInicial()).isNotNull();
        assertThat(lote.getMadurFechaFinal()).isNotNull();
        assertThat(lote.getCarbFechaInicial()).isNotNull();
        assertThat(lote.getCarbFechaFinal()).isNotNull();
    }

    @Test
    @DisplayName("completados — no pisa fechas que ya están seteadas")
    void completados_noPisaFechasExistentes() {
        LocalDate haceUnMes = LocalDate.now().minusMonths(1);
        lote.setFermFechaInicial(haceUnMes);
        givenLote();

        service.moverFase(1L, "completados");

        assertThat(lote.getFermFechaInicial()).isEqualTo(haceUnMes);
    }

    // ── fase inválida ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("fase inválida — lanza IllegalArgumentException con el nombre de la fase")
    void faseInvalida_lanzaExcepcion() {
        givenLote();

        assertThatThrownBy(() -> service.moverFase(1L, "noExiste"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("noExiste");
    }

    // ── lote no encontrado ────────────────────────────────────────────────────

    @Test
    @DisplayName("lote no encontrado — lanza LoteNoEncontradoException")
    void loteNoEncontrado_lanzaExcepcion() {
        when(loteRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.moverFase(99L, "sinIniciar"))
                .isInstanceOf(LoteNoEncontradoException.class);
    }

    // ── historial ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("moverFase — guarda historial con acción EDITADO y detalle de la fase")
    void moverFase_guardaHistorial() {
        lote.setEquipoFermentador(new Equipo());
        givenLote();

        service.moverFase(1L, "fermentacion");

        ArgumentCaptor<HistorialLote> captor = ArgumentCaptor.forClass(HistorialLote.class);
        verify(historialRepo).save(captor.capture());
        assertThat(captor.getValue().getAccion()).isEqualTo("EDITADO");
        assertThat(captor.getValue().getNotas()).contains("fermentacion");
    }

    @Test
    @DisplayName("moverFase — persiste el lote en todas las fases válidas")
    void moverFase_persisteLote() {
        givenLote();
        service.moverFase(1L, "sinIniciar");
        verify(loteRepo).save(lote);
    }
}
