package com.processguard.models;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * Unit tests for {@link ProcessInfo}.
 */
class ProcessInfoTest {

    @Test
    void constructor_validInputs_fieldsSetCorrectly() {
        Instant start = Instant.now();
        ProcessInfo p = new ProcessInfo(123, "java", "/usr/bin/java", 25.5, 512, 1, start);

        assertEquals(123, p.getPid());
        assertEquals("java", p.getName());
        assertEquals("/usr/bin/java", p.getExecutablePath());
        assertEquals(25.5, p.getCpuUsage(), 0.001);
        assertEquals(512, p.getMemoryUsageMB());
        assertEquals(1, p.getParentPid());
        assertEquals(start, p.getStartTime());
        assertEquals(Status.NORMAL, p.getStatus());
        assertNotNull(p.getCapturedAt());
    }

    @Test
    void constructor_nullName_defaultsToUnknown() {
        ProcessInfo p = new ProcessInfo(1, null, null, 0, 0, -1, null);

        assertEquals("unknown", p.getName());
        assertEquals("", p.getExecutablePath());
        assertEquals(Instant.EPOCH, p.getStartTime());
    }

    @Test
    void constructor_negativeCpuAndMemory_clampedToZero() {
        ProcessInfo p = new ProcessInfo(1, "test", "", -10.0, -100, -1, Instant.now());

        assertEquals(0.0, p.getCpuUsage(), 0.001);
        assertEquals(0, p.getMemoryUsageMB());
    }

    @Test
    void setStatus_updatesStatus() {
        ProcessInfo p = new ProcessInfo(1, "test", "", 0, 0, -1, Instant.now());

        p.setStatus(Status.BLOCKED);
        assertEquals(Status.BLOCKED, p.getStatus());

        p.setStatus(null);
        assertEquals(Status.NORMAL, p.getStatus());
    }

    @Test
    void equals_samePid_returnsTrue() {
        ProcessInfo a = new ProcessInfo(42, "a", "", 0, 0, -1, Instant.now());
        ProcessInfo b = new ProcessInfo(42, "b", "/other", 99.0, 1024, 0, Instant.EPOCH);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void copy_returnsSeparateObjectWithSameData() {
        ProcessInfo original = new ProcessInfo(10, "chrome", "/usr/bin/chrome", 50.0, 200, 1, Instant.now());
        original.setStatus(Status.SUSPICIOUS);

        ProcessInfo copy = original.copy();

        assertNotSame(original, copy);
        assertEquals(original.getPid(), copy.getPid());
        assertEquals(original.getName(), copy.getName());
        assertEquals(original.getStatus(), copy.getStatus());
    }

    @Test
    void toString_containsPidAndName() {
        ProcessInfo p = new ProcessInfo(99, "firefox", "", 10.0, 300, -1, Instant.now());
        String s = p.toString();

        assertEquals(true, s.contains("99"));
        assertEquals(true, s.contains("firefox"));
    }
}
