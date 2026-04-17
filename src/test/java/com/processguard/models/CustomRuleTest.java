package com.processguard.models;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CustomRule}.
 *
 * NOTE: The CustomRule constructor takes a {@link RuleAction} enum, not a String.
 * Several old tests passed a String literal ("ALERT") which would not compile —
 * all tests here use the correct RuleAction enum constants.
 */
class CustomRuleTest {

    /** Convenience builder for the common case. */
    private CustomRule buildRule(long id, String name, boolean enabled,
                                 List<Condition> conditions, String logic,
                                 Severity severity, String msg,
                                 int cooldown, RuleAction action) {
        return new CustomRule(id, name, "", enabled, conditions, logic, severity, msg, cooldown, action);
    }

    // ── Constructor – happy path ───────────────────────────────────────────────

    @Test
    void constructor_validInputs_allFieldsCorrect() {
        Condition c = new Condition("cpuUsage", "GREATER_THAN", "80");
        CustomRule rule = new CustomRule(
                1, "High CPU Rule", "Fires on high CPU",
                true, List.of(c), "AND",
                Severity.HIGH, "CPU is too high",
                60, RuleAction.ALERT_ONLY
        );

        assertEquals(1,                    rule.getId());
        assertEquals("High CPU Rule",      rule.getName());
        assertEquals("Fires on high CPU",  rule.getDescription());
        assertTrue(rule.isEnabled());
        assertEquals(1,                    rule.getConditions().size());
        assertEquals("AND",                rule.getLogicOperator());
        assertEquals(Severity.HIGH,        rule.getSeverity());
        assertEquals("CPU is too high",    rule.getMessageTemplate());
        assertEquals(60,                   rule.getCooldownSeconds());
        assertEquals(RuleAction.ALERT_ONLY, rule.getAction());
    }

    // ── Null / default handling ────────────────────────────────────────────────

