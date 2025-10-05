package io.github.anandb.mockserver.config;

import io.github.anandb.mockserver.util.JsonCommentParser;
import io.github.anandb.mockserver.util.MapperSupplier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/**
 * HTTP message converter for the custom application/jsonmc MIME type.
 * <p>
 * This converter handles JSON documents with extended syntax:
 * - C++ style comments (// and /* *\/)
 * - Multiline strings using backticks
 * <p>
 * When a request arrives with Content-Type: application/jsonmc, this converter:
 * 1. Parses the JSON with comments and multiline strings using JsonCommentParser
 * 2. Converts it to a standard JSON object
 * 3. Passes it to the standard JSON processing pipeline
 */
public class JsonMultilineCommentHttpMessageConverter extends AbstractHttpMessageConverter<Object> {

    private final JsonMapper objectMapper;

    public JsonMultilineCommentHttpMessageConverter() {
        super(new MediaType("application", "jsonmc", StandardCharsets.UTF_8));
        this.objectMapper = MapperSupplier.getMapper();
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        // Support all types - we'll convert to the target type using Jackson
        return true;
    }

    @Override
    protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {

        try {
            // Read the raw input with comments and multiline strings
            StringBuilder jsonWithComments = new StringBuilder();
            try (InputStreamReader reader = new InputStreamReader(
                    inputMessage.getBody(), StandardCharsets.UTF_8)) {
                char[] buffer = new char[1024];
                int bytesRead;
                while ((bytesRead = reader.read(buffer)) != -1) {
                    jsonWithComments.append(buffer, 0, bytesRead);
                }
            }

            // Parse using JsonCommentParser to get clean JSON
            JsonNode cleanJsonNode = JsonCommentParser.parse(jsonWithComments.toString());

            // Convert the clean JSON to the target class type
            return objectMapper.treeToValue(cleanJsonNode, clazz);

        } catch (Exception e) {
            throw new HttpMessageNotReadableException(
                    "Failed to parse application/jsonmc: " + e.getMessage(),
                    e, inputMessage);
        }
    }

    @Override
    protected void writeInternal(Object object, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {

        // For responses, we write standard JSON (not jsonmc)
        try (OutputStreamWriter writer = new OutputStreamWriter(
                outputMessage.getBody(), StandardCharsets.UTF_8)) {
            objectMapper.writeValue(writer, object);
        }
    }
}
