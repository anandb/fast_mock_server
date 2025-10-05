package io.github.anandb.mockserver.config;

import io.github.anandb.mockserver.util.JsonCommentParser;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
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
 * - Multiline strings using triple quotes (""")
 * <p>
 * When a request arrives with Content-Type: application/jsonmc, this converter:
 * 1. Parses the JSON with comments and multiline strings using JsonCommentParser
 * 2. Converts it to a standard JSON object
 * 3. Passes it to the standard JSON processing pipeline
 */
public class JsonMultilineCommentHttpMessageConverter extends AbstractHttpMessageConverter<Object> {

    private final Gson gson;

    public JsonMultilineCommentHttpMessageConverter() {
        super(new MediaType("application", "jsonmc", StandardCharsets.UTF_8));
        this.gson = new Gson();
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        // Support all types - we'll convert to the target type using Gson
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
            JsonObject cleanJsonObject = JsonCommentParser.parse(jsonWithComments.toString());

            // Convert the clean JSON to the target class type
            return gson.fromJson(cleanJsonObject, clazz);

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
            gson.toJson(object, writer);
        }
    }
}
