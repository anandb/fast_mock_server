package io.github.anandb.mockserver.config;

import io.github.anandb.mockserver.util.MapperSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.http.MockHttpOutputMessage;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JsonMCHttpMessageConverter Tests")
class JsonMCHttpMessageConverterTest {

    private JsonMCHttpMessageConverter converter;

    @BeforeEach
    void setUp() {
        converter = new JsonMCHttpMessageConverter();
    }

    @Test
    void supportsAllClasses() {
        assertTrue(converter.supports(String.class));
        assertTrue(converter.supports(Object.class));
        assertTrue(converter.supports(Integer.class));
    }

    @Test
    void readInternalParsesJsonmc() throws Exception {
        String jsonmc = "{\n" +
                "  // comment line\n" +
                "  \"name\": \"test\",\n" +
                "  \"value\": 42\n" +
                "}";

        MockHttpInputMessage inputMessage = new MockHttpInputMessage(
                jsonmc.getBytes(StandardCharsets.UTF_8));

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> result = (java.util.Map<String, Object>)
                converter.readInternal(java.util.Map.class, inputMessage);

        assertEquals("test", result.get("name"));
        assertEquals(42, result.get("value"));
    }

    @Test
    void readInternalThrowsOnInvalidJson() {
        String invalid = "{bad json}";
        MockHttpInputMessage inputMessage = new MockHttpInputMessage(
                invalid.getBytes(StandardCharsets.UTF_8));

        assertThrows(HttpMessageNotReadableException.class, () ->
                converter.readInternal(Object.class, inputMessage));
    }

    @Test
    void writeInternalWritesJson() throws Exception {
        MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
        java.util.Map<String, String> data = java.util.Map.of("key", "value");

        converter.writeInternal(data, outputMessage);

        String written = outputMessage.getBodyAsString(StandardCharsets.UTF_8);
        assertTrue(written.contains("\"key\""));
        assertTrue(written.contains("\"value\""));
    }

    @Test
    void supportedMediaTypeIsJsonmc() {
        MediaType mediaType = new MediaType("application", "jsonmc", StandardCharsets.UTF_8);
        assertTrue(converter.canRead(Object.class, mediaType));
    }
}
