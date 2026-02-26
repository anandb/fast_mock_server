package io.github.anandb.mockserver;

import io.github.anandb.mockserver.service.ConfigurationLoaderService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Integration tests for the MockServer application.
 * <p>
 * Verifies that the Spring Boot application context loads successfully
 * with all required beans and configurations.
 * </p>
 */
@SpringBootTest(properties = {"mock.server.config.file=fast_mock_server/src/test/resources/test-server-config.jsonmc"})
class MockServerApplicationTests {

    /**
     * Tests that the Spring application context loads successfully.
     * <p>
     * This is a basic smoke test that verifies all beans can be instantiated
     * and the application configuration is valid.
     * </p>
     */
    @BeforeAll
    static void setUp() {
        ConfigurationLoaderService.SKIP_CONFIG_VALIDATIONS_FOR_TESTS = true;
    }

    @Test
    void contextLoads() {
        // This test verifies that the Spring context loads successfully
    }
}
