package io.github.anandb.mockserver.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Singleton supplier for JsonMapper instances.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MapperSupplier {

    private static final class Holder {
        private static final JsonMapper INSTANCE = JsonMapper.builder()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
    }

    public static JsonMapper getMapper() {
        return Holder.INSTANCE;
    }
}
