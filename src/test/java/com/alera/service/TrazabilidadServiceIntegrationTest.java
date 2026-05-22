package com.alera.service;

import com.alera.AbstractIntegrationTest;
import com.alera.dto.InsumoDto;
import com.alera.dto.LoteFormDto;
import com.alera.repository.LoteCervezaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de integración para el flujo completo de trazabilidad.
 * Verifica guardar, actualizar y eliminar contra una BD real.
 */
@Transactional
class TrazabilidadServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TrazabilidadService service;

    @Autowired
    private LoteCervezaRepository loteRepo;

    @Test
    void guardarLoteGeneraCodigoCorrecto() {
        var dto = LoteFormDto.empty();
        dto.setEstilo("India Pale Ale");

        var resultado = service.guardar(dto);

        assertThat(resultado.getLote()).isNotNull();
        assertThat(resultado.getLote().getCodigoLote()).startsWith("IND-");
        assertThat(resultado.getLote().getId()).isPositive();
    }

    @Test
    void guardarDosLotesMismoEstiloGeneraCodigosConsecutivos() {
        var dto1 = LoteFormDto.empty(); dto1.setEstilo("Stout");
        var dto2 = LoteFormDto.empty(); dto2.setEstilo("Stout");

        var r1 = service.guardar(dto1);
        var r2 = service.guardar(dto2);

        assertThat(r1.getLote().getCodigoLote()).isEqualTo("STO-001");
        assertThat(r2.getLote().getCodigoLote()).isEqualTo("STO-002");
    }

    @Test
    void guardarLoteConIngredientesLosPersiste() {
        var dto = LoteFormDto.empty();
        dto.setEstilo("Porter");
        var malta = new InsumoDto();
        malta.setNombre("Malta Pilsen");
        malta.setCantidad("5000");
        malta.setUnidad("gr");
        dto.getMaltas().clear();
        dto.getMaltas().add(malta);

        var resultado = service.guardar(dto);
        var loteId = resultado.getLote().getId();

        var loteGuardado = service.buscarPorId(loteId);
        assertThat(loteGuardado.getMaltas()).hasSize(1);
        assertThat(loteGuardado.getMaltas().get(0).getNombre()).isEqualTo("Malta Pilsen");
    }

    @Test
    void eliminarLoteLoBorraDeBaseDeDatos() {
        var dto = LoteFormDto.empty();
        dto.setEstilo("Lager");
        var loteId = service.guardar(dto).getLote().getId();

        assertThat(loteRepo.existsById(loteId)).isTrue();

        service.eliminar(loteId);

        assertThat(loteRepo.existsById(loteId)).isFalse();
    }

    @Test
    void historialRegistraCreacionYEliminacion() {
        var dto = LoteFormDto.empty();
        dto.setEstilo("Wheat");
        var lote = service.guardar(dto).getLote();

        var historialCreado = service.obtenerHistorial(lote.getId());
        assertThat(historialCreado).hasSize(1);
        assertThat(historialCreado.get(0).getAccion()).isEqualTo("CREADO");

        service.eliminar(lote.getId());

        // El historial persiste incluso después de eliminar el lote (sin FK)
        var historialTotal = service.obtenerHistorial(lote.getId());
        assertThat(historialTotal).hasSize(2);
        assertThat(historialTotal.get(0).getAccion()).isEqualTo("ELIMINADO");
    }
}
