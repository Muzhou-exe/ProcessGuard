package com.processguard.core;

import com.processguard.models.AlertEvent;
import com.processguard.models.AlertType;
import com.processguard.models.ProcessInfo;
import com.processguard.models.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link HistoryStorage}.
 */
class HistoryStorageTest {

    private HistoryStorage storage;

    @BeforeEach
    void setUp() {
        storage = new HistoryStorage();
    }

    @Test
    void saveSnapshot_storesProcesses() {
        ProcessInfo p1 = new ProcessInfo(1, "a", "", 10, 100, -1, Instant.now());
        ProcessInfo p2 = new ProcessInfo(2, "b", "", 20, 200, -1, Instant.now());

        storage.saveSnapshot(List.of(p1, p2));

        List<ProcessInfo> snapshots = storage.getRecentSnapshots();
        assertEquals(2, snapshots.size());
    }

    @Test
    void saveSnapshot_replacesOldSnapshot() {
        ProcessInfo p1 = new ProcessInfo(1, "a", "", 10, 100, -1, Instant.now());
        storage.saveSnapshot(List.of(p1));

        ProcessInfo p2 = new ProcessInfo(2, "b", "", 20, 200, -1, Instant.now());
        storage.saveSnapshot(List.of(p2));

        List<ProcessInfo> snapshots = storage.getRecentSnapshots();
        assertEquals(1, snapshots.size());
        assertEquals(2, snapshots.get(0).getPid());
    }

    @Test
    void saveAlert_storesAlert() {
        ProcessInfo proc = new ProcessInfo(1, "test", "", 0, 0, -1, Instant.now());
        AlertEvent alert = new AlertEvent(1, proc, AlertType.HIGH_CPU_USAGE, Severity.HIGH, "test alert");

        storage.saveAlert(alert);

        List<AlertEvent> alerts = storage.getRecentAlerts();
        assertEquals(1, alerts.size());
        assertEquals("test alert", alerts.get(0).getMessage());
    }

    @Test
    void saveAlert_capsAt1000() {
        ProcessInfo proc = new ProcessInfo(1, "test", "", 0, 0, -1, Instant.now());

        for (int i = 0; i < 1005; i++) {
            AlertEvent alert = new AlertEvent(i, proc, AlertType.HIGH_CPU_USAGE, Severity.HIGH, "alert " + i);
            storage.saveAlert(alert);
        }

        assertEquals(1000, storage.getRecentAlerts().size());
    }

    @Test
    void getRecentSnapshots_emptyByDefault() {
        assertTrue(storage.getRecentSnapshots().isEmpty());
    }

    @Test
    void getRecentAlerts_emptyByDefault() {
        assertTrue(storage.getRecentAlerts().isEmpty());
    }
}
