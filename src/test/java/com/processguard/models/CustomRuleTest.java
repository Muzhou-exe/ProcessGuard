package com.processguard.models;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link CustomRule}.
 */
class CustomRuleTest {

    @Test
    void constructor_validInputs_fieldsSetCorrectly() {
        Condition c = new Condition("cpuUsage", "GREATER_THAN", "80");
        CustomRule rule = new CustomRule(1, "High CPU Rule", "Fires on high CPU",
                true, List.of(c), "AND", Severity.HIGH, "CPU is too high", 60, "ALERT");

        assertEquals(1, rule.getId());
        assertEquals("High CPU Rule", rule.getName());
        assertEquals("Fires on high CPU", rule.getDescription());
        assertTrue(rule.isEnabled());
        assertEquals(1, rule.getConditions().size());
        assertEquals("AND", rule.getLogicOperator());
        assertEquals(Severity.HIGH, rule.getSeverity());
        assertEquals("CPU is too high", rule.getMessageTemplate());
        assertEquals(60, rule.getCooldownSeconds());
        assertEquals("ALERT", rule.getAction());
    }

    @Test
    void constructor_nullName_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> new CustomRule(1, null, "", true, null, "AND",
                        Severity.LOW, "", 0, "ALERT"));
    }

    @Test
    void constructor_orLogicOperator_storedAsOr() {
        CustomRule rule = new CustomRule(1, "rule", "", true, null, "or",
                Severity.LOW, "", 0, "ALERT");
        assertEquals("OR", rule.getLogicOperator());
    }

    @Test
    void constructor_invalidLogicOperator_defaultsToAnd() {
        CustomRule rule = new CustomRule(1, "rule", "", true, null, "XOR",
                Severity.LOW, "", 0, "ALERT");
        assertEquals("AND", rule.getLogicOperator());
    }

    @Test
    void constructor_negativeCooldown_clampedToZero() {
        CustomRule rule = new CustomRule(1, "rule", "", true, null, "AND",
                Severity.LOW, "", -5, "ALERT");
        assertEquals(0, rule.getCooldownSeconds());
    }

    @Test
    void setEnabled_togglesState() {
        CustomRule rule = new CustomRule(1, "rule", "", true, null, "AND",
                Severity.LOW, "", 0, "ALERT");

        rule.setEnabled(false);
        assertFalse(rule.isEnabled());
    }

    @Test
    void getConditions_returnsDefensiveCopy() {
        Condition c = new Condition("name", "EQUALS", "test");
        CustomRule rule = new CustomRule(1, "rule", "", true, List.of(c), "AND",
                Severity.LOW, "", 0, "ALERT");

        List<Condition> conditions = rule.getConditions();
        conditions.clear();

        assertEquals(1, rule.getConditions().size());
    }
}
