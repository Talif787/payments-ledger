package com.reconciliation.infrastructure.config;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Two datasources: the reconciliation service's own store (primary, read-write,
 * migrated by Flyway) and a read-only connection to the ledger database (a read
 * replica in production). Keeping them separate enforces that reconciliation
 * never writes ledger state.
 */
@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties reconciliationDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    public DataSource reconciliationDataSource() {
        return reconciliationDataSourceProperties().initializeDataSourceBuilder().build();
    }

    @Bean
    @ConfigurationProperties("recon.ledger-datasource")
    public DataSourceProperties ledgerDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource ledgerDataSource() {
        return ledgerDataSourceProperties().initializeDataSourceBuilder().build();
    }

    @Bean
    @Primary
    public NamedParameterJdbcTemplate reconciliationJdbc(
            @Qualifier("reconciliationDataSource") DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @Bean
    public NamedParameterJdbcTemplate ledgerJdbc(@Qualifier("ledgerDataSource") DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(
            @Qualifier("reconciliationDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
