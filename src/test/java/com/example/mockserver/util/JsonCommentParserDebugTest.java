package com.example.mockserver.util;

import org.junit.jupiter.api.Test;

class JsonCommentParserDebugTest {

    @Test
    void debugComplexExample() {
        String json = """
            {
                // User configuration
                "user": {
                    "name": "John Doe",
                    /* Contact info
                       Multiple lines */
                    "email": "john@example.com"
                },
                "bio": \"\"\"John is a software engineer.
            He loves coding and solving problems.
            His motto: "Keep it simple"\"\"\"
            }
            """;

        System.out.println("Input JSON:");
        System.out.println(json);
        System.out.println("\n--- Processing ---\n");

        try {
            String processed = getProcessedJson(json);
            System.out.println("Processed JSON:");
            System.out.println(processed);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getProcessedJson(String json) {
        // Access the private method through reflection for debugging
        try {
            java.lang.reflect.Method method = JsonCommentParser.class.getDeclaredMethod(
                "removeCommentsAndConvertMultilineStrings", String.class);
            method.setAccessible(true);
            return (String) method.invoke(null, json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
