package com.alera;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que todas las migraciones Flyway se apliquen correctamente
 * sobre una base de datos PostgreSQL real (Testcontainers).
 */
class FlywayMigrationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private Flyway flyway;

    @Test
    void todasLasMigracionesAplicadas() {
        var info = flyway.info();
        assertThat(info.pending())
                .as("No debe haber migraciones pendientes")
                .isEmpty();
    }

    @Test
    void hayMigracionesAplicadas() {
        var info = flyway.info();
        assertThat(info.applied())
                .as("Debe haber al menos una migración aplicada")
                .isNotEmpty();
    }

    @Test
    void ningunaMigracionFallida() {
        var info = flyway.info();
        var fallidas = java.util.Arrays.stream(info.all())
                .filter(m -> m.getState().isFailed())
                .toList();
        assertThat(fallidas)
                .as("No debe haber migraciones fallidas")
                .isEmpty();
    }

    @Test
    void conteoMigracionesIncluyeV1HastaV19() {
        // Verifica que se hayan aplicado al menos V1-V19 (+ baseline opcional)
        var applied = flyway.info().applied();
        assertThat(applied.length)
                .as("Deben estar aplicadas al menos 19 migraciones (V1-V19)")
                .isGreaterThanOrEqualTo(19);
    }
}
