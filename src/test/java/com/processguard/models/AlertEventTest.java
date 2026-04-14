package com.processguard.models;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link AlertEvent}.
 */
class AlertEventTest {

    private ProcessInfo sampleProcess() {
        return new ProcessInfo(1, "malware.exe", "C:\\malware.exe", 95.0, 800, -1, Instant.now());
    }

    @Test
    void constructor_validInputs_fieldsSetCorrectly() {
        ProcessInfo proc = sampleProcess();
        AlertEvent alert = new AlertEvent(100, proc, AlertType.HIGH_CPU_USAGE, Severity.HIGH, "CPU too high");

        assertEquals(100, alert.getId());
        assertEquals(proc, alert.getProcess());
        assertEquals(AlertType.HIGH_CPU_USAGE, alert.getType());
        assertEquals(Severity.HIGH, alert.getSeverity());
        assertEquals("CPU too high", alert.getMessage());
        assertNotNull(alert.getTimestamp());
        assertFalse(alert.isAcknowledged());
    }

    @Test
    void constructor_nullProcess_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> new AlertEvent(1, null, AlertType.HIGH_CPU_USAGE, Severity.HIGH, "msg"));
    }

    @Test
    void constructor_blankMessage_generatesDefault() {
        ProcessInfo proc = sampleProcess();
        AlertEvent alert = new AlertEvent(1, proc, AlertType.BLACKLISTED_PROCESS, Severity.CRITICAL, "  ");

        assertTrue(alert.getMessage().contains("malware.exe"));
    }

    @Test
    void setAcknowledged_updatesFlag() {
        AlertEvent alert = new AlertEvent(1, sampleProcess(), AlertType.HIGH_CPU_USAGE, Severity.HIGH, "msg");

        alert.setAcknowledged(true);
        assertTrue(alert.isAcknowledged());
    }

    @Test
    void equals_sameId_returnsTrue() {
        ProcessInfo proc = sampleProcess();
        AlertEvent a = new AlertEvent(42, proc, AlertType.HIGH_CPU_USAGE, Severity.HIGH, "a");
        AlertEvent b = new AlertEvent(42, proc, AlertType.HIGH_MEMORY_USAGE, Severity.LOW, "b");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