    @Test
    void constructor_nullName_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () ->
                new CustomRule(1, null, "", true, null, "AND",
                        Severity.LOW, "", 0, RuleAction.ALERT_ONLY));
    }

    @Test
    void constructor_nullSeverity_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () ->
                new CustomRule(1, "rule", "", true, null, "AND",
                        null, "msg", 0, RuleAction.ALERT_ONLY));
    }

    @Test
    void constructor_nullDescription_defaultsToEmpty() {
        CustomRule rule = new CustomRule(1, "rule", null, true, null,
                "AND", Severity.LOW, "msg", 0, RuleAction.ALERT_ONLY);
        assertEquals("", rule.getDescription());
    }

    @Test
    void constructor_nullConditions_defaultsToEmptyList() {
        CustomRule rule = buildRule(1, "rule", true, null,
                "AND", Severity.LOW, "msg", 0, RuleAction.ALERT_ONLY);
        assertNotNull(rule.getConditions());
        assertTrue(rule.getConditions().isEmpty());
    }

    @Test
    void constructor_nullAction_defaultsToAlertOnly() {
        CustomRule rule = new CustomRule(1, "rule", "", true, null,
                "AND", Severity.LOW, "msg", 0, null);
        assertEquals(RuleAction.ALERT_ONLY, rule.getAction());
    }

    @Test
    void constructor_blankMessageTemplate_generatesDefault() {
        CustomRule rule = buildRule(1, "MyRule", true, null,
                "AND", Severity.LOW, "   ", 0, RuleAction.ALERT_ONLY);
        assertTrue(rule.getMessageTemplate().contains("MyRule"));
    }

    @Test
    void constructor_nullMessageTemplate_generatesDefault() {
        CustomRule rule = new CustomRule(1, "MyRule", "", true, null,
                "AND", Severity.LOW, null, 0, RuleAction.ALERT_ONLY);
        assertTrue(rule.getMessageTemplate().contains("MyRule"));
    }

    // ── Logic operator normalisation ──────────────────────────────────────────

    @Test
    void constructor_orLogic_storedAsOR() {
        CustomRule rule = buildRule(1, "rule", true, null,
                "or", Severity.LOW, "msg", 0, RuleAction.ALERT_ONLY);
        assertEquals("OR", rule.getLogicOperator());
    }

    @Test
    void constructor_andLogic_storedAsAND() {
        CustomRule rule = buildRule(1, "rule", true, null,
                "and", Severity.LOW, "msg", 0, RuleAction.ALERT_ONLY);
        assertEquals("AND", rule.getLogicOperator());
    }

    @Test
    void constructor_invalidLogic_defaultsToAND() {
        CustomRule rule = buildRule(1, "rule", true, null,
                "XOR", Severity.LOW, "msg", 0, RuleAction.ALERT_ONLY);
        assertEquals("AND", rule.getLogicOperator());
    }

    @Test
    void constructor_nullLogic_defaultsToAND() {
        CustomRule rule = buildRule(1, "rule", true, null,
                null, Severity.LOW, "msg", 0, RuleAction.ALERT_ONLY);
        assertEquals("AND", rule.getLogicOperator());
    }

    // ── Cooldown clamping ─────────────────────────────────────────────────────

    @Test
    void constructor_negativeCooldown_clampedToZero() {
        CustomRule rule = buildRule(1, "rule", true, null,
                "AND", Severity.LOW, "msg", -99, RuleAction.ALERT_ONLY);
        assertEquals(0, rule.getCooldownSeconds());
    }

    @Test
    void constructor_zeroCooldown_stored() {
        CustomRule rule = buildRule(1, "rule", true, null,
                "AND", Severity.LOW, "msg", 0, RuleAction.ALERT_ONLY);
        assertEquals(0, rule.getCooldownSeconds());
    }

    @Test
    void constructor_positiveCooldown_stored() {
        CustomRule rule = buildRule(1, "rule", true, null,
                "AND", Severity.LOW, "msg", 120, RuleAction.ALERT_ONLY);
        assertEquals(120, rule.getCooldownSeconds());
    }

    // ── setEnabled ────────────────────────────────────────────────────────────

    @Test
    void setEnabled_false_updatesCorrectly() {
        CustomRule rule = buildRule(1, "rule", true, null,
                "AND", Severity.LOW, "msg", 0, RuleAction.ALERT_ONLY);
        rule.setEnabled(false);
        assertFalse(rule.isEnabled());
    }

    @Test
    void setEnabled_true_updatesCorrectly() {
        CustomRule rule = buildRule(1, "rule", false, null,
                "AND", Severity.LOW, "msg", 0, RuleAction.ALERT_ONLY);
        rule.setEnabled(true);
        assertTrue(rule.isEnabled());
    }

    // ── getConditions defensive copy ──────────────────────────────────────────

    @Test
    void getConditions_returnsDefensiveCopy() {
        Condition c = new Condition("name", "EQUALS", "test");
        CustomRule rule = buildRule(1, "rule", true, new ArrayList<>(List.of(c)),
                "AND", Severity.LOW, "msg", 0, RuleAction.ALERT_ONLY);

        List<Condition> copy = rule.getConditions();
        copy.clear();

        assertEquals(1, rule.getConditions().size());
    }

    // ── RuleAction variants ───────────────────────────────────────────────────

    @Test
    void constructor_logOnlyAction_stored() {
        CustomRule rule = buildRule(1, "rule", true, null,
                "AND", Severity.LOW, "msg", 0, RuleAction.LOG_ONLY);
        assertEquals(RuleAction.LOG_ONLY, rule.getAction());
    }

    @Test
    void constructor_killProcessAction_stored() {
        CustomRule rule = buildRule(1, "rule", true, null,
                "AND", Severity.CRITICAL, "msg", 0, RuleAction.KILL_PROCESS);
        assertEquals(RuleAction.KILL_PROCESS, rule.getAction());
    }

    // ── toString ──────────────────────────────────────────────────────────────

    @Test
    void toString_containsIdNameEnabledSeverityAction() {
        CustomRule rule = buildRule(7, "MyRule", true, null,
                "AND", Severity.HIGH, "msg", 0, RuleAction.ALERT_ONLY);
        String s = rule.toString();
        assertTrue(s.contains("7"));
        assertTrue(s.contains("MyRule"));
        assertTrue(s.contains("HIGH"));
        assertTrue(s.contains("ALERT_ONLY"));
    }
}
