package edu.sjsu.cmpe272.simpleblog.server.config;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import javax.sql.DataSource;

/*@Configuration
public class DBconfig {
    @Bean
    public DataSource getDataSource() {
        DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
        dataSourceBuilder.driverClassName("org.h2.Driver");
        dataSourceBuilder.url("jdbc:h2:tcp://10.0.0.164:9092/test");
        dataSourceBuilder.username("test");
        dataSourceBuilder.password("");
        return dataSourceBuilder.build();
    }
}*/
