package com.processguard.models;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AlertEvent}.
 *
 * NOTE: AlertEvent does NOT override equals/hashCode, so identity equality applies.
 * The old test that called assertEquals(a, b) for two different objects with the same
 * id was incorrect and has been removed.
 */
class AlertEventTest {

    private ProcessInfo sampleProcess() {
        return new ProcessInfo(1, "malware.exe", "C:\\malware.exe", 95.0, 800, -1, Instant.now());
    }

    // ── Constructor – happy path ───────────────────────────────────────────────

    @Test
    void constructor_validInputs_allFieldsCorrect() {
        ProcessInfo proc = sampleProcess();
        AlertEvent alert = new AlertEvent(100, proc, AlertType.HIGH_CPU_USAGE, Severity.HIGH, "CPU too high");

        assertEquals(100,                   alert.getId());
        assertSame(proc,                    alert.getProcess());
        assertEquals(AlertType.HIGH_CPU_USAGE, alert.getType());
        assertEquals(Severity.HIGH,         alert.getSeverity());
        assertEquals("CPU too high",        alert.getMessage());
        assertNotNull(alert.getTimestamp());
        assertFalse(alert.isAcknowledged());
        assertNull(alert.getTriggeringRule());
    }

    @Test
    void constructor_timestampIsRecentlyNow() {
        Instant before = Instant.now();
        AlertEvent alert = new AlertEvent(1, sampleProcess(),
                AlertType.HIGH_CPU_USAGE, Severity.HIGH, "msg");
        Instant after = Instant.now();

        assertFalse(alert.getTimestamp().isBefore(before));
        assertFalse(alert.getTimestamp().isAfter(after));
    }

    // ── Null guarding ─────────────────────────────────────────────────────────

    @Test
    void constructor_nullProcess_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () ->
                new AlertEvent(1, null, AlertType.HIGH_CPU_USAGE, Severity.HIGH, "msg"));
    }

    @Test
    void constructor_nullAlertType_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () ->
                new AlertEvent(1, sampleProcess(), null, Severity.HIGH, "msg"));
    }

    @Test
    void constructor_nullSeverity_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () ->
                new AlertEvent(1, sampleProcess(), AlertType.HIGH_CPU_USAGE, null, "msg"));
    }

    // ── Message fallback ──────────────────────────────────────────────────────

    @Test
    void constructor_blankMessage_generatesDefaultContainingProcessName() {
        ProcessInfo proc = sampleProcess();
        AlertEvent alert = new AlertEvent(1, proc, AlertType.BLACKLISTED_PROCESS, Severity.CRITICAL, "   ");
        assertTrue(alert.getMessage().contains("malware.exe"));
    }

    @Test
    void constructor_nullMessage_generatesDefault() {
        ProcessInfo proc = sampleProcess();
        AlertEvent alert = new AlertEvent(1, proc, AlertType.UNKNOWN_PROCESS, Severity.LOW, null);
        assertFalse(alert.getMessage().isBlank());
        assertTrue(alert.getMessage().contains("malware.exe"));
    }

    @Test
    void constructor_normalMessage_storedAsIs() {
        AlertEvent alert = new AlertEvent(1, sampleProcess(),
                AlertType.HIGH_MEMORY_USAGE, Severity.MEDIUM, "Memory exceeded limit");
        assertEquals("Memory exceeded limit", alert.getMessage());
    }

    // ── setAcknowledged ───────────────────────────────────────────────────────

    @Test
    void setAcknowledged_true_flagUpdated() {
        AlertEvent alert = new AlertEvent(1, sampleProcess(),
                AlertType.HIGH_CPU_USAGE, Severity.HIGH, "msg");
        alert.setAcknowledged(true);
        assertTrue(alert.isAcknowledged());
    }

    @Test
    void setAcknowledged_falseAfterTrue_flagUpdated() {
        AlertEvent alert = new AlertEvent(1, sampleProcess(),
                AlertType.HIGH_CPU_USAGE, Severity.HIGH, "msg");
        alert.setAcknowledged(true);
        alert.setAcknowledged(false);
        assertFalse(alert.isAcknowledged());
    }

    // ── Custom-rule constructor ───────────────────────────────────────────────

    @Test
    void customRuleConstructor_triggeringRuleStored() {
        CustomRule rule = new CustomRule(5, "MyRule", "", true, null,
                "AND", Severity.HIGH, "msg", 0, RuleAction.ALERT_ONLY);
        ProcessInfo proc = sampleProcess();

        AlertEvent alert = new AlertEvent(1, proc,
                AlertType.CUSTOM_RULE_VIOLATION, Severity.HIGH, "custom msg", rule);

        assertSame(rule, alert.getTriggeringRule());
    }

    // ── getDisplayMessage ─────────────────────────────────────────────────────

    @Test
    void getDisplayMessage_noRule_returnsPlainMessage() {
        AlertEvent alert = new AlertEvent(1, sampleProcess(),
                AlertType.HIGH_CPU_USAGE, Severity.HIGH, "plain message");
        assertEquals("plain message", alert.getDisplayMessage());
    }

    @Test
    void getDisplayMessage_withRule_prefixedByRuleName() {
        CustomRule rule = new CustomRule(1, "MyRule", "", true, null,
                "AND", Severity.HIGH, "msg", 0, RuleAction.ALERT_ONLY);
        AlertEvent alert = new AlertEvent(1, sampleProcess(),
                AlertType.CUSTOM_RULE_VIOLATION, Severity.HIGH, "detail msg", rule);

        String display = alert.getDisplayMessage();
        assertTrue(display.startsWith("MyRule"));
        assertTrue(display.contains("detail msg"));
    }

    // ── toString ──────────────────────────────────────────────────────────────

    @Test
    void toString_containsSeverityAndMessage() {
        AlertEvent alert = new AlertEvent(1, sampleProcess(),
                AlertType.HIGH_CPU_USAGE, Severity.HIGH, "some alert");
        String s = alert.toString();
        assertTrue(s.contains("HIGH"));
        assertTrue(s.contains("some alert"));
    }
}
