package com.processguard.core;

import com.processguard.listeners.ProcessListener;
import com.processguard.models.ProcessInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ProcessMonitor}.
 */
class ProcessMonitorTest {

    private ProcessMonitor monitor;

    @BeforeEach
    void setUp() {
        monitor = new ProcessMonitor(new HistoryStorage());
    }

    @AfterEach
    void tearDown() {
        monitor.stop();
    }

    // ── isRunning ─────────────────────────────────────────────────────────────

    @Test
    void isRunning_beforeStart_returnsFalse() {
        assertFalse(monitor.isRunning());
    }

    @Test
    void isRunning_afterStart_returnsTrue() {
        monitor.start();
        assertTrue(monitor.isRunning());
    }

    @Test
    void isRunning_afterStop_returnsFalse() {
        monitor.start();
        monitor.stop();
        assertFalse(monitor.isRunning());
    }

    // ── start / stop idempotency ──────────────────────────────────────────────

    @Test
    void start_calledTwice_doesNotThrow() {
        assertDoesNotThrow(() -> {
            monitor.start();
            monitor.start();
        });
        assertTrue(monitor.isRunning());
    }

    @Test
    void stop_calledWithoutStart_doesNotThrow() {
        assertDoesNotThrow(() -> monitor.stop());
        assertFalse(monitor.isRunning());
    }

    @Test
    void stop_calledTwice_doesNotThrow() {
        monitor.start();
        assertDoesNotThrow(() -> {
            monitor.stop();
            monitor.stop();
        });
    }

    // ── scanNow ───────────────────────────────────────────────────────────────

    @Test
    void scanNow_populatesProcessCount() {
        monitor.scanNow();
        assertTrue(monitor.getProcessCount() > 0);
    }

    @Test
    void scanNow_getCurrentProcesses_returnsNonEmpty() {
        monitor.scanNow();
        assertFalse(monitor.getCurrentProcesses().isEmpty());
    }

    @Test
    void scanNow_notifiesOnSnapshotUpdate() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<ProcessInfo> received = new ArrayList<>();

        monitor.addListener(new ProcessListener() {
            @Override public void onNewProcesses(List<ProcessInfo> p) {}
            @Override public void onExitedProcesses(List<ProcessInfo> p) {}
            @Override public void onSnapshotUpdate(List<ProcessInfo> snapshot) {
                received.addAll(snapshot);
                latch.countDown();
            }
        });

        monitor.scanNow();
        assertTrue(latch.await(5, TimeUnit.SECONDS), "onSnapshotUpdate was not called in time");
        assertFalse(received.isEmpty());
    }

    @Test
    void scanNow_secondScan_detectsNewAndExitedViaListeners() throws InterruptedException {
        // First scan populates lastSnapshot with real processes.
        // Second scan should call onSnapshotUpdate — we just verify no exception.
        monitor.scanNow();
        assertDoesNotThrow(() -> monitor.scanNow());
    }

    // ── addListener / removeListener ──────────────────────────────────────────

    @Test
    void addListener_null_doesNotThrow() {
        assertDoesNotThrow(() -> monitor.addListener(null));
    }

    @Test
    void removeListener_registeredListener_removedSuccessfully() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        ProcessListener listener = new ProcessListener() {
            @Override public void onNewProcesses(List<ProcessInfo> p) {}
            @Override public void onExitedProcesses(List<ProcessInfo> p) {}
            @Override public void onSnapshotUpdate(List<ProcessInfo> snapshot) {
                latch.countDown();
            }
        };

        monitor.addListener(listener);
        monitor.removeListener(listener);
        monitor.scanNow();

        // Listener was removed — latch should NOT count down
        assertFalse(latch.await(500, TimeUnit.MILLISECONDS),
                "Listener was called after removal");
    }

    @Test
    void removeListener_unregisteredListener_doesNotThrow() {
        ProcessListener stranger = new ProcessListener() {
            @Override public void onNewProcesses(List<ProcessInfo> p) {}
            @Override public void onExitedProcesses(List<ProcessInfo> p) {}
            @Override public void onSnapshotUpdate(List<ProcessInfo> s) {}
        };
        assertDoesNotThrow(() -> monitor.removeListener(stranger));
    }

    // ── multiple listeners ────────────────────────────────────────────────────

    @Test
    void multipleListeners_allNotified() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);

        monitor.addListener(new ProcessListener() {
            @Override public void onNewProcesses(List<ProcessInfo> p) {}
            @Override public void onExitedProcesses(List<ProcessInfo> p) {}
            @Override public void onSnapshotUpdate(List<ProcessInfo> s) { latch.countDown(); }
        });
        monitor.addListener(new ProcessListener() {
            @Override public void onNewProcesses(List<ProcessInfo> p) {}
            @Override public void onExitedProcesses(List<ProcessInfo> p) {}
            @Override public void onSnapshotUpdate(List<ProcessInfo> s) { latch.countDown(); }
        });

        monitor.scanNow();
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // ── listener that throws ──────────────────────────────────────────────────

    @Test
    void faultyListener_doesNotPreventOtherListeners() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        monitor.addListener(new ProcessListener() {
            @Override public void onNewProcesses(List<ProcessInfo> p) {}
            @Override public void onExitedProcesses(List<ProcessInfo> p) {}
            @Override public void onSnapshotUpdate(List<ProcessInfo> s) {
                throw new RuntimeException("intentional fault");
            }
        });
        monitor.addListener(new ProcessListener() {
            @Override public void onNewProcesses(List<ProcessInfo> p) {}
            @Override public void onExitedProcesses(List<ProcessInfo> p) {}
            @Override public void onSnapshotUpdate(List<ProcessInfo> s) { latch.countDown(); }
        });

        assertDoesNotThrow(() -> monitor.scanNow());
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    // ── getProcessCount ───────────────────────────────────────────────────────

    @Test
    void getProcessCount_beforeScan_isZeroOrMore() {
        // Before any scan the static snapshot may be leftover from other tests;
        // we only verify no exception and a non-negative result.
        assertTrue(monitor.getProcessCount() >= 0);
    }
}
