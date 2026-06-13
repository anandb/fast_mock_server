package io.github.anandb.mockserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ConfigGeneratorService Tests")
class ConfigGeneratorServiceTest {

    private ConfigGeneratorService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        service = new ConfigGeneratorService();
    }

    @Test
    void getAvailableTypesReturnsAllTypes() {
        Set<String> types = service.getAvailableTypes();
        assertFalse(types.isEmpty());
        assertTrue(types.contains("simple"));
        assertTrue(types.contains("relay"));
        assertTrue(types.contains("mtls"));
        assertTrue(types.contains("sse"));
        assertTrue(types.contains("tunnel"));
        assertEquals(10, types.size());
    }

    @Test
    void getDescriptionReturnsNonNullForAllTypes() {
        for (String type : service.getAvailableTypes()) {
            assertNotNull(service.getDescription(type), "Description missing for type: " + type);
        }
    }

    @Test
    void generateCreatesFileForValidType() throws Exception {
        Path outPath = tempDir.resolve("output.json");
        service.generate("simple", outPath.toString());

        assertTrue(Files.exists(outPath));
        String content = Files.readString(outPath);
        assertTrue(content.contains("my-api"));
        assertTrue(content.contains("9001"));
    }

    @Test
    void generateCreatesParentDirectories() throws Exception {
        Path outPath = tempDir.resolve("sub/dir/config.json");
        service.generate("simple", outPath.toString());

        assertTrue(Files.exists(outPath));
    }

    @Test
    void generateThrowsOnUnknownType() {
        Path outPath = tempDir.resolve("output.json");
        assertThrows(IllegalArgumentException.class,
            () -> service.generate("nonexistent", outPath.toString()));
    }

    @Test
    void generateThrowsWithAvailableTypesInMessage() {
        Path outPath = tempDir.resolve("output.json");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> service.generate("bad-type", outPath.toString()));
        assertTrue(ex.getMessage().contains("simple"));
        assertTrue(ex.getMessage().contains("relay"));
    }

    @Test
    void generateAllTypes() throws Exception {
        for (String type : service.getAvailableTypes()) {
            Path outPath = tempDir.resolve(type + ".json");
            service.generate(type, outPath.toString());
            assertTrue(Files.exists(outPath), "Failed to generate type: " + type);
            assertTrue(Files.size(outPath) > 0, "Empty file for type: " + type);
        }
    }

    @Test
    void loadTemplateReturnsContent() throws Exception {
        String content = service.loadTemplate("simple");
        assertNotNull(content);
        assertFalse(content.isBlank());
        assertTrue(content.contains("my-api"));
    }

    @Test
    void loadTemplateThrowsOnUnknownType() {
        assertThrows(IllegalArgumentException.class,
            () -> service.loadTemplate("nonexistent"));
    }
}
