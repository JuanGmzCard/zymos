package com.alera;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base para todos los tests de integración.
 * Levanta PostgreSQL real via Testcontainers + ejecuta todas las migraciones Flyway.
 * Spring Boot 3.4 con @ServiceConnection configura automáticamente el datasource.
 *
 * El contenedor se arranca UNA VEZ con el inicializador estático y vive todo el JVM.
 * Esto evita que @Testcontainers detenga y reinicie el contenedor entre clases de test,
 * lo que causaría que el contexto de Spring Boot (cacheado) intentara conectar a un
 * puerto que ya no existe.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("alera_test")
                    .withUsername("alera")
                    .withPassword("test");

    static {
        POSTGRES.start();
    }
}
