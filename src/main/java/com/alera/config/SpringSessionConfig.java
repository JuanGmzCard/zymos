package com.alera.config;

/**
 * La configuración de Spring Session se delega completamente a JdbcSessionAutoConfiguration,
 * que importa JdbcHttpSessionConfiguration (extends SpringHttpSessionConfiguration).
 *
 * El motivo es que SpringHttpSessionConfiguration.springSessionRepositoryFilter() crea un
 * FilterRegistrationBean con SessionRepositoryFilter.DEFAULT_ORDER = Integer.MIN_VALUE + 50,
 * garantizando que SessionRepositoryFilter corra ANTES que Spring Security (orden -100).
 *
 * Si se proporciona un SessionRepository bean propio, JdbcSessionAutoConfiguration se salta
 * (@ConditionalOnMissingBean), SessionRepositoryFilterConfiguration crea el filtro SIN un
 * FilterRegistrationBean explícito → Spring Boot lo registra en Ordered.LOWEST_PRECEDENCE
 * (Integer.MAX_VALUE) → corre DESPUÉS de Spring Security → el request no está envuelto
 * por Spring Session → isRequestedSessionIdValid() falla para POST → estrategia de sesión
 * inválida se dispara aunque el usuario esté autenticado.
 *
 * La configuración necesaria está en application.properties:
 *   spring.session.store-type=jdbc
 *   spring.session.jdbc.initialize-schema=never
 *   spring.session.jdbc.flush-mode=immediate
 *   server.servlet.session.timeout=8h
 */
public class SpringSessionConfig {
    // No beans — JdbcSessionAutoConfiguration maneja todo.
    // HttpSessionEventPublisher ya está en SecurityConfig.
}
