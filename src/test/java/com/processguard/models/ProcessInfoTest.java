package com.processguard.models;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ProcessInfo}.
 */
class ProcessInfoTest {

    // ── Constructor – happy path ───────────────────────────────────────────────

    @Test
    void constructor_validInputs_allFieldsCorrect() {
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        ProcessInfo p = new ProcessInfo(123, "java", "/usr/bin/java", 25.5, 512, 1, start);

        assertEquals(123,              p.getPid());
        assertEquals("java",           p.getName());
        assertEquals("/usr/bin/java",  p.getExecutablePath());
        assertEquals(25.5,             p.getCpuUsage(), 0.001);
        assertEquals(512,              p.getMemoryUsageMB());
        assertEquals(1,                p.getParentPid());
        assertEquals(start,            p.getStartTime());
        assertEquals(Status.NORMAL,    p.getStatus());
        assertNotNull(p.getCapturedAt());
    }

    // ── Constructor – null / negative handling ─────────────────────────────────

    @Test
    void constructor_nullName_defaultsToUnknown() {
        ProcessInfo p = new ProcessInfo(1, null, "/path", 0, 0, -1, Instant.now());
        assertEquals("unknown", p.getName());
    }

    @Test
    void constructor_nullExecutablePath_defaultsToEmpty() {
        ProcessInfo p = new ProcessInfo(1, "test", null, 0, 0, -1, Instant.now());
        assertEquals("", p.getExecutablePath());
    }

    @Test
    void constructor_nullStartTime_defaultsToEpoch() {
        ProcessInfo p = new ProcessInfo(1, "test", "", 0, 0, -1, null);
        assertEquals(Instant.EPOCH, p.getStartTime());
    }

    @Test
    void constructor_negativeCpu_clampedToZero() {
        ProcessInfo p = new ProcessInfo(1, "test", "", -50.0, 100, -1, Instant.now());
        assertEquals(0.0, p.getCpuUsage(), 0.001);
    }

    @Test
    void constructor_negativeMemory_clampedToZero() {
        ProcessInfo p = new ProcessInfo(1, "test", "", 10.0, -200, -1, Instant.now());
        assertEquals(0, p.getMemoryUsageMB());
    }

    @Test
    void constructor_zeroCpuAndMemory_storedAsZero() {
        ProcessInfo p = new ProcessInfo(1, "test", "", 0.0, 0, -1, Instant.now());
        assertEquals(0.0, p.getCpuUsage(), 0.001);
        assertEquals(0, p.getMemoryUsageMB());
    }

    @Test
    void constructor_defaultStatus_isNormal() {
        ProcessInfo p = new ProcessInfo(1, "test", "", 0, 0, -1, Instant.now());
        assertEquals(Status.NORMAL, p.getStatus());
    }

    // ── setStatus ─────────────────────────────────────────────────────────────

    @Test
    void setStatus_toBlocked_updatesCorrectly() {
        ProcessInfo p = new ProcessInfo(1, "test", "", 0, 0, -1, Instant.now());
        p.setStatus(Status.BLOCKED);
        assertEquals(Status.BLOCKED, p.getStatus());
    }

    @Test
    void setStatus_toSuspicious_updatesCorrectly() {
        ProcessInfo p = new ProcessInfo(1, "test", "", 0, 0, -1, Instant.now());
        p.setStatus(Status.SUSPICIOUS);
        assertEquals(Status.SUSPICIOUS, p.getStatus());
    }

    @Test
    void setStatus_toWhitelisted_updatesCorrectly() {
        ProcessInfo p = new ProcessInfo(1, "test", "", 0, 0, -1, Instant.now());
        p.setStatus(Status.WHITELISTED);
        assertEquals(Status.WHITELISTED, p.getStatus());
    }

    @Test
    void setStatus_nullFallsBackToNormal() {
        ProcessInfo p = new ProcessInfo(1, "test", "", 0, 0, -1, Instant.now());
        p.setStatus(Status.BLOCKED);
        p.setStatus(null);
        assertEquals(Status.NORMAL, p.getStatus());
    }

    // ── equals / hashCode ─────────────────────────────────────────────────────

    @Test
    void equals_samePid_returnsTrue() {
        ProcessInfo a = new ProcessInfo(42, "a", "",    0,    0, -1, Instant.now());
        ProcessInfo b = new ProcessInfo(42, "b", "/x", 99.0, 512, 0, Instant.EPOCH);
        assertEquals(a, b);
    }

    @Test
    void equals_differentPid_returnsFalse() {
        ProcessInfo a = new ProcessInfo(1, "test", "", 0, 0, -1, Instant.now());
        ProcessInfo b = new ProcessInfo(2, "test", "", 0, 0, -1, Instant.now());
        assertNotEquals(a, b);
    }

    @Test
    void hashCode_samePid_sameHash() {
        ProcessInfo a = new ProcessInfo(99, "a", "", 0, 0, -1, Instant.now());
        ProcessInfo b = new ProcessInfo(99, "b", "", 0, 0, -1, Instant.now());
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_null_returnsFalse() {
        ProcessInfo p = new ProcessInfo(1, "test", "", 0, 0, -1, Instant.now());
        assertNotEquals(null, p);
    }

    @Test
    void equals_differentType_returnsFalse() {
        ProcessInfo p = new ProcessInfo(1, "test", "", 0, 0, -1, Instant.now());
        assertNotEquals("not a ProcessInfo", p);
    }

    // ── copy ──────────────────────────────────────────────────────────────────

    @Test
    void copy_isDifferentObject() {
        ProcessInfo original = new ProcessInfo(10, "chrome", "/usr/bin/chrome", 50.0, 200, 1, Instant.now());
        assertNotSame(original, original.copy());
    }

    @Test
    void copy_hasSamePidAndName() {
        ProcessInfo original = new ProcessInfo(10, "chrome", "/usr/bin/chrome", 50.0, 200, 1, Instant.now());
        ProcessInfo copy = original.copy();
        assertEquals(original.getPid(),  copy.getPid());
        assertEquals(original.getName(), copy.getName());
    }

    @Test
    void copy_preservesStatus() {
        ProcessInfo original = new ProcessInfo(10, "chrome", "", 50.0, 200, 1, Instant.now());
        original.setStatus(Status.SUSPICIOUS);
        assertEquals(Status.SUSPICIOUS, original.copy().getStatus());
    }

    @Test
    void copy_mutatingCopyDoesNotAffectOriginal() {
        ProcessInfo original = new ProcessInfo(10, "chrome", "", 50.0, 200, 1, Instant.now());
        ProcessInfo copy = original.copy();
        copy.setStatus(Status.BLOCKED);
        assertEquals(Status.NORMAL, original.getStatus());
    }

    // ── toString ──────────────────────────────────────────────────────────────

    @Test
    void toString_containsPidNameCpuMemStatus() {
        ProcessInfo p = new ProcessInfo(99, "firefox", "", 10.0, 300, -1, Instant.now());
        String s = p.toString();
        assertTrue(s.contains("99"));
        assertTrue(s.contains("firefox"));
        assertTrue(s.contains("10.0"));
        assertTrue(s.contains("300"));
        assertTrue(s.contains("NORMAL"));
    }
}