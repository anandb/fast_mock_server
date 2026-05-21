package io.github.anandb.mockserver.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FreemarkerTemplateDetector Tests")
class FreemarkerTemplateDetectorTest {

    @Test
    void detectsDollarBraceSyntax() {
        assertTrue(FreemarkerTemplateDetector.isFreemarkerTemplate("Hello ${name}!"));
    }

    @Test
    void detectsHashTagSyntax() {
        assertTrue(FreemarkerTemplateDetector.isFreemarkerTemplate("<#list items>"));
    }

    @Test
    void detectsBracketHashSyntax() {
        assertTrue(FreemarkerTemplateDetector.isFreemarkerTemplate("[#-- comment --]"));
    }

    @Test
    void detectsAngleAtSyntax() {
        assertTrue(FreemarkerTemplateDetector.isFreemarkerTemplate("<@compress>"));
    }

    @Test
    void detectsBracketAtSyntax() {
        assertTrue(FreemarkerTemplateDetector.isFreemarkerTemplate("[@spring.bind]"));
    }

    @Test
    void returnsFalseForPlainText() {
        assertFalse(FreemarkerTemplateDetector.isFreemarkerTemplate("Just plain text"));
    }

    @Test
    void returnsFalseForEmptyString() {
        assertFalse(FreemarkerTemplateDetector.isFreemarkerTemplate(""));
    }

    @Test
    void returnsFalseForNull() {
        assertFalse(FreemarkerTemplateDetector.isFreemarkerTemplate(null));
    }
}
