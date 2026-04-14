package com.processguard.core;

import com.processguard.listeners.AlertListener;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    /**
     * Creates a process with the given status and resource usage.
     */
    private ProcessInfo createProcess(long pid, String name, double cpu, long memMB, Status status) {
        ProcessInfo p = new ProcessInfo(pid, name, "", cpu, memMB, -1, Instant.now());
        p.setStatus(status);
        return p;
    }

    @Test
    void onSnapshotUpdate_blockedProcess_firesAlert() {
        ProcessInfo blocked = createProcess(1, "malware.exe", 10, 100, Status.BLOCKED);
        alertEngine.onSnapshotUpdate(List.of(blocked));

        assertEquals(1, receivedAlerts.size());
        assertEquals(AlertType.BLACKLISTED_PROCESS, receivedAlerts.get(0).getType());
        assertEquals(Severity.CRITICAL, receivedAlerts.get(0).getSeverity());
    }

    @Test
    void onSnapshotUpdate_highCpu_firesAlert() {
        double threshold = AppConfig.getInstance().getCpuThreshold();
        ProcessInfo highCpu = createProcess(2, "busy.exe", threshold + 1, 100, Status.NORMAL);
        alertEngine.onSnapshotUpdate(List.of(highCpu));

        assertEquals(1, receivedAlerts.size());
        assertEquals(AlertType.HIGH_CPU_USAGE, receivedAlerts.get(0).getType());
        assertEquals(Severity.HIGH, receivedAlerts.get(0).getSeverity());
    }

    @Test
    void onSnapshotUpdate_highMemory_firesAlert() {
        double memThreshold = AppConfig.getInstance().getMemoryThreshold();
        ProcessInfo highMem = createProcess(3, "hog.exe", 0, (long) memThreshold + 1, Status.NORMAL);
        alertEngine.onSnapshotUpdate(List.of(highMem));

        assertEquals(1, receivedAlerts.size());
        assertEquals(AlertType.HIGH_MEMORY_USAGE, receivedAlerts.get(0).getType());
        assertEquals(Severity.MEDIUM, receivedAlerts.get(0).getSeverity());
    }

    @Test
    void onSnapshotUpdate_suspiciousProcess_firesAlert() {
        ProcessInfo suspicious = createProcess(4, "unknown.exe", 0, 0, Status.SUSPICIOUS);
        alertEngine.onSnapshotUpdate(List.of(suspicious));

        assertEquals(1, receivedAlerts.size());
        assertEquals(AlertType.UNKNOWN_PROCESS, receivedAlerts.get(0).getType());
        assertEquals(Severity.LOW, receivedAlerts.get(0).getSeverity());
    }

    @Test
    void onSnapshotUpdate_normalProcess_noAlert() {
        ProcessInfo normal = createProcess(5, "safe.exe", 10, 100, Status.NORMAL);
        alertEngine.onSnapshotUpdate(List.of(normal));

        assertTrue(receivedAlerts.isEmpty());
    }

    @Test
    void onSnapshotUpdate_duplicateAlert_notFiredTwice() {
        ProcessInfo blocked = createProcess(10, "malware.exe", 10, 100, Status.BLOCKED);

        alertEngine.onSnapshotUpdate(List.of(blocked));
        alertEngine.onSnapshotUpdate(List.of(blocked));

        assertEquals(1, receivedAlerts.size());
    }

    @Test
    void onSnapshotUpdate_alertClearedThenRefired() {
        ProcessInfo process = createProcess(20, "app.exe", 0, 0, Status.SUSPICIOUS);

        alertEngine.onSnapshotUpdate(List.of(process));
        assertEquals(1, receivedAlerts.size());

        // Process becomes normal — clears the alert
        ProcessInfo normal = createProcess(20, "app.exe", 0, 0, Status.NORMAL);
        alertEngine.onSnapshotUpdate(List.of(normal));

        // Process becomes suspicious again — alert should fire again
        alertEngine.onSnapshotUpdate(List.of(process));
        assertEquals(2, receivedAlerts.size());
    }

    @Test
    void onNewProcesses_blockedProcess_firesAlert() {
        ProcessInfo blocked = createProcess(30, "virus.exe", 0, 0, Status.BLOCKED);
        alertEngine.onNewProcesses(List.of(blocked));

        assertEquals(1, receivedAlerts.size());
        assertEquals(AlertType.BLACKLISTED_PROCESS, receivedAlerts.get(0).getType());
    }

    @Test
    void getAlertHistory_returnsAllFiredAlerts() {
        ProcessInfo blocked = createProcess(40, "bad.exe", 0, 0, Status.BLOCKED);
        alertEngine.onSnapshotUpdate(List.of(blocked));

        List<AlertEvent> history = alertEngine.getAlertHistory();
        assertEquals(1, history.size());
    }
}
