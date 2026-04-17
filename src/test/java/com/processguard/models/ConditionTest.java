package com.processguard.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Condition}.
 */
class ConditionTest {

    // ── Constructor ───────────────────────────────────────────────────────────

    @Test
    void constructor_validInputs_fieldsSetCorrectly() {
        Condition c = new Condition("cpuUsage", "GREATER_THAN", "80");

        assertEquals("cpuUsage",     c.getField());
        assertEquals("GREATER_THAN", c.getOperator());
        assertEquals("80",           c.getValue());
    }

    @Test
    void constructor_nullField_defaultsToEmpty() {
        Condition c = new Condition(null, "EQUALS", "test");
        assertEquals("", c.getField());
    }

    @Test
    void constructor_nullOperator_defaultsToEmpty() {
        Condition c = new Condition("name", null, "test");
        assertEquals("", c.getOperator());
    }

    @Test
    void constructor_nullValue_defaultsToEmpty() {
        Condition c = new Condition("name", "EQUALS", null);
        assertEquals("", c.getValue());
    }

    @Test
    void constructor_allNulls_defaultToEmpty() {
        Condition c = new Condition(null, null, null);
        assertEquals("", c.getField());
        assertEquals("", c.getOperator());
        assertEquals("", c.getValue());
    }

    @Test
    void constructor_operatorIsUppercased() {
        Condition c = new Condition("name", "equals", "chrome");
        assertEquals("EQUALS", c.getOperator());

        Condition c2 = new Condition("cpuUsage", "greater_than", "50");
        assertEquals("GREATER_THAN", c2.getOperator());
    }

    @Test
    void constructor_trimsWhitespaceFromAllFields() {
        Condition c = new Condition("  name  ", "  equals  ", "  chrome  ");
        assertEquals("name",   c.getField());
        assertEquals("EQUALS", c.getOperator());
        assertEquals("chrome", c.getValue());
    }

    @Test
    void constructor_fieldAndValueCasePreserved() {
        // field names are case-sensitive; values may be case-sensitive too
        Condition c = new Condition("cpuUsage", "EQUALS", "Chrome");
        assertEquals("cpuUsage", c.getField());
        assertEquals("Chrome",   c.getValue());
    }

    // ── toString ──────────────────────────────────────────────────────────────

    @Test
    void toString_containsAllThreeFields() {
        Condition c = new Condition("memoryUsageMB", "GREATER_THAN", "500");
        String s = c.toString();
        assertTrue(s.contains("memoryUsageMB"));
        assertTrue(s.contains("GREATER_THAN"));
        assertTrue(s.contains("500"));
    }

    @Test
    void toString_emptyCondition_doesNotThrow() {
        Condition c = new Condition(null, null, null);
        assertDoesNotThrow(c::toString);
    }
}
