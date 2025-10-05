package io.github.anandb.mockserver.util;

import com.google.gson.JsonObject;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonCommentParserTest {

    @Test
    void testSimpleJsonWithoutComments() {
        String json = "{\"name\": \"John\", \"age\": 30}";
        JsonObject result = JsonCommentParser.parse(json);

        assertEquals("John", result.get("name").getAsString());
        assertEquals(30, result.get("age").getAsInt());
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
        JsonObject result = JsonCommentParser.parse(json);

        assertEquals("John", result.get("name").getAsString());
        assertEquals(30, result.get("age").getAsInt());
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
        JsonObject result = JsonCommentParser.parse(json);

        assertEquals("John", result.get("name").getAsString());
        assertEquals(30, result.get("age").getAsInt());
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
        JsonObject result = JsonCommentParser.parse(json);

        assertEquals("John", result.get("name").getAsString());
        assertEquals(30, result.get("age").getAsInt());
    }

    @Test
    void testMultilineString() {
        String json = "{\"description\": \"\"\"This is a\nmulti-line\ndescription\"\"\"}";
        JsonObject result = JsonCommentParser.parse(json);

        String expected = "This is a\nmulti-line\ndescription";
        assertEquals(expected, result.get("description").getAsString());
    }

    @Test
    void testMultilineStringWithDoubleQuotes() {
        String json = "{\"quote\": \"\"\"He said \"Hello\" to me\"\"\"}";
        JsonObject result = JsonCommentParser.parse(json);

        assertEquals("He said \"Hello\" to me", result.get("quote").getAsString());
    }

    @Test
    void testMultilineStringWithBackslashes() {
        String json = "{\"path\": \"\"\"C:\\\\Users\\\\test\"\"\"}";
        JsonObject result = JsonCommentParser.parse(json);

        assertEquals("C:\\\\Users\\\\test", result.get("path").getAsString());
    }

    @Test
    void testCommentsInsideRegularStrings() {
        String json = """
            {
                "url": "http://example.com//path",
                "comment": "This is not /* a comment */"
            }
            """;
        JsonObject result = JsonCommentParser.parse(json);

        assertEquals("http://example.com//path", result.get("url").getAsString());
        assertEquals("This is not /* a comment */", result.get("comment").getAsString());
    }

    @Test
    void testEscapedQuotesInRegularStrings() {
        String json = """
            {
                "quote": "He said \\"Hello\\""
            }
            """;
        JsonObject result = JsonCommentParser.parse(json);

        assertEquals("He said \"Hello\"", result.get("quote").getAsString());
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
        JsonObject result = JsonCommentParser.parse(json);

        JsonObject user = result.getAsJsonObject("user");
        assertEquals("John Doe", user.get("name").getAsString());
        assertEquals("john@example.com", user.get("email").getAsString());
        assertEquals("Simple text without special chars", result.get("description").getAsString());
    }

    @Test
    void testMultilineStringWithSpecialCharacters() {
        String json = "{\"text\": \"\"\"Line 1\n\tTabbed line\nLine with \"quotes\" and \\\\backslash\\\\\"\"\"}";
        JsonObject result = JsonCommentParser.parse(json);

        String expected = "Line 1\n\tTabbed line\nLine with \"quotes\" and \\\\backslash\\\\";
        assertEquals(expected, result.get("text").getAsString());
    }

    @Test
    void testEmptyMultilineString() {
        String json = "{\"empty\": \"\"\"\"\"\"}";
        JsonObject result = JsonCommentParser.parse(json);

        assertEquals("", result.get("empty").getAsString());
    }

    @Test
    void testMultipleMultilineStrings() {
        String json = "{\n" +
            "    \"first\": \"\"\"First\nmultiline\"\"\",\n" +
            "    \"second\": \"\"\"Second\nmultiline\"\"\"\n" +
            "}";
        JsonObject result = JsonCommentParser.parse(json);

        assertEquals("First\nmultiline", result.get("first").getAsString());
        assertEquals("Second\nmultiline", result.get("second").getAsString());
    }

    @Test
    void testCommentAtEndOfFile() {
        String json = """
            {
                "name": "John"
            }
            // Final comment""";
        JsonObject result = JsonCommentParser.parse(json);

        assertEquals("John", result.get("name").getAsString());
    }

    @Test
    void testCommentAtStartOfFile() {
        String json = """
            // Header comment
            {
                "name": "John"
            }
            """;
        JsonObject result = JsonCommentParser.parse(json);

        assertEquals("John", result.get("name").getAsString());
    }

    @Test
    void testUnclosedMultilineString() {
        String json = """
            {
                "text": \"\"\"This is unclosed
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
        JsonObject result = JsonCommentParser.parse(json);

        assertEquals("John", result.get("name").getAsString());
    }

    @Test
    void testMultilineStringWithWindowsLineEndings() {
        String json = "{\"text\": \"\"\"Line1\r\nLine2\"\"\"}";
        JsonObject result = JsonCommentParser.parse(json);

        assertEquals("Line1\nLine2", result.get("text").getAsString());
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
        JsonObject result = JsonCommentParser.parse(json);

        JsonObject user = result.getAsJsonObject("user");
        JsonObject address = user.getAsJsonObject("address");
        assertEquals("NYC", address.get("city").getAsString());
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
        JsonObject result = JsonCommentParser.parse(json);

        assertEquals(3, result.getAsJsonArray("names").size());
        assertEquals("John", result.getAsJsonArray("names").get(0).getAsString());
    }

    @Test
    void testMultilineStringInArray() {
        String json = "{\n" +
            "    \"messages\": [\n" +
            "        \"\"\"First\nmessage\"\"\",\n" +
            "        \"\"\"Second\nmessage\"\"\"\n" +
            "    ]\n" +
            "}";
        JsonObject result = JsonCommentParser.parse(json);

        assertEquals("First\nmessage", result.getAsJsonArray("messages").get(0).getAsString());
        assertEquals("Second\nmessage", result.getAsJsonArray("messages").get(1).getAsString());
    }
}
