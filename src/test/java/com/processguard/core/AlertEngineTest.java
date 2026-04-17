package com.processguard.core;

import com.processguard.models.AlertEvent;
import com.processguard.models.AlertType;
import com.processguard.models.ProcessInfo;
import com.processguard.models.Severity;
import com.processguard.models.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AlertEngine}.
 */
class AlertEngineTest {

    private AlertEngine alertEngine;
    private List<AlertEvent> receivedAlerts;

    @BeforeEach
    void setUp() {
        HistoryStorage storage = new HistoryStorage();
        alertEngine = new AlertEngine(storage);
        receivedAlerts = new ArrayList<>();
        alertEngine.addAlertListener(receivedAlerts::add);
    }

    private ProcessInfo process(long pid, String name, double cpu, long memMB, Status status) {
        ProcessInfo p = new ProcessInfo(pid, name, "", cpu, memMB, -1, Instant.now());
        p.setStatus(status);
        return p;
    }

    // ── onSnapshotUpdate – four built-in rules ────────────────────────────────

    @Test
    void snapshot_blockedProcess_firesCriticalBlacklistAlert() {
        alertEngine.onSnapshotUpdate(List.of(
                process(1, "malware.exe", 10, 100, Status.BLOCKED)));

        assertEquals(1, receivedAlerts.size());
        assertEquals(AlertType.BLACKLISTED_PROCESS, receivedAlerts.get(0).getType());
        assertEquals(Severity.CRITICAL,             receivedAlerts.get(0).getSeverity());
    }

    @Test
    void snapshot_highCpu_firesHighCpuAlert() {
        double threshold = AppConfig.getInstance().getCpuThreshold();
        alertEngine.onSnapshotUpdate(List.of(
                process(2, "busy.exe", threshold + 1, 100, Status.NORMAL)));

        assertEquals(1, receivedAlerts.size());
        assertEquals(AlertType.HIGH_CPU_USAGE, receivedAlerts.get(0).getType());
        assertEquals(Severity.HIGH,            receivedAlerts.get(0).getSeverity());
    }

    @Test
    void snapshot_cpuExactlyAtThreshold_noAlert() {
        double threshold = AppConfig.getInstance().getCpuThreshold();
        alertEngine.onSnapshotUpdate(List.of(
                process(2, "busy.exe", threshold, 100, Status.NORMAL)));

        assertTrue(receivedAlerts.isEmpty());
    }

    @Test
    void snapshot_highMemory_firesMediumMemoryAlert() {
        double memThreshold = AppConfig.getInstance().getMemoryThreshold();
        alertEngine.onSnapshotUpdate(List.of(
                process(3, "hog.exe", 0, (long) memThreshold + 1, Status.NORMAL)));

        assertEquals(1, receivedAlerts.size());
        assertEquals(AlertType.HIGH_MEMORY_USAGE, receivedAlerts.get(0).getType());
        assertEquals(Severity.MEDIUM,             receivedAlerts.get(0).getSeverity());
    }

    @Test
    void snapshot_suspiciousProcess_firesLowUnknownAlert() {
        alertEngine.onSnapshotUpdate(List.of(
                process(4, "unknown.exe", 0, 0, Status.SUSPICIOUS)));

        assertEquals(1, receivedAlerts.size());
        assertEquals(AlertType.UNKNOWN_PROCESS, receivedAlerts.get(0).getType());
        assertEquals(Severity.LOW,              receivedAlerts.get(0).getSeverity());
    }

    @Test
    void snapshot_normalProcess_noAlert() {
        alertEngine.onSnapshotUpdate(List.of(
                process(5, "safe.exe", 10, 100, Status.NORMAL)));

        assertTrue(receivedAlerts.isEmpty());
    }

    @Test
    void snapshot_whitelistedProcess_noAlert() {
        alertEngine.onSnapshotUpdate(List.of(
                process(6, "trusted.exe", 0, 0, Status.WHITELISTED)));

        assertTrue(receivedAlerts.isEmpty());
    }

    // ── Rule priority: BLOCKED > high CPU ─────────────────────────────────────

    @Test
    void snapshot_blockedAndHighCpu_onlyBlacklistAlertFires() {
        double threshold = AppConfig.getInstance().getCpuThreshold();
        alertEngine.onSnapshotUpdate(List.of(
                process(10, "bad.exe", threshold + 50, 100, Status.BLOCKED)));

        assertEquals(1, receivedAlerts.size());
        assertEquals(AlertType.BLACKLISTED_PROCESS, receivedAlerts.get(0).getType());
    }

