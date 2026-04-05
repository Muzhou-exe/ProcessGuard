package com.processguard.core;

import com.processguard.listeners.AlertListener;
import com.processguard.listeners.ProcessListener;
import com.processguard.models.AlertEvent;
import com.processguard.models.AlertType;
import com.processguard.models.ProcessInfo;
import com.processguard.models.Severity;
import com.processguard.models.Status;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Evaluates processes against built-in rules and fires alerts.
 * Implements ProcessListener (Observer of ProcessMonitor).
 * Matches section 2.3 of the SDD.
 */
public class AlertEngine implements ProcessListener {

    private final HistoryStorage historyStorage;
    private final CopyOnWriteArrayList<AlertListener> alertListeners = new CopyOnWriteArrayList<>();

    // Bounded alert history (max 1000 entries as per SDD)
    private final CopyOnWriteArrayList<AlertEvent> alertHistory = new CopyOnWriteArrayList<>();

    private final Set<String> activeAlerts = ConcurrentHashMap.newKeySet();

    public AlertEngine(HistoryStorage historyStorage) {
        this.historyStorage = historyStorage;
    }

    public void addAlertListener(AlertListener listener) {
        if (listener != null) {
            alertListeners.add(listener);
        }
    }

    @Override
    public void onNewProcesses(List<ProcessInfo> newProcesses) {
        for (ProcessInfo process : newProcesses) {
            checkBuiltInRules(process);
        }
    }

    @Override
    public void onExitedProcesses(List<ProcessInfo> exitedProcesses) {
        // No alerts on process exit (per minimal SDD design)
    }

    @Override
    public void onSnapshotUpdate(List<ProcessInfo> currentSnapshot) {
        for (ProcessInfo process : currentSnapshot) {
            checkBuiltInRules(process);
        }
    }

    /**
     * Built-in rules as specified in SDD section 2.3:
     * - blacklist check (CRITICAL)
     * - unknown process (LOW)
     * - CPU threshold (HIGH)
     * - memory threshold (MEDIUM)
     */
    private void checkBuiltInRules(ProcessInfo process) {
        AlertEvent alert = null;
        String alertKey = null;

        if (process.getStatus() == Status.BLOCKED) {
            alert = new AlertEvent(generateAlertId(), process,
                    AlertType.BLACKLISTED_PROCESS,
                    Severity.CRITICAL,
                    "Blacklisted process detected: " + process.getName());

            alertKey = process.getPid() + "_BLACKLISTED_PROCESS";
        }
        else if (process.getCpuUsage() > AppConfig.getInstance().getCpuThreshold()) {
            alert = new AlertEvent(generateAlertId(), process,
                    AlertType.HIGH_CPU_USAGE,
                    Severity.HIGH,
                    "High CPU usage detected: " + process.getName());

            alertKey = process.getPid() + "_HIGH_CPU_USAGE";
        }
        else if (process.getMemoryUsageMB() > AppConfig.getInstance().getMemoryThreshold()) {
            alert = new AlertEvent(generateAlertId(), process,
                    AlertType.HIGH_MEMORY_USAGE,
                    Severity.MEDIUM,
                    "High memory usage detected: " + process.getName());

            alertKey = process.getPid() + "_HIGH_MEMORY_USAGE";
        }
        else if (process.getStatus() == Status.SUSPICIOUS) {
            alert = new AlertEvent(generateAlertId(), process,
                    AlertType.UNKNOWN_PROCESS,
                    Severity.LOW,
                    "Unknown/suspicious process: " + process.getName());

            alertKey = process.getPid() + "_UNKNOWN_PROCESS";
        }

        if (alert != null &&
                alertKey != null &&
                !activeAlerts.contains(alertKey) &&
                alertHistory.size() < 1000) {

            activeAlerts.add(alertKey);
            alertHistory.add(alert);
            notifyAlertListeners(alert);

            if (historyStorage != null) {
                historyStorage.saveAlert(alert);
            }

            System.out.println("[ALERT] " + alert);
        }

        // clear inactive alerts only when process is NORMAL
        if (alert == null) {
            activeAlerts.remove(process.getPid() + "_HIGH_CPU_USAGE");
            activeAlerts.remove(process.getPid() + "_HIGH_MEMORY_USAGE");
            activeAlerts.remove(process.getPid() + "_UNKNOWN_PROCESS");
            activeAlerts.remove(process.getPid() + "_BLACKLISTED_PROCESS");
        }
    }

    private void notifyAlertListeners(AlertEvent alert) {
        for (AlertListener listener : alertListeners) {
            try {
                listener.onAlert(alert);
            } catch (Exception ignored) {
                // Silent fail as per minimal design
            }
        }
    }

    private long generateAlertId() {
        return System.currentTimeMillis();
    }

    public List<AlertEvent> getAlertHistory() {
        return new ArrayList<>(alertHistory);
    }
}