package com.alera.service;

import com.alera.model.LogAcceso;
import com.alera.repository.LogAccesoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogAccesoServiceTest {

    @Mock
    LogAccesoRepository repo;

    @InjectMocks
    LogAccesoService service;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(service, "pageSize", 25);
    }

    // ── registrar ────────────────────────────────────────────────────

    @Test
    void registrar_persisteElLogEnRepositorio() {
        service.registrar("admin", "LOGIN_OK", "127.0.0.1", "/", "Mozilla/5.0", null);

        ArgumentCaptor<LogAcceso> captor = ArgumentCaptor.forClass(LogAcceso.class);
        verify(repo).save(captor.capture());

        LogAcceso guardado = captor.getValue();
        assertEquals("admin",     guardado.getUsuario());
        assertEquals("LOGIN_OK",  guardado.getTipo());
        assertEquals("127.0.0.1", guardado.getIp());
        assertEquals("/",          guardado.getUrl());
        assertNotNull(guardado.getFecha());
    }

    @Test
    void registrar_loginFallido_persisteYLogea() {
        service.registrar("hacker", "LOGIN_FALLIDO", "10.0.0.1", "/login", "curl/7.0", null);

        verify(repo).save(argThat(log ->
            "LOGIN_FALLIDO".equals(log.getTipo()) &&
            "hacker".equals(log.getUsuario()) &&
            "10.0.0.1".equals(log.getIp())
        ));
    }

    @Test
    void registrar_accesoDenegado_persisteConUrl() {
        service.registrar("user", "ACCESO_DENEGADO", "192.168.1.1", "/admin", "Browser", null);

        verify(repo).save(argThat(log ->
            "ACCESO_DENEGADO".equals(log.getTipo()) &&
            "/admin".equals(log.getUrl())
        ));
    }

    @Test
    void registrar_userAgentLargo_seRecorta() {
        String agentLargo = "X".repeat(350);

        service.registrar("admin", "LOGIN_OK", "1.2.3.4", "/", agentLargo, null);

        verify(repo).save(argThat(log ->
            log.getUserAgent() != null && log.getUserAgent().length() <= 300
        ));
    }

    @Test
    void registrar_conDetalles_losIncluye() {
        service.registrar("admin", "LOGIN_OK", "1.2.3.4", "/", "Agent", "detalle extra");

        verify(repo).save(argThat(log -> "detalle extra".equals(log.getDetalles())));
    }

    // ── listarPaginado ────────────────────────────────────────────────

    @Test
    void listarPaginado_sinTipo_usaFindAll() {
        Page<LogAcceso> pagina = new PageImpl<>(List.of());
        when(repo.findAllByOrderByFechaDesc(any())).thenReturn(pagina);

        service.listarPaginado(null, 0);

        verify(repo).findAllByOrderByFechaDesc(PageRequest.of(0, 25));
        verify(repo, never()).findByTipoOrderByFechaDesc(any(), any());
    }

    @Test
    void listarPaginado_conTipoVacio_usaFindAll() {
        when(repo.findAllByOrderByFechaDesc(any())).thenReturn(new PageImpl<>(List.of()));

        service.listarPaginado("", 0);

        verify(repo).findAllByOrderByFechaDesc(any());
        verify(repo, never()).findByTipoOrderByFechaDesc(any(), any());
    }

    @Test
    void listarPaginado_conTipo_usaFindByTipo() {
        Page<LogAcceso> pagina = new PageImpl<>(List.of());
        when(repo.findByTipoOrderByFechaDesc(eq("LOGIN_FALLIDO"), any())).thenReturn(pagina);

        Page<LogAcceso> result = service.listarPaginado("LOGIN_FALLIDO", 0);

        verify(repo).findByTipoOrderByFechaDesc("LOGIN_FALLIDO", PageRequest.of(0, 25));
        verify(repo, never()).findAllByOrderByFechaDesc(any());
        assertNotNull(result);
    }

    @Test
    void listarPaginado_segundaPagina_usaOffsetCorrecto() {
        when(repo.findAllByOrderByFechaDesc(any())).thenReturn(new PageImpl<>(List.of()));

        service.listarPaginado(null, 2);

        verify(repo).findAllByOrderByFechaDesc(PageRequest.of(2, 25));
    }

    // ── fallidosUltimaHora ────────────────────────────────────────────

    @Test
    void fallidosUltimaHora_retornaConteoDelRepositorio() {
        when(repo.countFallidosDesde(any())).thenReturn(7L);

        long resultado = service.fallidosUltimaHora();

        assertEquals(7L, resultado);
    }

    @Test
    void fallidosUltimaHora_consultaElUltimoHora() {
        when(repo.countFallidosDesde(any())).thenReturn(0L);

        service.fallidosUltimaHora();

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repo).countFallidosDesde(captor.capture());

        LocalDateTime desde = captor.getValue();
        LocalDateTime esperado = LocalDateTime.now().minusHours(1);
        // Tolerancia de 5 segundos para evitar flakiness
        assertTrue(desde.isAfter(esperado.minusSeconds(5)),  "La fecha debe ser aprox. hace 1 hora");
        assertTrue(desde.isBefore(esperado.plusSeconds(5)), "La fecha debe ser aprox. hace 1 hora");
    }

    @Test
    void fallidosUltimaHora_cero_cuandoNoHayFallos() {
        when(repo.countFallidosDesde(any())).thenReturn(0L);

        assertEquals(0L, service.fallidosUltimaHora());
    }
}