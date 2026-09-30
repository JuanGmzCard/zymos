package com.alera.config;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.session.FlushMode;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.transaction.support.TransactionOperations;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;

@Configuration
public class SpringSessionConfig {

    /**
     * Repositorio de sesiones con DataSource wrapper de referencia distinta al HikariCP primario.
     *
     * Root cause: TransactionSynchronizationManager (TSM) indexa recursos por referencia exacta
     * del objeto DataSource. Si JpaTransactionManager tiene una conexión enlazada en TSM bajo
     * la clave del DataSource primario, el JdbcTemplate de Spring Session la reutilizaría
     * (con estado de transacción o snapshot que no ve filas recién comprometidas) → findById()
     * retorna null → sesión inválida falsa → ZymosInvalidSessionStrategy → /login?expired=true.
     *
     * Con wrapper de referencia distinta: TSM nunca tiene ConnectionHolder bajo esa clave →
     * siempre conexión fresca de HikariCP (autoCommit=true, READ COMMITTED) → findById() ve
     * todas las filas.
     *
     * El wrapper NO se registra como @Bean: evita que DataSourceAutoConfiguration saltee la
     * creación del HikariCP primario por @ConditionalOnMissingBean(DataSource.class).
     *
     * JdbcSessionAutoConfiguration usa @ConditionalOnMissingBean(SessionRepository.class) →
     * no crea un segundo JdbcIndexedSessionRepository al encontrar este bean.
     */
    @Bean
    public JdbcIndexedSessionRepository sessionRepository(
            DataSource dataSource,
            ServerProperties serverProperties) {

        DataSource wrapper = new AbstractDataSource() {
            @Override
            public Connection getConnection() throws SQLException {
                return dataSource.getConnection();
            }
            @Override
            public Connection getConnection(String username, String password) throws SQLException {
                return dataSource.getConnection(username, password);
            }
        };

        JdbcIndexedSessionRepository repo = new JdbcIndexedSessionRepository(
                new JdbcTemplate(wrapper),
                TransactionOperations.withoutTransaction());
        repo.setFlushMode(FlushMode.IMMEDIATE);

        Duration timeout = serverProperties.getServlet().getSession().getTimeout();
        if (timeout != null) {
            repo.setDefaultMaxInactiveInterval(timeout);
        }

        return repo;
    }
}
