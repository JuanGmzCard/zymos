package com.alera.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Configura Spring Session JDBC para usar DataSourceTransactionManager en lugar del
 * JpaTransactionManager primario. Esto evita que las interacciones del EntityManager
 * de JPA (open-in-view, lecturas read-only) interfieran con el SELECT de findById().
 *
 * Sin este bean, Spring Session usa JpaTransactionManager (@Transactional readOnly=true)
 * que en ciertos casos no ve rows recién committed, causando que sesiones válidas sean
 * consideradas inválidas (ZymosInvalidSessionStrategy dispara incorrectamente).
 */
@Configuration
public class SpringSessionConfig {

    @Bean("springSessionTransactionManager")
    public PlatformTransactionManager springSessionTransactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
