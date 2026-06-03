package com.alera.service;

import com.alera.model.Proveedor;
import com.alera.repository.ProveedorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProveedorService")
class ProveedorServiceTest {

    @Mock ProveedorRepository repo;

    @InjectMocks
    ProveedorService service;

    private Proveedor proveedor(String nombre, String nit, boolean activo) {
        Proveedor p = new Proveedor();
        p.setNombre(nombre);
        p.setNit(nit);
        p.setActivo(activo);
        return p;
    }

    // ── listarActivos ─────────────────────────────────────────────────

    @Test
    @DisplayName("listarActivos delega al repositorio con filtro activo=true")
    void listarActivos_delegaAlRepositorio() {
        when(repo.findAllByActivoTrueOrderByNombreAsc())
                .thenReturn(List.of(proveedor("MaltaCo", "123", true)));

        List<Proveedor> resultado = service.listarActivos();

        assertThat(resultado).hasSize(1);
        verify(repo).findAllByActivoTrueOrderByNombreAsc();
    }

    // ── listarTodos ───────────────────────────────────────────────────

    @Test
    @DisplayName("listarTodos retorna todos ordenados por nombre")
    void listarTodos_retornaTodos() {
        when(repo.findAllByOrderByNombreAsc()).thenReturn(List.of(
                proveedor("Alfa", "111", true),
                proveedor("Beta", "222", false)));

        assertThat(service.listarTodos()).hasSize(2);
        verify(repo).findAllByOrderByNombreAsc();
    }

    // ── buscarPorId ───────────────────────────────────────────────────

    @Test
    @DisplayName("buscarPorId retorna el proveedor cuando existe")
    void buscarPorId_existe_retornaProveedor() {
        Proveedor p = proveedor("MaltaCo", "900", true);
        when(repo.findById(1L)).thenReturn(Optional.of(p));

        assertThat(service.buscarPorId(1L)).isPresent();
    }

    @Test
    @DisplayName("buscarPorId retorna vacío cuando no existe")
    void buscarPorId_noExiste_retornaVacio() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThat(service.buscarPorId(99L)).isEmpty();
    }

    // ── suggest ───────────────────────────────────────────────────────

    @Test
    @DisplayName("suggest retorna vacío si la query es nula")
    void suggest_queryNula_retornaVacio() {
        assertThat(service.suggest(null)).isEmpty();
        verifyNoInteractions(repo);
    }

    @Test
    @DisplayName("suggest retorna vacío si la query tiene menos de 2 caracteres")
    void suggest_queryCorta_retornaVacio() {
        assertThat(service.suggest("M")).isEmpty();
        verifyNoInteractions(repo);
    }

    @Test
    @DisplayName("suggest delega a repo.search con la query recortada")
    void suggest_delegaASearch() {
        when(repo.search(eq("malt"), any(Pageable.class)))
                .thenReturn(List.of(proveedor("MaltaCo", "900", true)));

        service.suggest("malt");

        verify(repo).search(eq("malt"), any(Pageable.class));
    }

    @Test
    @DisplayName("suggest retorna los resultados del repositorio")
    void suggest_retornaResultadosDelRepo() {
        when(repo.search(eq("malt"), any(Pageable.class))).thenReturn(List.of(
                proveedor("MaltaCo", "900", true),
                proveedor("Malta Sur", "700", false)));

        List<Map<String, Object>> resultado = service.suggest("malt");

        assertThat(resultado).hasSize(2);
        assertThat(resultado).extracting("nombre")
                .containsExactlyInAnyOrder("MaltaCo", "Malta Sur");
    }

    @Test
    @DisplayName("suggest retorna máximo 6 resultados (limitado en el Pageable)")
    void suggest_limiteSeisResultados() {
        List<Proveedor> seis = IntStream.rangeClosed(1, 6)
                .mapToObj(i -> proveedor("Proveedor " + i, "NIT" + i, true))
                .toList();
        when(repo.search(anyString(), any(Pageable.class))).thenReturn(seis);

        assertThat(service.suggest("Proveedor")).hasSize(6);
    }

    @Test
    @DisplayName("suggest incluye nombre, nit, activo y url en cada resultado")
    void suggest_estructuraDelMapa() {
        when(repo.search(eq("malt"), any(Pageable.class))).thenReturn(List.of(
                proveedor("MaltaCo", "900123", true)));

        Map<String, Object> item = service.suggest("malt").get(0);

        assertThat(item).containsKeys("nombre", "nit", "activo", "url");
        assertThat(item.get("nombre")).isEqualTo("MaltaCo");
        assertThat(item.get("nit")).isEqualTo("900123");
        assertThat(item.get("activo")).isEqualTo(true);
        assertThat(item.get("url").toString()).contains("/proveedores/editar/");
    }

    @Test
    @DisplayName("suggest usa string vacío como nit cuando el proveedor no tiene NIT")
    void suggest_nitNull_usaStringVacio() {
        when(repo.search(anyString(), any(Pageable.class))).thenReturn(List.of(
                proveedor("SinNit", null, true)));

        Map<String, Object> item = service.suggest("SinNit").get(0);

        assertThat(item.get("nit")).isEqualTo("");
    }

    // ── guardar ───────────────────────────────────────────────────────

    @Test
    @DisplayName("guardar delega al repositorio y retorna el proveedor guardado")
    void guardar_persisteYRetorna() {
        Proveedor p = proveedor("MaltaCo", "900", true);
        when(repo.save(p)).thenReturn(p);

        Proveedor resultado = service.guardar(p);

        assertThat(resultado.getNombre()).isEqualTo("MaltaCo");
        verify(repo).save(p);
    }

    // ── eliminar ──────────────────────────────────────────────────────

    @Test
    @DisplayName("eliminar delega a deleteById")
    void eliminar_delegaADeleteById() {
        service.eliminar(7L);

        verify(repo).deleteById(7L);
    }

    // ── contarFacturas / totalFacturas ────────────────────────────────

    @Test
    @DisplayName("contarFacturas delega al repositorio")
    void contarFacturas_delegaAlRepositorio() {
        when(repo.countFacturas(1L)).thenReturn(5L);

        assertThat(service.contarFacturas(1L)).isEqualTo(5L);
        verify(repo).countFacturas(1L);
    }

    @Test
    @DisplayName("totalFacturas delega al repositorio")
    void totalFacturas_delegaAlRepositorio() {
        when(repo.sumFacturas(1L)).thenReturn(new BigDecimal("1500000"));

        assertThat(service.totalFacturas(1L)).isEqualByComparingTo("1500000");
        verify(repo).sumFacturas(1L);
    }
}
