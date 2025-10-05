package io.github.anandb.mockserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.anandb.mockserver.util.MapperSupplier;

import java.util.List;

/**
 * Web configuration for the Mock Server application.
 * <p>
 * Configures custom HTTP message converters, including support for
 * the application/jsonmc MIME type which allows JSON with comments
 * and multiline strings.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Configures custom HTTP message converters.
     * <p>
     * Adds the JsonMultilineCommentHttpMessageConverter to handle
     * application/jsonmc content type. This converter is added at the
     * beginning of the converter list to ensure it's checked before
     * standard JSON converters.
     *
     * @param converters the list of configured HTTP message converters
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        // Add our custom converter at the beginning of the list
        // This ensures it's checked before the standard JSON converters
        converters.add(0, new JsonMultilineCommentHttpMessageConverter());
    }

    @Bean
    public ObjectMapper mapper() {
        return MapperSupplier.getMapper();
    }
}
