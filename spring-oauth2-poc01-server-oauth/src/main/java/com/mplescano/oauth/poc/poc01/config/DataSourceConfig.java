package com.mplescano.oauth.poc.poc01.config;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import javax.sql.DataSource;

import org.hsqldb.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
public class DataSourceConfig {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Bean(initMethod = "start", destroyMethod = "stop")
    public Server hsqlServer(@Value("${hsqldb.database.path}") String path, 
                                  @Value("${hsqldb.server.port}") int port, 
                                  @Value("${hsqldb.database.name}") String name) {

        Server server = new Server();
        server.setDatabaseName(0, name);
        server.setDatabasePath(0, path);
        server.setNoSystemExit(true);
        server.setPort(port);
        server.setLogWriter(slf4jPrintWriter());
        server.setErrWriter(slf4jPrintWriter());

        return server;
    }
    
    private PrintWriter slf4jPrintWriter() {
        return new PrintWriter(new ByteArrayOutputStream()) {
            @Override
            public void println(final String x) {
                log.debug(x);
            }
        };
    }
 
    @Bean
    @DependsOn("hsqlServer")
    public DataSource dataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }
    
}