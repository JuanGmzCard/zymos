package com.alera.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.session.jdbc.config.annotation.SpringSessionTransactionManager;

import javax.sql.DataSource;

/**
 * Aísla Spring Session JDBC del JpaTransactionManager de Hibernate.
 *
 * Problema: JpaBaseConfiguration.transactionManager() tiene @ConditionalOnMissingBean(PlatformTransactionManager.class).
 * Si solo registramos el DataSourceTransactionManager para Spring Session, esa condición se satisface
 * y el bean 'transactionManager' de JPA nunca se crea → la app no arranca.
 *
 * Solución: definir ambos beans explícitamente.
 *   1. JpaTransactionManager  →  @Primary, nombre 'transactionManager' (igual que el auto-config)
 *   2. DataSourceTransactionManager → @SpringSessionTransactionManager (qualifier que JdbcHttpSessionConfiguration
 *      usa en su @Autowired para inyectar el TM de sesiones, separándolo de JPA)
 *
 * Con DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION en Hibernate, el EntityManager abre la tx pero
 * no adquiere la conexión JDBC de inmediato. Al usar JpaTransactionManager (REQUIRES_NEW) para findById(),
 * JdbcTemplate no encontraba ConnectionHolder en el hilo → SELECT devolvía 0 filas aunque el registro
 * existiera → isRequestedSessionIdValid() = false → ZymosInvalidSessionStrategy → /login?expired=true.
 * DataSourceTransactionManager gestiona una conexión JDBC directa sin interferencia de Hibernate.
 */
@Configuration
public class SpringSessionConfig {

    @Bean
    @Primary
    public JpaTransactionManager transactionManager(EntityManagerFactory emf,
            ObjectProvider<TransactionManagerCustomizers> customizers) {
        JpaTransactionManager tm = new JpaTransactionManager(emf);
        customizers.ifAvailable(c -> c.customize(tm));
        return tm;
    }

    @Bean
    @SpringSessionTransactionManager
    public DataSourceTransactionManager springSessionTransactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}