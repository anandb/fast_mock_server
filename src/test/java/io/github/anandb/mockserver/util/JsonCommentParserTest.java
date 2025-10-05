package io.github.anandb.mockserver.util;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonCommentParserTest {

    @Test
    void testSimpleJsonWithoutComments() {
        String json = "{\"name\": \"John\", \"age\": 30}";
        JsonNode result = JsonCommentParser.parse(json);

        assertEquals("John", result.get("name").asText());
        assertEquals(30, result.get("age").asInt());
    }

    @Test
    void testSingleLineComment() {
        String json = """
            {
                // This is a comment
                "name": "John",
                "age": 30 // Another comment
            }
            """;
        JsonNode result = JsonCommentParser.parse(json);

        assertEquals("John", result.get("name").asText());
        assertEquals(30, result.get("age").asInt());
    }

    @Test
    void testMultiLineComment() {
        String json = """
            {
                /* This is a
                   multi-line comment */
                "name": "John",
                "age": 30
            }
            """;
        JsonNode result = JsonCommentParser.parse(json);

        assertEquals("John", result.get("name").asText());
        assertEquals(30, result.get("age").asInt());
    }

    @Test
    void testMixedComments() {
        String json = """
            {
                // Single line comment
                "name": "John", /* inline multi-line */
                /* Multi-line
                   comment */
                "age": 30
            }
            """;
        JsonNode result = JsonCommentParser.parse(json);

        assertEquals("John", result.get("name").asText());
        assertEquals(30, result.get("age").asInt());
    }

    @Test
    void testMultilineString() {
        String json = "{\"description\": `This is a\nmulti-line\ndescription`}";
        JsonNode result = JsonCommentParser.parse(json);

        String expected = "This is a\nmulti-line\ndescription";
        assertEquals(expected, result.get("description").asText());
    }

    @Test
    void testMultilineStringWithDoubleQuotes() {
        String json = "{\"quote\": `He said \"Hello\" to me`}";
        JsonNode result = JsonCommentParser.parse(json);

        assertEquals("He said \"Hello\" to me", result.get("quote").asText());
    }

    @Test
    void testMultilineStringWithBackslashes() {
        String json = "{\"path\": `C:\\\\Users\\\\test`}";
        JsonNode result = JsonCommentParser.parse(json);

        assertEquals("C:\\\\Users\\\\test", result.get("path").asText());
    }

    @Test
    void testCommentsInsideRegularStrings() {
        String json = """
            {
                "url": "http://example.com//path",
                "comment": "This is not /* a comment */"
            }
            """;
        JsonNode result = JsonCommentParser.parse(json);

        assertEquals("http://example.com//path", result.get("url").asText());
        assertEquals("This is not /* a comment */", result.get("comment").asText());
    }

    @Test
    void testEscapedQuotesInRegularStrings() {
        String json = """
            {
                "quote": "He said \\"Hello\\""
            }
            """;
        JsonNode result = JsonCommentParser.parse(json);

        assertEquals("He said \"Hello\"", result.get("quote").asText());
    }

    @Test
    void testComplexExample() {
        String json = """
            {
                // User configuration
                "user": {
                    "name": "John Doe",
                    /* Contact info
                       Multiple lines */
                    "email": "john@example.com"
                },
                "description": "Simple text without special chars"
            }
            """;
        JsonNode result = JsonCommentParser.parse(json);

        JsonNode user = result.get("user");
        assertEquals("John Doe", user.get("name").asText());
        assertEquals("john@example.com", user.get("email").asText());
        assertEquals("Simple text without special chars", result.get("description").asText());
    }

    @Test
    void testMultilineStringWithSpecialCharacters() {
        String json = "{\"text\": `Line 1\n\tTabbed line\nLine with \"quotes\" and \\\\backslash\\\\`}";
        JsonNode result = JsonCommentParser.parse(json);

        String expected = "Line 1\n\tTabbed line\nLine with \"quotes\" and \\\\backslash\\\\";
        assertEquals(expected, result.get("text").asText());
    }

    @Test
    void testEmptyMultilineString() {
        String json = "{\"empty\": ``}";
        JsonNode result = JsonCommentParser.parse(json);

        assertEquals("", result.get("empty").asText());
    }

    @Test
    void testMultipleMultilineStrings() {
        String json = "{\n" +
            "    \"first\": `First\nmultiline`,\n" +
            "    \"second\": `Second\nmultiline`\n" +
            "}";
        JsonNode result = JsonCommentParser.parse(json);

        assertEquals("First\nmultiline", result.get("first").asText());
        assertEquals("Second\nmultiline", result.get("second").asText());
    }

    @Test
    void testCommentAtEndOfFile() {
        String json = """
            {
                "name": "John"
            }
            // Final comment""";
        JsonNode result = JsonCommentParser.parse(json);

        assertEquals("John", result.get("name").asText());
    }

    @Test
    void testCommentAtStartOfFile() {
        String json = """
            // Header comment
            {
                "name": "John"
            }
            """;
        JsonNode result = JsonCommentParser.parse(json);

        assertEquals("John", result.get("name").asText());
    }

    @Test
    void testUnclosedMultilineString() {
        String json = """
            {
                "text": `This is unclosed
            }
            """;

        assertThrows(IllegalArgumentException.class, () -> {
            JsonCommentParser.parse(json);
        });
    }

    @Test
    void testUnclosedMultiLineComment() {
        String json = """
            {
                /* This comment is not closed
                "name": "John"
            }
            """;

        assertThrows(IllegalArgumentException.class, () -> {
            JsonCommentParser.parse(json);
        });
    }

    @Test
    void testUnclosedRegularString() {
        String json = """
            {
                "name": "John
            }
            """;

        assertThrows(IllegalArgumentException.class, () -> {
            JsonCommentParser.parse(json);
        });
    }

    @Test
    void testNullInput() {
        assertThrows(IllegalArgumentException.class, () -> {
            JsonCommentParser.parse(null);
        });
    }

    @Test
    void testInvalidJsonAfterProcessing() {
        String json = """
            {
                "name": "John",
            }
            """;

        assertThrows(IllegalArgumentException.class, () -> {
            JsonCommentParser.parse(json);
        });
    }

    @Test
    void testWindowsLineEndings() {
        String json = "{\r\n  // Comment\r\n  \"name\": \"John\"\r\n}";
        JsonNode result = JsonCommentParser.parse(json);

        assertEquals("John", result.get("name").asText());
    }

    @Test
    void testMultilineStringWithWindowsLineEndings() {
        String json = "{\"text\": `Line1\r\nLine2`}";
        JsonNode result = JsonCommentParser.parse(json);

        assertEquals("Line1\nLine2", result.get("text").asText());
    }

    @Test
    void testNestedObjects() {
        String json = """
            {
                // Root level
                "user": {
                    // User level
                    "name": "John",
                    "address": {
                        /* Address level */
                        "city": "NYC"
                    }
                }
            }
            """;
        JsonNode result = JsonCommentParser.parse(json);

        JsonNode user = result.get("user");
        JsonNode address = user.get("address");
        assertEquals("NYC", address.get("city").asText());
    }

    @Test
    void testArrayWithComments() {
        String json = """
            {
                // List of names
                "names": [
                    "John", // First name
                    "Jane", /* Second name */
                    "Bob"
                ]
            }
            """;
        JsonNode result = JsonCommentParser.parse(json);

        assertEquals(3, result.get("names").size());
        assertEquals("John", result.get("names").get(0).asText());
    }

    @Test
    void testMultilineStringInArray() {
        String json = "{\n" +
            "    \"messages\": [\n" +
            "        `First\nmessage`,\n" +
            "        `Second\nmessage`\n" +
            "    ]\n" +
            "}";
        JsonNode result = JsonCommentParser.parse(json);

        assertEquals("First\nmessage", result.get("messages").get(0).asText());
        assertEquals("Second\nmessage", result.get("messages").get(1).asText());
    }
}
