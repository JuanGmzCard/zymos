package com.alera.service;

import com.alera.dto.InsumoDto;
import com.alera.dto.RecetaFormDto;
import com.alera.model.Receta;
import com.alera.model.RecetaIngrediente;
import com.alera.model.enums.TipoIngrediente;
import com.alera.repository.RecetaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecetaService")
class RecetaServiceTest {

    @Mock RecetaRepository repo;

    @InjectMocks
    RecetaService service;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(service, "pageSize", 15);
    }

    // ── listarActivas ─────────────────────────────────────────────────

    @Test
    @DisplayName("listarActivas usa findAllByActivaTrueOrderByNombreAsc")
    void listarActivas_llamaMetodoCorrecto() {
        when(repo.findAllByActivaTrueOrderByNombreAsc()).thenReturn(List.of());

        service.listarActivas();

        verify(repo).findAllByActivaTrueOrderByNombreAsc();
        verify(repo, never()).findAllByOrderByActivaDescNombreAsc();
    }

    // ── listarTodas ───────────────────────────────────────────────────

    @Test
    @DisplayName("listarTodas usa findAllByOrderByActivaDescNombreAsc")
    void listarTodas_llamaMetodoCorrecto() {
        when(repo.findAllByOrderByActivaDescNombreAsc()).thenReturn(List.of());

        service.listarTodas();

        verify(repo).findAllByOrderByActivaDescNombreAsc();
    }

    // ── listarPaginado ────────────────────────────────────────────────

    @Test
    @DisplayName("listarPaginado sin filtro usa findAllByOrderByActivaDescNombreAsc paginado")
    void listarPaginado_sinFiltro_usaFindAll() {
        when(repo.findAllByOrderByActivaDescNombreAsc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.listarPaginado(null, 0);

        verify(repo).findAllByOrderByActivaDescNombreAsc(any(Pageable.class));
        verify(repo, never()).findByActivaOrderByNombreAsc(anyBoolean(), any());
    }

    @Test
    @DisplayName("listarPaginado con activa=true filtra solo recetas activas")
    void listarPaginado_activas_filtraCorrectamente() {
        when(repo.findByActivaOrderByNombreAsc(eq(true), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<Receta> resultado = service.listarPaginado(true, 0);

        assertThat(resultado).isNotNull();
        verify(repo).findByActivaOrderByNombreAsc(eq(true), any(Pageable.class));
        verify(repo, never()).findAllByOrderByActivaDescNombreAsc(any(Pageable.class));
    }

    @Test
    @DisplayName("listarPaginado con activa=false filtra solo recetas inactivas")
    void listarPaginado_inactivas_filtraCorrectamente() {
        when(repo.findByActivaOrderByNombreAsc(eq(false), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.listarPaginado(false, 0);

        verify(repo).findByActivaOrderByNombreAsc(eq(false), any(Pageable.class));
    }

    // ── buscarPorId ───────────────────────────────────────────────────

    @Test
    @DisplayName("buscarPorId retorna la receta cuando existe")
    void buscarPorId_encontrada_retornaReceta() {
        Receta receta = new Receta();
        receta.setNombre("IPA Clásica");
        when(repo.findById(1L)).thenReturn(Optional.of(receta));

        Receta resultado = service.buscarPorId(1L);

        assertThat(resultado.getNombre()).isEqualTo("IPA Clásica");
    }

    @Test
    @DisplayName("buscarPorId lanza RuntimeException si no existe")
    void buscarPorId_noEncontrada_lanzaExcepcion() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    // ── guardar ───────────────────────────────────────────────────────

    @Test
    @DisplayName("guardar mapea nombre, estilo y activa desde el DTO")
    void guardar_mapeaCamposBasicos() {
        RecetaFormDto dto = new RecetaFormDto();
        dto.setNombre("Stout Imperial");
        dto.setEstilo("Stout");
        dto.setActiva(true);

        when(repo.save(any(Receta.class))).thenAnswer(inv -> inv.getArgument(0));

        Receta resultado = service.guardar(dto);

        assertThat(resultado.getNombre()).isEqualTo("Stout Imperial");
        assertThat(resultado.getEstilo()).isEqualTo("Stout");
        assertThat(resultado.isActiva()).isTrue();
    }

    @Test
    @DisplayName("guardar agrega ingredientes normalizando unidades (kg → gr)")
    void guardar_normalizaIngredientesKgAGr() {
        RecetaFormDto dto = new RecetaFormDto();
        dto.setNombre("Porter");
        dto.setEstilo("Porter");
        dto.getMaltas().add(new InsumoDto("Pale Ale", "3", "kg"));

        when(repo.save(any(Receta.class))).thenAnswer(inv -> inv.getArgument(0));

        Receta resultado = service.guardar(dto);

        assertThat(resultado.getIngredientes()).hasSize(1);
        RecetaIngrediente ingrediente = resultado.getIngredientes().get(0);
        assertThat(ingrediente.getNombre()).isEqualTo("Pale Ale");
        assertThat(ingrediente.getCantidad()).contains("3000");
        assertThat(ingrediente.getTipo()).isEqualTo(TipoIngrediente.MALTA);
    }

    @Test
    @DisplayName("guardar ignora ingredientes con nombre vacío")
    void guardar_ignoraIngredientesVacios() {
        RecetaFormDto dto = new RecetaFormDto();
        dto.setNombre("Helles");
        dto.setEstilo("Lager");
        dto.getMaltas().add(new InsumoDto("", "", "gr")); // vacío — debe ignorarse

        when(repo.save(any(Receta.class))).thenAnswer(inv -> inv.getArgument(0));

        Receta resultado = service.guardar(dto);

        assertThat(resultado.getIngredientes()).isEmpty();
    }

    @Test
    @DisplayName("guardar agrega escalones de macerado en orden")
    void guardar_agregaEscalonesEnOrden() {
        RecetaFormDto dto = new RecetaFormDto();
        dto.setNombre("All Grain");
        dto.setEstilo("Pale Ale");

        RecetaFormDto.EscalonDto e1 = new RecetaFormDto.EscalonDto();
        e1.setNombre("Proteólisis");
        e1.setDuracionMinutos(20);
        e1.setTemperaturaC(new BigDecimal("50"));

        RecetaFormDto.EscalonDto e2 = new RecetaFormDto.EscalonDto();
        e2.setNombre("Sacarificación");
        e2.setDuracionMinutos(60);
        e2.setTemperaturaC(new BigDecimal("67"));

        dto.getEscalones().add(e1);
        dto.getEscalones().add(e2);

        when(repo.save(any(Receta.class))).thenAnswer(inv -> inv.getArgument(0));

        Receta resultado = service.guardar(dto);

        assertThat(resultado.getEscalones()).hasSize(2);
        assertThat(resultado.getEscalones().get(0).getNombre()).isEqualTo("Proteólisis");
        assertThat(resultado.getEscalones().get(0).getOrden()).isZero();
        assertThat(resultado.getEscalones().get(1).getNombre()).isEqualTo("Sacarificación");
        assertThat(resultado.getEscalones().get(1).getOrden()).isEqualTo(1);
    }

    // ── actualizar ────────────────────────────────────────────────────

    @Test
    @DisplayName("actualizar limpia ingredientes anteriores antes de asignar los nuevos")
    void actualizar_limpiaIngredientesAnteriores() {
        Receta receta = new Receta();
        receta.setNombre("IPA Original");
        receta.setEstilo("IPA");

        RecetaIngrediente ingredienteViejo = new RecetaIngrediente();
        ingredienteViejo.setNombre("Malta Antigua");
        receta.getIngredientes().add(ingredienteViejo);

        when(repo.findById(1L)).thenReturn(Optional.of(receta));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RecetaFormDto dto = new RecetaFormDto();
        dto.setNombre("IPA Renovada");
        dto.setEstilo("IPA");
        dto.getMaltas().add(new InsumoDto("Malta Nueva", "4000", "gr"));

        Receta resultado = service.actualizar(1L, dto);

        assertThat(resultado.getIngredientes()).hasSize(1);
        assertThat(resultado.getIngredientes().get(0).getNombre()).isEqualTo("Malta Nueva");
    }

    // ── eliminar ──────────────────────────────────────────────────────

    @Test
    @DisplayName("eliminar hace soft delete (setDeletedAt + save)")
    void eliminar_softDelete() {
        Receta receta = new Receta();
        receta.setNombre("IPA Clásica");
        when(repo.findById(5L)).thenReturn(Optional.of(receta));

        service.eliminar(5L);

        verify(repo, never()).deleteById(any());
        verify(repo).save(argThat(r -> r.getDeletedAt() != null));
    }

    // ── toFormDto ─────────────────────────────────────────────────────

    @Test
    @DisplayName("toFormDto mapea campos directos correctamente")
    void toFormDto_mapeaCamposDirectos() {
        Receta receta = new Receta();
        receta.setNombre("Weizen");
        receta.setEstilo("Trigo");
        receta.setActiva(false);
        receta.setOgObjetivo(1050);
        receta.setFgObjetivo(1010);
        receta.setTiempoHervorMinutos(75);

        RecetaFormDto dto = service.toFormDto(receta);

        assertThat(dto.getNombre()).isEqualTo("Weizen");
        assertThat(dto.getEstilo()).isEqualTo("Trigo");
        assertThat(dto.isActiva()).isFalse();
        assertThat(dto.getOgObjetivo()).isEqualTo(1050);
        assertThat(dto.getTiempoHervorMinutos()).isEqualTo(75);
    }

    @Test
    @DisplayName("toFormDto parsea cantidad normalizada '5000 gr' en cantidad='5000' y unidad='gr'")
    void toFormDto_parseaCantidadNormalizada() {
        Receta receta = new Receta();
        receta.setNombre("Session IPA");
        receta.setEstilo("IPA");

        RecetaIngrediente malta = new RecetaIngrediente();
        malta.setTipo(TipoIngrediente.MALTA);
        malta.setNombre("Pilsner");
        malta.setCantidad("5000 gr");
        receta.getIngredientes().add(malta);

        RecetaFormDto dto = service.toFormDto(receta);

        assertThat(dto.getMaltas()).hasSize(1);
        assertThat(dto.getMaltas().get(0).getNombre()).isEqualTo("Pilsner");
        assertThat(dto.getMaltas().get(0).getCantidad()).isEqualTo("5000");
        assertThat(dto.getMaltas().get(0).getUnidad()).isEqualTo("gr");
    }

    @Test
    @DisplayName("toFormDto agrega fila vacía cuando la receta no tiene maltas")
    void toFormDto_agregaFilaVaciaParaMaltasVacias() {
        Receta receta = new Receta();
        receta.setNombre("Sin maltas");
        receta.setEstilo("Experimental");

        RecetaFormDto dto = service.toFormDto(receta);

        assertThat(dto.getMaltas()).hasSize(1);
        assertThat(dto.getMaltas().get(0).getNombre()).isNull();
    }
}