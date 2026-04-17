package com.processguard.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for enum types: {@link Status}, {@link Severity}, {@link AlertType}, {@link RuleAction}.
 */
class EnumTest {

    // ── Status ────────────────────────────────────────────────────────────────

    @Test
    void status_hasFourValues() {
        assertEquals(4, Status.values().length);
    }

    @Test
    void status_allValuesParseCorrectly() {
        assertDoesNotThrow(() -> Status.valueOf("NORMAL"));
        assertDoesNotThrow(() -> Status.valueOf("SUSPICIOUS"));
        assertDoesNotThrow(() -> Status.valueOf("BLOCKED"));
        assertDoesNotThrow(() -> Status.valueOf("WHITELISTED"));
    }

    // ── Severity ──────────────────────────────────────────────────────────────

    @Test
    void severity_hasFourValues() {
        assertEquals(4, Severity.values().length);
    }

    @Test
    void severity_allValuesParseCorrectly() {
        assertDoesNotThrow(() -> Severity.valueOf("LOW"));
        assertDoesNotThrow(() -> Severity.valueOf("MEDIUM"));
        assertDoesNotThrow(() -> Severity.valueOf("HIGH"));
        assertDoesNotThrow(() -> Severity.valueOf("CRITICAL"));
    }

    @Test
    void severity_ordinalOrder_lowToHigh() {
        assertTrue(Severity.LOW.ordinal() < Severity.MEDIUM.ordinal());
        assertTrue(Severity.MEDIUM.ordinal() < Severity.HIGH.ordinal());
        assertTrue(Severity.HIGH.ordinal() < Severity.CRITICAL.ordinal());
    }

    // ── AlertType ─────────────────────────────────────────────────────────────

    @Test
    void alertType_hasSevenValues() {
        assertEquals(7, AlertType.values().length);
    }

    @Test
    void alertType_allValuesParseCorrectly() {
        assertDoesNotThrow(() -> AlertType.valueOf("HIGH_CPU_USAGE"));
        assertDoesNotThrow(() -> AlertType.valueOf("HIGH_MEMORY_USAGE"));
        assertDoesNotThrow(() -> AlertType.valueOf("BLACKLISTED_PROCESS"));
        assertDoesNotThrow(() -> AlertType.valueOf("UNKNOWN_PROCESS"));
        assertDoesNotThrow(() -> AlertType.valueOf("SUSPICIOUS_PARENT"));
        assertDoesNotThrow(() -> AlertType.valueOf("RAPID_SPAWN"));
        assertDoesNotThrow(() -> AlertType.valueOf("CUSTOM_RULE_VIOLATION"));
    }

    // ── RuleAction ────────────────────────────────────────────────────────────

    @Test
    void ruleAction_hasThreeValues() {
        assertEquals(3, RuleAction.values().length);
    }

    @Test
    void ruleAction_allValuesParseCorrectly() {
        assertDoesNotThrow(() -> RuleAction.valueOf("LOG_ONLY"));
        assertDoesNotThrow(() -> RuleAction.valueOf("ALERT_ONLY"));
        assertDoesNotThrow(() -> RuleAction.valueOf("KILL_PROCESS"));
    }

    @Test
    void ruleAction_fromString_validUpperCase() {
        assertEquals(RuleAction.LOG_ONLY,     RuleAction.fromString("LOG_ONLY"));
        assertEquals(RuleAction.ALERT_ONLY,   RuleAction.fromString("ALERT_ONLY"));
        assertEquals(RuleAction.KILL_PROCESS, RuleAction.fromString("KILL_PROCESS"));
    }

    @Test
    void ruleAction_fromString_caseInsensitive() {
        assertEquals(RuleAction.LOG_ONLY,     RuleAction.fromString("log_only"));
        assertEquals(RuleAction.ALERT_ONLY,   RuleAction.fromString("alert_only"));
        assertEquals(RuleAction.KILL_PROCESS, RuleAction.fromString("kill_process"));
    }

    @Test
    void ruleAction_fromString_withLeadingTrailingWhitespace() {
        assertEquals(RuleAction.LOG_ONLY, RuleAction.fromString("  LOG_ONLY  "));
    }

    @Test
    void ruleAction_fromString_nullReturnsAlertOnly() {
        assertEquals(RuleAction.ALERT_ONLY, RuleAction.fromString(null));
    }

    @Test
    void ruleAction_fromString_unknownValueReturnsAlertOnly() {
        assertEquals(RuleAction.ALERT_ONLY, RuleAction.fromString("INVALID"));
        assertEquals(RuleAction.ALERT_ONLY, RuleAction.fromString(""));
        assertEquals(RuleAction.ALERT_ONLY, RuleAction.fromString("ALERT"));
    }
}
