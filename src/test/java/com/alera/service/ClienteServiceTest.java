package com.alera.service;

import com.alera.dto.ClienteFormDto;
import com.alera.model.Cliente;
import com.alera.model.enums.ListaPrecio;
import com.alera.repository.ClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClienteService")
class ClienteServiceTest {

    @Mock ClienteRepository repo;

    @InjectMocks
    ClienteService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "pageSize", 15);
    }

    private ClienteFormDto buildDto(String nombre, String nit) {
        ClienteFormDto dto = new ClienteFormDto();
        dto.setNombre(nombre);
        dto.setNit(nit);
        dto.setActivo(true);
        return dto;
    }

    private Cliente clienteConId(Long id, String nit) {
        Cliente c = new Cliente();
        c.setId(id);
        c.setNit(nit);
        c.setNombre("Nombre Test");
        return c;
    }

    // ── listarActivos ─────────────────────────────────────────────────

    @Test
    @DisplayName("listarActivos delega al repositorio con filtro activo=true")
    void listarActivos_delegaAlRepositorio() {
        Cliente c = new Cliente();
        c.setNombre("Bar La Espuma");
        when(repo.findAllByActivoTrueOrderByNombreAsc()).thenReturn(List.of(c));

        List<Cliente> resultado = service.listarActivos();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombre()).isEqualTo("Bar La Espuma");
        verify(repo).findAllByActivoTrueOrderByNombreAsc();
    }

    // ── listarTodos ───────────────────────────────────────────────────

    @Test
    @DisplayName("listarTodos retorna activos e inactivos ordenados por nombre")
    void listarTodos_retornaTodos() {
        when(repo.findAllByOrderByNombreAsc()).thenReturn(List.of(new Cliente(), new Cliente()));

        assertThat(service.listarTodos()).hasSize(2);
        verify(repo).findAllByOrderByNombreAsc();
    }

    // ── buscarPorId ───────────────────────────────────────────────────

    @Test
    @DisplayName("buscarPorId retorna el cliente cuando existe")
    void buscarPorId_existe_retornaCliente() {
        Cliente c = new Cliente();
        c.setNombre("Distribuidora Norte");
        when(repo.findById(1L)).thenReturn(Optional.of(c));

        assertThat(service.buscarPorId(1L)).isPresent();
    }

    @Test
    @DisplayName("buscarPorId retorna vacío cuando no existe")
    void buscarPorId_noExiste_retornaVacio() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThat(service.buscarPorId(99L)).isEmpty();
    }

    // ── guardar ───────────────────────────────────────────────────────

    @Test
    @DisplayName("guardar persiste cliente con campos básicos")
    void guardar_persisteCliente() {
        ClienteFormDto dto = buildDto("Cervecería Test", null);
        dto.setEmail("test@mail.com");
        dto.setCiudad("Bogotá");
        when(repo.save(any())).thenReturn(new Cliente());

        service.guardar(dto);

        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Cervecería Test");
        assertThat(captor.getValue().getEmail()).isEqualTo("test@mail.com");
        assertThat(captor.getValue().isActivo()).isTrue();
    }

    @Test
    @DisplayName("guardar con NIT null no verifica unicidad")
    void guardar_nitNull_noVerificaUnicidad() {
        when(repo.save(any())).thenReturn(new Cliente());

        service.guardar(buildDto("Cliente A", null));

        verify(repo, never()).findByNit(any());
    }

    @Test
    @DisplayName("guardar con NIT en blanco no verifica unicidad")
    void guardar_nitBlanco_noVerificaUnicidad() {
        when(repo.save(any())).thenReturn(new Cliente());

        service.guardar(buildDto("Cliente B", "   "));

        verify(repo, never()).findByNit(any());
    }

    @Test
    @DisplayName("guardar con NIT no duplicado persiste sin error")
    void guardar_nitNoDuplicado_persiste() {
        when(repo.findByNit("900-1")).thenReturn(Optional.empty());
        when(repo.save(any())).thenReturn(new Cliente());

        service.guardar(buildDto("Cliente C", "900-1"));

        verify(repo).save(any());
    }

    @Test
    @DisplayName("guardar con NIT duplicado en otro cliente lanza excepción")
    void guardar_nitDuplicado_lanzaExcepcion() {
        // existente.getId()=99L != dto.getId()=null → lanza excepción
        when(repo.findByNit("900-1")).thenReturn(Optional.of(clienteConId(99L, "900-1")));

        assertThatThrownBy(() -> service.guardar(buildDto("Nuevo Cliente", "900-1")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("900-1");
    }

    @Test
    @DisplayName("guardar normaliza campos en blanco a null via blank()")
    void guardar_blancosNormalizadosANull() {
        ClienteFormDto dto = buildDto("Cliente D", null);
        dto.setRazonSocial("  ");
        dto.setTelefono("   ");
        dto.setNotas("");
        when(repo.save(any())).thenReturn(new Cliente());

        service.guardar(dto);

        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getRazonSocial()).isNull();
        assertThat(captor.getValue().getTelefono()).isNull();
        assertThat(captor.getValue().getNotas()).isNull();
    }

    // ── actualizar ────────────────────────────────────────────────────

    @Test
    @DisplayName("actualizar lanza excepción si el cliente no existe")
    void actualizar_noExiste_lanzaExcepcion() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(99L, buildDto("x", null)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("actualizar modifica campos del cliente existente")
    void actualizar_modificaCampos() {
        Cliente existente = new Cliente();
        existente.setNombre("Viejo Nombre");
        when(repo.findById(1L)).thenReturn(Optional.of(existente));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.actualizar(1L, buildDto("Nuevo Nombre", null));

        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Nuevo Nombre");
    }

    @Test
    @DisplayName("actualizar con NIT del mismo cliente no lanza excepción")
    void actualizar_nitDelMismoCliente_noLanzaExcepcion() {
        // existente.getId()=1L == id parameter=1L → no exception
        Cliente existente = clienteConId(1L, "800-1");
        when(repo.findById(1L)).thenReturn(Optional.of(existente));
        when(repo.findByNit("800-1")).thenReturn(Optional.of(existente));
        when(repo.save(any())).thenReturn(existente);

        service.actualizar(1L, buildDto("Nombre", "800-1"));

        verify(repo).save(any());
    }

    @Test
    @DisplayName("actualizar con NIT duplicado en otro cliente lanza excepción")
    void actualizar_nitDuplicadoEnOtroCliente_lanzaExcepcion() {
        // otroCliente.getId()=55L != id parameter=1L → lanza excepción
        when(repo.findById(1L)).thenReturn(Optional.of(clienteConId(1L, null)));
        when(repo.findByNit("700-2")).thenReturn(Optional.of(clienteConId(55L, "700-2")));

        assertThatThrownBy(() -> service.actualizar(1L, buildDto("Nombre", "700-2")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("700-2");
    }

    // ── toggleActivo ──────────────────────────────────────────────────

    @Test
    @DisplayName("toggleActivo invierte activo=true a false")
    void toggleActivo_activoAInactivo() {
        Cliente c = new Cliente();
        c.setActivo(true);
        when(repo.findById(1L)).thenReturn(Optional.of(c));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.toggleActivo(1L);

        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().isActivo()).isFalse();
    }

    @Test
    @DisplayName("toggleActivo invierte activo=false a true")
    void toggleActivo_inactivoAActivo() {
        Cliente c = new Cliente();
        c.setActivo(false);
        when(repo.findById(2L)).thenReturn(Optional.of(c));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.toggleActivo(2L);

        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().isActivo()).isTrue();
    }

    @Test
    @DisplayName("toggleActivo no hace nada si el cliente no existe")
    void toggleActivo_noExiste_noOp() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        service.toggleActivo(99L);

        verify(repo, never()).save(any());
    }

    // ── suggest ───────────────────────────────────────────────────────

    @Test
    @DisplayName("suggest retorna vacío si la query es null")
    void suggest_queryNull_retornaVacio() {
        assertThat(service.suggest(null)).isEmpty();
        verifyNoInteractions(repo);
    }

    @Test
    @DisplayName("suggest retorna vacío si la query está vacía")
    void suggest_queryVacia_retornaVacio() {
        assertThat(service.suggest("")).isEmpty();
        verifyNoInteractions(repo);
    }

    @Test
    @DisplayName("suggest delega al repositorio con el query recortado")
    void suggest_delegaASearchActivos() {
        when(repo.searchActivos(eq("mosto"), any(Pageable.class))).thenReturn(List.of());

        service.suggest("mosto");

        verify(repo).searchActivos(eq("mosto"), any(Pageable.class));
    }

    @Test
    @DisplayName("suggest retorna máximo 6 resultados aunque el repo devuelva más")
    void suggest_limiteSeisResultados() {
        List<Cliente> ocho = IntStream.rangeClosed(1, 8).mapToObj(i -> {
            Cliente c = new Cliente();
            c.setNombre("Cliente " + i);
            return c;
        }).toList();
        when(repo.searchActivos(anyString(), any(Pageable.class))).thenReturn(ocho);

        assertThat(service.suggest("cliente")).hasSize(6);
    }

    @Test
    @DisplayName("suggest incluye id, nombre, nit, listaPrecio y ciudad en cada resultado")
    void suggest_estructuraDelMapa() {
        Cliente c = new Cliente();
        c.setNombre("Cervecería Sur");
        c.setNit("900-5");
        c.setListaPrecio(ListaPrecio.BAR);
        c.setCiudad("Medellín");
        when(repo.searchActivos(anyString(), any(Pageable.class))).thenReturn(List.of(c));

        Map<String, Object> item = service.suggest("sur").get(0);

        assertThat(item).containsKeys("id", "nombre", "nit", "listaPrecio", "ciudad");
        assertThat(item.get("nombre")).isEqualTo("Cervecería Sur");
        assertThat(item.get("nit")).isEqualTo("900-5");
        assertThat(item.get("listaPrecio")).isEqualTo(ListaPrecio.BAR.getDisplayName());
        assertThat(item.get("ciudad")).isEqualTo("Medellín");
    }

    @Test
    @DisplayName("suggest usa string vacío para nit null")
    void suggest_nitNull_usaStringVacio() {
        Cliente c = new Cliente();
        c.setNombre("Sin NIT");
        when(repo.searchActivos(anyString(), any(Pageable.class))).thenReturn(List.of(c));

        assertThat(service.suggest("sin").get(0).get("nit")).isEqualTo("");
    }

    @Test
    @DisplayName("suggest usa string vacío para listaPrecio null")
    void suggest_listaPrecioNull_usaStringVacio() {
        Cliente c = new Cliente();
        c.setNombre("Sin Lista");
        when(repo.searchActivos(anyString(), any(Pageable.class))).thenReturn(List.of(c));

        assertThat(service.suggest("sin").get(0).get("listaPrecio")).isEqualTo("");
    }

    @Test
    @DisplayName("suggest usa string vacío para ciudad null")
    void suggest_ciudadNull_usaStringVacio() {
        Cliente c = new Cliente();
        c.setNombre("Sin Ciudad");
        when(repo.searchActivos(anyString(), any(Pageable.class))).thenReturn(List.of(c));

        assertThat(service.suggest("sin").get(0).get("ciudad")).isEqualTo("");
    }
}
