package com.clhost.memes.tree.conf;

import com.clhost.memes.tree.MemesDao;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@ConfigurationProperties(prefix = "spring.datasource")
@EnableTransactionManagement
public class JdbcConfiguration {
    @Bean
    public JdbcTemplate jdbcOperations(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(JdbcTemplate jdbcOperations) {
        return new NamedParameterJdbcTemplate(jdbcOperations);
    }

    @Bean
    public MemesDao memesDao(NamedParameterJdbcTemplate jdbcTemplate) {
        return new MemesDao(jdbcTemplate);
    }
}
