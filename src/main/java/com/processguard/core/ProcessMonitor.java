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
 * Matches section 2.2 of the Software Design Document (SDD).
 */
public class ProcessMonitor {

    private static final String THREAD_NAME = "ProcessGuard-Monitor";

    private final ProcessScanner scanner;
    private final AppConfig config = AppConfig.getInstance();
    private final HistoryStorage historyStorage;

    private ScheduledExecutorService scheduler;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    private final ConcurrentHashMap<Long, ProcessInfo> lastSnapshot = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<ProcessListener> listeners = new CopyOnWriteArrayList<>();

    public ProcessMonitor(HistoryStorage historyStorage) {
        this.scanner = new ProcessScanner();
        this.historyStorage = historyStorage;
    }

    /**
     * Adds a process listener.
     * @param listener observer to register
     */
    public void addListener(ProcessListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a process listener.
     * @param listener observer to remove
     */
    public void removeListener(ProcessListener listener) {
        listeners.remove(listener);
    }

    /**
     * Starts process monitoring using configured scan interval.
     */
    public void start() {
        if (isRunning.compareAndSet(false, true)) {
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, THREAD_NAME);
                t.setDaemon(false);
                return t;
            });

            int interval = config.getScanIntervalSeconds();
            scheduler.scheduleAtFixedRate(this::performScan, 0, interval, TimeUnit.SECONDS);

            System.out.println("ProcessMonitor started with scan interval: " + interval + " seconds.");
        }
    }

    /**
     * Stops process monitoring safely.
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
     * Performs a single scan cycle of process detection and notification.
     */
    private void performScan() {
        try {
            List<ProcessInfo> currentSnapshot = scanner.scanProcesses();

            List<ProcessInfo> newProcesses = detectNewProcesses(currentSnapshot);
            List<ProcessInfo> exitedProcesses = detectExitedProcesses(currentSnapshot);

            updateLastSnapshot(currentSnapshot);
            notifyListeners(newProcesses, exitedProcesses, currentSnapshot);

            if (historyStorage != null) {
                historyStorage.saveSnapshot(currentSnapshot);
            }

            System.out.println("\n=== SCAN CYCLE ===");
            System.out.println("Processes: " + currentSnapshot.size());

        } catch (Exception e) {
            System.err.println("Error during process scan cycle: " + e.getMessage());
        }
    }

    /**
     * Detects newly started processes.
     * @param current current process list
     * @return list of new processes
     */
    private List<ProcessInfo> detectNewProcesses(List<ProcessInfo> current) {
        Set<Long> previousPids = lastSnapshot.keySet();
        return current.stream()
                .filter(p -> !previousPids.contains(p.getPid()))
                .collect(Collectors.toList());
    }

    /**
     * Detects exited processes.
     * @param current current process list
     * @return list of exited processes
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
     * Updates internal snapshot cache.
     * @param currentSnapshot latest process list
     */
    private void updateLastSnapshot(List<ProcessInfo> currentSnapshot) {
        lastSnapshot.clear();
        for (ProcessInfo p : currentSnapshot) {
            lastSnapshot.put(p.getPid(), p.copy());
        }
    }

    /**
     * Notifies all registered listeners.
     * @param newProcesses newly detected processes
     * @param exitedProcesses removed processes
     * @param currentSnapshot full snapshot
     */
    private void notifyListeners(List<ProcessInfo> newProcesses,
                                 List<ProcessInfo> exitedProcesses,
                                 List<ProcessInfo> currentSnapshot) {

        for (ProcessListener listener : listeners) {
            try {
                if (!newProcesses.isEmpty()) listener.onNewProcesses(newProcesses);
                if (!exitedProcesses.isEmpty()) listener.onExitedProcesses(exitedProcesses);
                listener.onSnapshotUpdate(currentSnapshot);
            } catch (Exception e) {
                System.err.println("Error notifying listener " +
                        listener.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Returns current snapshot of processes.
     * @return list of processes
     */
    public List<ProcessInfo> getCurrentProcesses() {
        return new ArrayList<>(lastSnapshot.values());
    }

    /**
     * Returns whether monitoring is active.
     * @return true if running
     */
    public boolean isRunning() {
        return isRunning.get();
    }

    /**
     * Triggers an immediate scan cycle.
     */
    public void scanNow() {
        performScan();
    }

    /**
     * Returns number of processes in last snapshot.
     * @return process count
     */
    public int getProcessCount() {
        return lastSnapshot.size();
    }
}