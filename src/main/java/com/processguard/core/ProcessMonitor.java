package com.processguard.core;

import com.processguard.listeners.ProcessListener;
import com.processguard.models.ProcessInfo;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Orchestrates periodic process scanning, detects process changes (new/exited),
 * classifies processes, and notifies registered observers using the Observer pattern.
 *
 * Matches section 2.2 of the Software Design Document (SDD).
 */
public class ProcessMonitor {

    private final ProcessScanner scanner;
    private final AppConfig config = AppConfig.getInstance();
    private final HistoryStorage historyStorage;

    private ScheduledExecutorService scheduler;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    // Maintains the last known snapshot (thread-safe)
    private final ConcurrentHashMap<Long, ProcessInfo> lastSnapshot = new ConcurrentHashMap<>();

    // Observer list (CopyOnWriteArrayList for thread-safety)
    private final CopyOnWriteArrayList<ProcessListener> listeners = new CopyOnWriteArrayList<>();

    public ProcessMonitor(HistoryStorage historyStorage) {
        this.scanner = new ProcessScanner();
        this.historyStorage = historyStorage;
    }

    /**
     * Adds a listener (Observer pattern).
     * Supports MainDashboard, AlertEngine, CustomRuleEngine, HistoryStorage, etc.
     */
    public void addListener(ProcessListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a listener.
     */
    public void removeListener(ProcessListener listener) {
        listeners.remove(listener);
    }

    /**
     * Starts the monitoring process with the configured scan interval.
     */
    public void start() {
        if (isRunning.compareAndSet(false, true)) {
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "ProcessGuard-Monitor");
                t.setDaemon(false);
                return t;
            });

            int interval = config.getScanIntervalSeconds();

            scheduler.scheduleAtFixedRate(this::performScan, 0, interval, TimeUnit.SECONDS);

            System.out.println("ProcessMonitor started with scan interval: " + interval + " seconds.");
        }
    }

    /**
     * Stops the monitoring process.
     */
    public void stop() {
        if (isRunning.compareAndSet(true, false)) {
            if (scheduler != null) {
                scheduler.shutdown();
                try {
                    if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                        scheduler.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    scheduler.shutdownNow();
                }
            }
            System.out.println("ProcessMonitor stopped.");
        }
    }

    /**
     * Performs a single scan cycle: scan → diff → notify observers → persist.
     */
    private void performScan() {
        try {
            List<ProcessInfo> currentSnapshot = scanner.scanProcesses();

            // Detect changes
            List<ProcessInfo> newProcesses = detectNewProcesses(currentSnapshot);
            List<ProcessInfo> exitedProcesses = detectExitedProcesses(currentSnapshot);

            // Update last snapshot
            updateLastSnapshot(currentSnapshot);

            // Notify all registered listeners (Observer pattern)
            notifyListeners(newProcesses, exitedProcesses, currentSnapshot);

            // Persist the current snapshot via HistoryStorage
            if (historyStorage != null) {
                historyStorage.saveSnapshot(currentSnapshot);
            }

            // System print tests in lieu of UI to be removed
            System.out.println("\n=== SCAN CYCLE ===");
            System.out.println("Processes: " + currentSnapshot.size());

            List<ProcessInfo> sorted = currentSnapshot.stream()
                    .sorted((a, b) -> Long.compare(b.getMemoryUsageMB(), a.getMemoryUsageMB()))
                    .limit(20)
                    .toList();

            for (ProcessInfo p : sorted) {
                System.out.println(p);
            }

        } catch (Exception e) {
            System.err.println("Error during process scan cycle: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Detects newly started processes by comparing current PIDs with lastSnapshot.
     */
    private List<ProcessInfo> detectNewProcesses(List<ProcessInfo> current) {
        Set<Long> previousPids = lastSnapshot.keySet();
        return current.stream()
                .filter(p -> !previousPids.contains(p.getPid()))
                .collect(Collectors.toList());
    }

    /**
     * Detects processes that have exited since the last scan.
     */
    private List<ProcessInfo> detectExitedProcesses(List<ProcessInfo> current) {
        Set<Long> currentPids = current.stream()
                .map(ProcessInfo::getPid)
                .collect(Collectors.toSet());

        return lastSnapshot.values().stream()
                .filter(p -> !currentPids.contains(p.getPid()))
                .collect(Collectors.toList());
    }

    /**
     * Updates the internal lastSnapshot map with the latest process information.
     */
    private void updateLastSnapshot(List<ProcessInfo> currentSnapshot) {
        lastSnapshot.clear();
        for (ProcessInfo p : currentSnapshot) {
            // Store a defensive copy
            lastSnapshot.put(p.getPid(), p.copy());
        }
    }

    /**
     * Notifies all registered ProcessListeners using the Observer pattern.
     */
    private void notifyListeners(List<ProcessInfo> newProcesses,
                                 List<ProcessInfo> exitedProcesses,
                                 List<ProcessInfo> currentSnapshot) {

        for (ProcessListener listener : listeners) {
            try {
                if (!newProcesses.isEmpty()) {
                    listener.onNewProcesses(newProcesses);
                }
                if (!exitedProcesses.isEmpty()) {
                    listener.onExitedProcesses(exitedProcesses);
                }
                listener.onSnapshotUpdate(currentSnapshot);
            } catch (Exception e) {
                System.err.println("Error notifying listener " + listener.getClass().getSimpleName()
                        + ": " + e.getMessage());
            }
        }
    }

    /**
     * Returns the current list of processes from the last snapshot.
     */
    public List<ProcessInfo> getCurrentProcesses() {
        return new ArrayList<>(lastSnapshot.values());
    }

    /**
     * Returns whether monitoring is currently active.
     */
    public boolean isRunning() {
        return isRunning.get();
    }

    /**
     * Returns the number of processes in the last snapshot.
     */
    public int getProcessCount() {
        return lastSnapshot.size();
    }
}