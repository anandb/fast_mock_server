package io.github.anandb.mockserver.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Singleton supplier for JsonMapper instances.
 * <p>
 * This class provides a single shared JsonMapper instance to be used throughout
 * the application, avoiding unnecessary object creation and ensuring consistent
 * JSON processing configuration.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MapperSupplier {

    private static final JsonMapper INSTANCE = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .addModule(new JavaTimeModule())
            .build();

    /**
     * Returns the singleton JsonMapper instance.
     *
     * @return the shared JsonMapper instance
     */
    public static JsonMapper getMapper() {
        return INSTANCE;
    }
}