    // ── Deduplication ─────────────────────────────────────────────────────────

    @Test
    void snapshot_sameAlertTwice_firesOnlyOnce() {
        ProcessInfo blocked = process(20, "malware.exe", 10, 100, Status.BLOCKED);
        alertEngine.onSnapshotUpdate(List.of(blocked));
        alertEngine.onSnapshotUpdate(List.of(blocked));

        assertEquals(1, receivedAlerts.size());
    }

    @Test
    void snapshot_alertClearedByNormal_thenRefiredOnRecurrence() {
        ProcessInfo suspicious = process(30, "app.exe", 0, 0, Status.SUSPICIOUS);
        ProcessInfo normal     = process(30, "app.exe", 0, 0, Status.NORMAL);

        alertEngine.onSnapshotUpdate(List.of(suspicious)); // fires alert
        alertEngine.onSnapshotUpdate(List.of(normal));     // clears the active alert key
        alertEngine.onSnapshotUpdate(List.of(suspicious)); // should fire again

        assertEquals(2, receivedAlerts.size());
    }

    // ── Multiple processes in one snapshot ───────────────────────────────────

    @Test
    void snapshot_multipleAlertingProcesses_eachFiresOnce() {
        double cpuThreshold = AppConfig.getInstance().getCpuThreshold();
        double memThreshold = AppConfig.getInstance().getMemoryThreshold();

        alertEngine.onSnapshotUpdate(List.of(
                process(1, "blocked.exe",  0,                  0,                    Status.BLOCKED),
                process(2, "cpuhog.exe",   cpuThreshold + 10,  0,                    Status.NORMAL),
                process(3, "memhog.exe",   0,                  (long)memThreshold+1, Status.NORMAL),
                process(4, "unknown.exe",  0,                  0,                    Status.SUSPICIOUS),
                process(5, "safe.exe",     5,                  50,                   Status.NORMAL)
        ));

        assertEquals(4, receivedAlerts.size());
    }

    // ── onNewProcesses ────────────────────────────────────────────────────────

    @Test
    void newProcesses_blockedProcess_firesAlert() {
        alertEngine.onNewProcesses(List.of(
                process(40, "virus.exe", 0, 0, Status.BLOCKED)));

        assertEquals(1, receivedAlerts.size());
        assertEquals(AlertType.BLACKLISTED_PROCESS, receivedAlerts.get(0).getType());
    }

    @Test
    void newProcesses_normalProcess_noAlert() {
        alertEngine.onNewProcesses(List.of(
                process(41, "good.exe", 5, 50, Status.NORMAL)));

        assertTrue(receivedAlerts.isEmpty());
    }

    // ── onExitedProcesses ─────────────────────────────────────────────────────

    @Test
    void exitedProcesses_neverFiresAnyAlert() {
        alertEngine.onExitedProcesses(List.of(
                process(50, "exited.exe", 99, 9999, Status.BLOCKED)));

        assertTrue(receivedAlerts.isEmpty());
    }

    // ── getAlertHistory ───────────────────────────────────────────────────────

    @Test
    void getAlertHistory_reflectsAllFiredAlerts() {
        alertEngine.onSnapshotUpdate(List.of(
                process(60, "bad.exe", 0, 0, Status.BLOCKED)));

        assertEquals(1, alertEngine.getAlertHistory().size());
    }

    @Test
    void getAlertHistory_returnsDefensiveCopy() {
        alertEngine.onSnapshotUpdate(List.of(
                process(61, "bad.exe", 0, 0, Status.BLOCKED)));

        List<AlertEvent> history = alertEngine.getAlertHistory();
        history.clear();

        assertEquals(1, alertEngine.getAlertHistory().size());
    }

    // ── addAlertListener null guard ───────────────────────────────────────────

    @Test
    void addAlertListener_null_doesNotThrow() {
        assertDoesNotThrow(() -> alertEngine.addAlertListener(null));
    }

    @Test
    void addAlertListener_null_doesNotPreventOtherListeners() {
        alertEngine.addAlertListener(null);
        alertEngine.onSnapshotUpdate(List.of(
                process(70, "bad.exe", 0, 0, Status.BLOCKED)));

        assertEquals(1, receivedAlerts.size());
    }
}
