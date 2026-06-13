package com.alera.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MigracionTemplateServiceTest {

    private final MigracionTemplateService service = new MigracionTemplateService();

    private static final byte[] PK_MAGIC = {0x50, 0x4B, 0x03, 0x04}; // ZIP/XLSX magic bytes

    private void assertEsXlsx(byte[] bytes) {
        assertThat(bytes).isNotNull();
        assertThat(bytes.length).isGreaterThan(100);
        // Los archivos XLSX son ZIP: comienzan con PK\x03\x04
        assertThat(bytes[0]).isEqualTo(PK_MAGIC[0]);
        assertThat(bytes[1]).isEqualTo(PK_MAGIC[1]);
        assertThat(bytes[2]).isEqualTo(PK_MAGIC[2]);
        assertThat(bytes[3]).isEqualTo(PK_MAGIC[3]);
    }

    @Test
    void plantillaAlmacen_generaXlsxValido() throws Exception {
        byte[] bytes = service.plantillaAlmacen();
        assertEsXlsx(bytes);
    }

    @Test
    void plantillaEquipos_generaXlsxValido() throws Exception {
        byte[] bytes = service.plantillaEquipos();
        assertEsXlsx(bytes);
    }

    @Test
    void plantillaComercial_generaXlsxValido() throws Exception {
        byte[] bytes = service.plantillaComercial();
        assertEsXlsx(bytes);
    }

    @Test
    void plantillaProduccion_generaXlsxValido() throws Exception {
        byte[] bytes = service.plantillaProduccion();
        assertEsXlsx(bytes);
    }

    @Test
    void plantillaClientes_generaXlsxValido() throws Exception {
        byte[] bytes = service.plantillaClientes();
        assertEsXlsx(bytes);
    }

    @Test
    void plantillaVentas_generaXlsxValido() throws Exception {
        byte[] bytes = service.plantillaVentas();
        assertEsXlsx(bytes);
    }

    @Test
    void todasLasPlantillas_generanBytesDistintos() throws Exception {
        byte[] almacen   = service.plantillaAlmacen();
        byte[] equipos   = service.plantillaEquipos();
        byte[] comercial = service.plantillaComercial();

        // Los archivos deben ser distintos (contenido diferente)
        assertThat(almacen).isNotEqualTo(equipos);
        assertThat(almacen).isNotEqualTo(comercial);
        assertThat(equipos).isNotEqualTo(comercial);
    }
}
