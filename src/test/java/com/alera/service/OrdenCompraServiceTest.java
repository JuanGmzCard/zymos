package com.alera.service;

import com.alera.config.TenantContext;
import com.alera.dto.OrdenCompraFormDto;
import com.alera.dto.OrdenCompraItemDto;
import com.alera.model.OrdenCompra;
import com.alera.model.Proveedor;
import com.alera.model.enums.EstadoOrdenCompra;
import com.alera.model.enums.TipoItemFactura;
import com.alera.repository.OrdenCompraRepository;
import com.alera.repository.ProveedorRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrdenCompraServiceTest {

    @Mock OrdenCompraRepository repo;
    @Mock ProveedorRepository   proveedorRepo;
    @Mock EntityManager         em;

    @InjectMocks OrdenCompraService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "pageSize", 15);
        ReflectionTestUtils.setField(service, "em", em);
        TenantContext.setCurrentTenant("default");
        lenient().when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ── listarPaginado ─────────────────────────────────────────────────────

    @Test
    void listarPaginado_delegaARepo() {
        Page<OrdenCompra> pagina = new PageImpl<>(List.of());
        when(repo.findAllFiltered(isNull(), any())).thenReturn(pagina);

        Page<OrdenCompra> result = service.listarPaginado(null, 0);

        assertThat(result).isNotNull();
        verify(repo).findAllFiltered(isNull(), any());
    }

    // ── buscarPorId ────────────────────────────────────────────────────────

    @Test
    void buscarPorId_encontrado_retornaOc() {
        OrdenCompra oc = ocConId(1L);
        when(repo.findByIdWithItems(1L)).thenReturn(Optional.of(oc));

        OrdenCompra result = service.buscarPorId(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void buscarPorId_noExiste_lanzaExcepcion() {
        when(repo.findByIdWithItems(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    // ── guardar ────────────────────────────────────────────────────────────

    @Test
    void guardar_creaOcConNumeroFormateado() {
        when(repo.findMaxNumeroOc("default")).thenReturn(0);

        OrdenCompraFormDto dto = dtoValido();
        OrdenCompra result = service.guardar(dto);

        assertThat(result.getNumeroOc()).isEqualTo("OC-001");
        assertThat(result.getFechaEmision()).isEqualTo(dto.getFechaEmision());
        verify(repo).save(any(OrdenCompra.class));
    }

    @Test
    void guardar_numeroCorrrelativoSiExistenOcsPrevias() {
        when(repo.findMaxNumeroOc("default")).thenReturn(5);

        OrdenCompra result = service.guardar(dtoValido());

        assertThat(result.getNumeroOc()).isEqualTo("OC-006");
    }

    @Test
    void guardar_vinculaProveedorSiProveedorIdPresente() {
        Proveedor prov = new Proveedor();
        prov.setNombre("Maltería Central");
        when(repo.findMaxNumeroOc("default")).thenReturn(0);
        when(proveedorRepo.findById(10L)).thenReturn(Optional.of(prov));

        OrdenCompraFormDto dto = dtoValido();
        dto.setProveedorId(10L);
        OrdenCompra result = service.guardar(dto);

        assertThat(result.getProveedor()).isEqualTo("Maltería Central");
        assertThat(result.getProveedorRef()).isEqualTo(prov);
    }

    @Test
    void guardar_mapearItemsDelDto() {
        when(repo.findMaxNumeroOc("default")).thenReturn(0);

        OrdenCompraFormDto dto = dtoValido();
        OrdenCompraItemDto itemDto = new OrdenCompraItemDto();
        itemDto.setTipoItem(TipoItemFactura.INSUMO);
        itemDto.setNombre("Malta Pilsen");
        itemDto.setCantidad(new BigDecimal("50"));
        itemDto.setUnidad("kg");
        itemDto.setPrecioUnitarioEstimado(new BigDecimal("5000"));
        itemDto.setPorcentajeIvaItem(new BigDecimal("19"));
        dto.getItems().add(itemDto);

        OrdenCompra result = service.guardar(dto);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getNombre()).isEqualTo("Malta Pilsen");
        assertThat(result.getItems().get(0).getCantidad()).isEqualByComparingTo("50");
    }

    @Test
    void guardar_ignoraItemsSinNombre() {
        when(repo.findMaxNumeroOc("default")).thenReturn(0);

        OrdenCompraFormDto dto = dtoValido();
        OrdenCompraItemDto vacio = new OrdenCompraItemDto();
        vacio.setNombre("   ");
        dto.getItems().add(vacio);

        OrdenCompra result = service.guardar(dto);

        assertThat(result.getItems()).isEmpty();
    }

    // ── actualizar ─────────────────────────────────────────────────────────

    @Test
    void actualizar_borradorActualiza() {
        OrdenCompra existente = ocConId(1L);
        existente.setEstado(EstadoOrdenCompra.BORRADOR);
        existente.setProveedor("Proveedor Viejo");
        when(repo.findByIdWithItems(1L)).thenReturn(Optional.of(existente));

        OrdenCompraFormDto dto = dtoValido();
        dto.setProveedor("Proveedor Nuevo");
        service.actualizar(1L, dto);

        ArgumentCaptor<OrdenCompra> captor = ArgumentCaptor.forClass(OrdenCompra.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getProveedor()).isEqualTo("Proveedor Nuevo");
    }

    @Test
    void actualizar_noEsBorrador_lanzaExcepcion() {
        OrdenCompra enviada = ocConId(1L);
        enviada.setEstado(EstadoOrdenCompra.ENVIADA);
        when(repo.findByIdWithItems(1L)).thenReturn(Optional.of(enviada));

        assertThatThrownBy(() -> service.actualizar(1L, dtoValido()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("BORRADOR");
    }

    // ── eliminar ───────────────────────────────────────────────────────────

    @Test
    void eliminar_borradorElimina() {
        OrdenCompra oc = ocConId(1L);
        oc.setEstado(EstadoOrdenCompra.BORRADOR);
        when(repo.findByIdWithItems(1L)).thenReturn(Optional.of(oc));

        service.eliminar(1L);

        verify(repo).deleteById(1L);
    }

    @Test
    void eliminar_canceladaPermiteEliminar() {
        OrdenCompra oc = ocConId(1L);
        oc.setEstado(EstadoOrdenCompra.CANCELADA);
        when(repo.findByIdWithItems(1L)).thenReturn(Optional.of(oc));

        service.eliminar(1L);

        verify(repo).deleteById(1L);
    }

    @Test
    void eliminar_estadoNoPermitido_lanzaExcepcion() {
        OrdenCompra oc = ocConId(1L);
        oc.setEstado(EstadoOrdenCompra.RECIBIDA);
        when(repo.findByIdWithItems(1L)).thenReturn(Optional.of(oc));

        assertThatThrownBy(() -> service.eliminar(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("BORRADOR");
    }

    // ── cambiarEstado ──────────────────────────────────────────────────────

    @Test
    void cambiarEstado_transicionValida_actualizaEstado() {
        OrdenCompra oc = ocConId(1L);
        oc.setEstado(EstadoOrdenCompra.BORRADOR);
        when(repo.findByIdWithItems(1L)).thenReturn(Optional.of(oc));

        OrdenCompra result = service.cambiarEstado(1L, EstadoOrdenCompra.ENVIADA);

        assertThat(result.getEstado()).isEqualTo(EstadoOrdenCompra.ENVIADA);
        verify(repo).save(oc);
    }

    @Test
    void cambiarEstado_transicionInvalida_lanzaExcepcion() {
        OrdenCompra oc = ocConId(1L);
        oc.setEstado(EstadoOrdenCompra.RECIBIDA);
        when(repo.findByIdWithItems(1L)).thenReturn(Optional.of(oc));

        assertThatThrownBy(() -> service.cambiarEstado(1L, EstadoOrdenCompra.BORRADOR))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("inválida");
    }

    @Test
    void cambiarEstado_borradorACancelada_valida() {
        OrdenCompra oc = ocConId(1L);
        oc.setEstado(EstadoOrdenCompra.BORRADOR);
        when(repo.findByIdWithItems(1L)).thenReturn(Optional.of(oc));

        OrdenCompra result = service.cambiarEstado(1L, EstadoOrdenCompra.CANCELADA);

        assertThat(result.getEstado()).isEqualTo(EstadoOrdenCompra.CANCELADA);
    }

    // ── convertirAFactura ──────────────────────────────────────────────────

    @Test
    void convertirAFactura_recibidasinFactura_llenaFacturaId() {
        OrdenCompra oc = ocConId(1L);
        oc.setEstado(EstadoOrdenCompra.RECIBIDA);
        oc.setFacturaId(null);
        when(repo.findByIdWithItems(1L)).thenReturn(Optional.of(oc));

        FacturaProveedorService facturaServiceMock = mock(FacturaProveedorService.class);
        when(facturaServiceMock.crearDesdeOrdenCompra(oc)).thenReturn(42L);

        Long facturaId = service.convertirAFactura(1L, facturaServiceMock);

        assertThat(facturaId).isEqualTo(42L);
        assertThat(oc.getFacturaId()).isEqualTo(42L);
        verify(repo).save(oc);
    }

    @Test
    void convertirAFactura_noConvertible_lanzaExcepcion() {
        OrdenCompra oc = ocConId(1L);
        oc.setEstado(EstadoOrdenCompra.ENVIADA);
        when(repo.findByIdWithItems(1L)).thenReturn(Optional.of(oc));

        FacturaProveedorService facturaServiceMock = mock(FacturaProveedorService.class);

        assertThatThrownBy(() -> service.convertirAFactura(1L, facturaServiceMock))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("convertida");
    }

    @Test
    void convertirAFactura_yaConvertida_lanzaExcepcion() {
        OrdenCompra oc = ocConId(1L);
        oc.setEstado(EstadoOrdenCompra.RECIBIDA);
        oc.setFacturaId(99L);
        when(repo.findByIdWithItems(1L)).thenReturn(Optional.of(oc));

        FacturaProveedorService facturaServiceMock = mock(FacturaProveedorService.class);

        assertThatThrownBy(() -> service.convertirAFactura(1L, facturaServiceMock))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("convertida");
    }

    // ── transicionesValidas ────────────────────────────────────────────────

    @Test
    void transicionesValidas_borrador_retornaEnviadaYCancelada() {
        List<EstadoOrdenCompra> result = service.transicionesValidas(EstadoOrdenCompra.BORRADOR);

        assertThat(result).containsExactlyInAnyOrder(EstadoOrdenCompra.ENVIADA, EstadoOrdenCompra.CANCELADA);
    }

    @Test
    void transicionesValidas_recibida_retornaVacio() {
        List<EstadoOrdenCompra> result = service.transicionesValidas(EstadoOrdenCompra.RECIBIDA);

        assertThat(result).isEmpty();
    }

    // ── suggest ────────────────────────────────────────────────────────────

    @Test
    void suggest_queryCorta_retornaVacio() {
        assertThat(service.suggest(null)).isEmpty();
        assertThat(service.suggest("a")).isEmpty();
        assertThat(service.suggest("  ")).isEmpty();
        verifyNoInteractions(repo);
    }

    @Test
    void suggest_retornaEstructura() {
        OrdenCompra oc = ocConId(1L);
        oc.setNumeroOc("OC-001");
        oc.setProveedor("Maltería Andina");
        oc.setEstado(EstadoOrdenCompra.BORRADOR);
        when(repo.search(eq("OC-001"), any())).thenReturn(new PageImpl<>(List.of(oc)));

        List<Map<String, Object>> result = service.suggest("OC-001");

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).containsKey("titulo");
        assertThat(result.get(0).get("titulo")).isEqualTo("OC-001");
        assertThat(result.get(0)).containsKey("url");
        assertThat(result.get(0).get("url").toString()).contains("/ordenes-compra/ver/1");
    }

    // ── countTotal / countByEstado ─────────────────────────────────────────

    @Test
    void countTotal_delegaARepo() {
        when(repo.count()).thenReturn(7L);

        assertThat(service.countTotal()).isEqualTo(7L);
    }

    @Test
    void countByEstado_delegaARepo() {
        when(repo.countByEstado(EstadoOrdenCompra.BORRADOR)).thenReturn(3L);

        assertThat(service.countByEstado(EstadoOrdenCompra.BORRADOR)).isEqualTo(3L);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private OrdenCompra ocConId(Long id) {
        OrdenCompra oc = new OrdenCompra();
        oc.setId(id);
        oc.setEstado(EstadoOrdenCompra.BORRADOR);
        oc.setFechaEmision(LocalDate.now());
        return oc;
    }

    private OrdenCompraFormDto dtoValido() {
        OrdenCompraFormDto dto = new OrdenCompraFormDto();
        dto.setFechaEmision(LocalDate.now());
        dto.setProveedor("Proveedor Test");
        return dto;
    }
}
