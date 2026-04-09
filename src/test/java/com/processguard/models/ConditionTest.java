package com.processguard.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link Condition}.
 */
class ConditionTest {

    @Test
    void constructor_validInputs_fieldsSetCorrectly() {
        Condition c = new Condition("cpuUsage", "GREATER_THAN", "80");

        assertEquals("cpuUsage", c.getField());
        assertEquals("GREATER_THAN", c.getOperator());
        assertEquals("80", c.getValue());
    }

    @Test
    void constructor_nullInputs_defaultToEmpty() {
        Condition c = new Condition(null, null, null);

        assertEquals("", c.getField());
        assertEquals("", c.getOperator());
        assertEquals("", c.getValue());
    }

    @Test
    void constructor_trims_whitespace() {
        Condition c = new Condition("  name  ", "  equals  ", "  test  ");

        assertEquals("name", c.getField());
        assertEquals("EQUALS", c.getOperator());
        assertEquals("test", c.getValue());
    }

    @Test
    void toString_containsAllFields() {
        Condition c = new Condition("memoryUsageMB", "GREATER_THAN", "500");
        String result = c.toString();

        assertEquals(true, result.contains("memoryUsageMB"));
        assertEquals(true, result.contains("GREATER_THAN"));
        assertEquals(true, result.contains("500"));
    }
}
