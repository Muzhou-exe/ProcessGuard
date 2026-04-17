package com.processguard.core;

import com.processguard.models.AlertEvent;
import com.processguard.models.AlertType;
import com.processguard.models.ProcessInfo;
import com.processguard.models.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link HistoryStorage}.
 */
class HistoryStorageTest {

    private HistoryStorage storage;

    @BeforeEach
    void setUp() {
        storage = new HistoryStorage();
    }

    private ProcessInfo proc(long pid) {
        return new ProcessInfo(pid, "proc" + pid, "", 0, 0, -1, Instant.now());
    }

    private AlertEvent alert(int id, String message) {
        return new AlertEvent(id, proc(1), AlertType.HIGH_CPU_USAGE, Severity.HIGH, message);
    }

    // ── saveSnapshot ──────────────────────────────────────────────────────────

    @Test
    void getRecentSnapshots_emptyByDefault() {
        assertTrue(storage.getRecentSnapshots().isEmpty());
    }

    @Test
    void saveSnapshot_storesAllProcesses() {
        storage.saveSnapshot(List.of(proc(1), proc(2)));
        assertEquals(2, storage.getRecentSnapshots().size());
    }

    @Test
    void saveSnapshot_replacesEntirePreviousSnapshot() {
        storage.saveSnapshot(List.of(proc(1), proc(2)));
        storage.saveSnapshot(List.of(proc(3)));

        List<ProcessInfo> result = storage.getRecentSnapshots();
        assertEquals(1, result.size());
        assertEquals(3, result.get(0).getPid());
    }

    @Test
    void saveSnapshot_emptyList_clearsSnapshot() {
        storage.saveSnapshot(List.of(proc(1)));
        storage.saveSnapshot(List.of());
        assertTrue(storage.getRecentSnapshots().isEmpty());
    }

    @Test
    void getRecentSnapshots_returnsDefensiveCopy() {
        storage.saveSnapshot(List.of(proc(1)));
        List<ProcessInfo> copy = storage.getRecentSnapshots();
        copy.clear();
        assertEquals(1, storage.getRecentSnapshots().size());
    }

    // ── saveAlert ─────────────────────────────────────────────────────────────

    @Test
    void getRecentAlerts_emptyByDefault() {
        assertTrue(storage.getRecentAlerts().isEmpty());
    }

    @Test
    void saveAlert_storesSingleAlert() {
        storage.saveAlert(alert(1, "test alert"));

        List<AlertEvent> alerts = storage.getRecentAlerts();
        assertEquals(1, alerts.size());
        assertEquals("test alert", alerts.get(0).getMessage());
    }

    @Test
    void saveAlert_multipleAlerts_allStored() {
        storage.saveAlert(alert(1, "a"));
        storage.saveAlert(alert(2, "b"));
        storage.saveAlert(alert(3, "c"));
        assertEquals(3, storage.getRecentAlerts().size());
    }

    @Test
    void saveAlert_alertsStoredInOrder() {
        storage.saveAlert(alert(1, "first"));
        storage.saveAlert(alert(2, "second"));

        List<AlertEvent> alerts = storage.getRecentAlerts();
        assertEquals("first",  alerts.get(0).getMessage());
        assertEquals("second", alerts.get(1).getMessage());
    }

    @Test
    void saveAlert_capsAtOneThousand() {
        for (int i = 0; i < 1005; i++) {
            storage.saveAlert(alert(i, "alert " + i));
        }
        assertEquals(1000, storage.getRecentAlerts().size());
    }

    @Test
    void saveAlert_whenAtCapacity_oldestEntryDropped() {
        // fill to capacity
        for (int i = 0; i < 1000; i++) {
            storage.saveAlert(alert(i, "alert " + i));
        }
        // add one more — "alert 0" should be gone
        storage.saveAlert(alert(1000, "newest"));

        List<AlertEvent> alerts = storage.getRecentAlerts();
        assertEquals(1000, alerts.size());
        assertEquals("alert 1", alerts.get(0).getMessage());
        assertEquals("newest",  alerts.get(999).getMessage());
    }

    @Test
    void getRecentAlerts_returnsDefensiveCopy() {
        storage.saveAlert(alert(1, "msg"));
        List<AlertEvent> copy = storage.getRecentAlerts();
        copy.clear();
        assertEquals(1, storage.getRecentAlerts().size());
    }
}
