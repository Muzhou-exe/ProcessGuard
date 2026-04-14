package com.processguard.core;

import com.processguard.listeners.ProcessListener;
import com.processguard.models.ProcessInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link ProcessMonitor}.
 */
class ProcessMonitorTest {

    private ProcessMonitor monitor;
    private HistoryStorage storage;

    @BeforeEach
    void setUp() {
        storage = new HistoryStorage();
        monitor = new ProcessMonitor(storage);
    }

    @AfterEach
    void tearDown() {
        monitor.stop();
    }

    @Test
    void isRunning_beforeStart_returnsFalse() {
        assertFalse(monitor.isRunning());
    }

    @Test
    void start_setsRunningTrue() {
        monitor.start();
        assertTrue(monitor.isRunning());
    }

    @Test
    void stop_setsRunningFalse() {
        monitor.start();
        monitor.stop();
        assertFalse(monitor.isRunning());
    }

    @Test
    void start_calledTwice_doesNotThrow() {
        monitor.start();
        monitor.start();
        assertTrue(monitor.isRunning());
    }

    @Test
    void stop_calledWithoutStart_doesNotThrow() {
        monitor.stop();
        assertFalse(monitor.isRunning());
    }

    @Test
    void scanNow_populatesCurrentProcesses() {
        monitor.scanNow();
        assertTrue(monitor.getProcessCount() > 0);
    }

    @Test
    void scanNow_notifiesListeners() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<ProcessInfo> received = new ArrayList<>();

        monitor.addListener(new ProcessListener() {
            @Override
            public void onNewProcesses(List<ProcessInfo> newProcesses) {
            }

            @Override
            public void onExitedProcesses(List<ProcessInfo> exitedProcesses) {
            }

            @Override
            public void onSnapshotUpdate(List<ProcessInfo> currentSnapshot) {
                received.addAll(currentSnapshot);
                latch.countDown();
            }
        });

        monitor.scanNow();
        latch.await(5, TimeUnit.SECONDS);

        assertFalse(received.isEmpty());
    }

    @Test
    void getCurrentProcesses_afterScan_returnsNonEmpty() {
        monitor.scanNow();
        List<ProcessInfo> processes = monitor.getCurrentProcesses();

        assertFalse(processes.isEmpty());
    }

    @Test
    void addListener_null_doesNotThrow() {
        monitor.addListener(null);
        assertEquals(0, monitor.getProcessCount());
    }

    @Test
    void removeListener_removesSuccessfully() {
        ProcessListener listener = new ProcessListener() {
            @Override
            public void onNewProcesses(List<ProcessInfo> newProcesses) {
            }

            @Override
            public void onExitedProcesses(List<ProcessInfo> exitedProcesses) {
            }

            @Override
            public void onSnapshotUpdate(List<ProcessInfo> currentSnapshot) {
            }
        };

        monitor.addListener(listener);
        monitor.removeListener(listener);
        // No assertion needed — just ensure no exception
    }
}
