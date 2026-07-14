package io.github.anandb.mockserver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MockServerApplication Tests")
class MockServerApplicationTest {

    @Test
    void hasArgReturnsTrueForExistingFlag() throws Exception {
        boolean result = invokeHasArg(new String[]{"-f", "config.json"}, "-f");
        assertTrue(result);
    }

    @Test
    void hasArgReturnsTrueForLongFlag() throws Exception {
        boolean result = invokeHasArg(new String[]{"--config", "config.json"}, "--config");
        assertTrue(result);
    }

    @Test
    void hasArgReturnsFalseForMissingFlag() throws Exception {
        boolean result = invokeHasArg(new String[]{"-f", "config.json"}, "-h");
        assertFalse(result);
    }

    @Test
    void hasArgReturnsFalseForEmptyArgs() throws Exception {
        boolean result = invokeHasArg(new String[]{}, "-f");
        assertFalse(result);
    }

    @Test
    void hasArgMatchesMultipleFlags() throws Exception {
        boolean result = invokeHasArg(new String[]{"--help"}, "-h", "--help");
        assertTrue(result);
    }

    @Test
    void getOptionValueReturnsValueAfterShortFlag() throws Exception {
        String result = invokeGetOptionValue(new String[]{"-f", "myfile.json"}, "-f", "--config");
        assertEquals("myfile.json", result);
    }

    @Test
    void getOptionValueReturnsValueAfterLongFlag() throws Exception {
        String result = invokeGetOptionValue(new String[]{"--config", "myfile.json"}, "-f", "--config");
        assertEquals("myfile.json", result);
    }

    @Test
    void getOptionValueReturnsNullWhenFlagMissing() throws Exception {
        String result = invokeGetOptionValue(new String[]{"-g", "simple"}, "-f", "--config");
        assertNull(result);
    }

    @Test
    void getOptionValueReturnsNullForEmptyArgs() throws Exception {
        String result = invokeGetOptionValue(new String[]{}, "-f", "--config");
        assertNull(result);
    }

    @Test
    void getOptionValueReturnsNullWhenFlagIsLastElement() throws Exception {
        // Flag exists but has no following value
        String result = invokeGetOptionValue(new String[]{"-f"}, "-f", "--config");
        assertNull(result);
    }

    @Test
    void getOptionValueHandlesGenerateWithOutput() throws Exception {
        String[] args = {"-g", "simple", "-o", "output.json"};
        String type = invokeGetOptionValue(args, "-g", "--generate");
        String output = invokeGetOptionValue(args, "-o", "--output");
        assertEquals("simple", type);
        assertEquals("output.json", output);
    }

    // --- Reflection helpers ---

    private static boolean invokeHasArg(String[] args, String... flags) throws Exception {
        Method method = MockServerApplication.class.getDeclaredMethod("hasArg", String[].class, String[].class);
        method.setAccessible(true);
        return (boolean) method.invoke(null, (Object) args, (Object) flags);
    }

    private static String invokeGetOptionValue(String[] args, String shortFlag, String longFlag) throws Exception {
        Method method = MockServerApplication.class.getDeclaredMethod("getOptionValue", String[].class, String.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, (Object) args, shortFlag, longFlag);
    }
}
