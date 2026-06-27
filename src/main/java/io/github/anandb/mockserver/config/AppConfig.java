package io.github.anandb.mockserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.anandb.mockserver.util.MapperSupplier;

/**
 * Application configuration providing shared beans.
 */
@Configuration
public class AppConfig {

    @Bean
    public ObjectMapper mapper() {
        return MapperSupplier.getMapper();
    }
}
