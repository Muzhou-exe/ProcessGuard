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
 * AlertEngine evaluates system processes against predefined rules and generates alerts when abnormal behavior is detected.
 * It implements the ProcessListener interface and acts as the decision-making component in the monitoring pipeline.
 */
public class AlertEngine implements ProcessListener {

    private static final int MAX_ALERT_HISTORY = 1000;

    private final HistoryStorage historyStorage;
    private final CopyOnWriteArrayList<AlertListener> alertListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<AlertEvent> alertHistory = new CopyOnWriteArrayList<>();
    private final Set<String> activeAlerts = ConcurrentHashMap.newKeySet();

    /**
     * Constructs an AlertEngine with a given history storage backend.
     * @param historyStorage storage system used to persist alert history
     */
    public AlertEngine(HistoryStorage historyStorage) {
        this.historyStorage = historyStorage;
    }

    /**
     * Registers a new listener to receive alert notifications.
     * @param listener the alert listener to register
     */
    public void addAlertListener(AlertListener listener) {
        if (listener != null) {
            alertListeners.add(listener);
        }
    }

    /**
     * Triggered when new processes are detected.
     * @param newProcesses list of newly detected processes
     */
    @Override
    public void onNewProcesses(List<ProcessInfo> newProcesses) {
        processAll(newProcesses);
    }

    /**
     * Triggered when processes exit.
     * No alert logic is applied for process termination events.
     * @param exitedProcesses list of exited processes
     */
    @Override
    public void onExitedProcesses(List<ProcessInfo> exitedProcesses) {
        // No alerts on process exit (per SDD)
    }

    /**
     * Triggered when a full process snapshot is updated.
     * @param currentSnapshot current list of all active processes
     */
    @Override
    public void onSnapshotUpdate(List<ProcessInfo> currentSnapshot) {
        processAll(currentSnapshot);
    }

    /**
     * Processes a list of processes by evaluating each one individually.
     * @param processes list of processes to evaluate
     */
    private void processAll(List<ProcessInfo> processes) {
        for (ProcessInfo process : processes) {
            processProcess(process);
        }
    }

    /**
     * Processes a single process through rule evaluation, alert handling and cleanup logic.
     * @param process process to evaluate
     */
    private void processProcess(ProcessInfo process) {
        AlertEvent alert = evaluateRules(process);
        processAlertIfNeeded(alert, process);
        cleanupInactiveAlerts(process, alert);
    }

    /**
     * Evaluates all built-in alert rules for a process.
     * @param process process to evaluate
     * @return generated AlertEvent if a rule is triggered, otherwise null
     */
    private AlertEvent evaluateRules(ProcessInfo process) {

        if (isBlacklisted(process)) {
            return createAlert(
                    process,
                    AlertType.BLACKLISTED_PROCESS,
                    Severity.CRITICAL,
                    "Blacklisted process detected: " + process.getName()
            );
        }

        if (isHighCpu(process)) {
            return createAlert(
                    process,
                    AlertType.HIGH_CPU_USAGE,
                    Severity.HIGH,
                    "High CPU usage detected: " + process.getName()
            );
        }

        if (isHighMemory(process)) {
            return createAlert(
                    process,
                    AlertType.HIGH_MEMORY_USAGE,
                    Severity.MEDIUM,
                    "High memory usage detected: " + process.getName()
            );
        }

        if (isSuspicious(process)) {
            return createAlert(
                    process,
                    AlertType.UNKNOWN_PROCESS,
                    Severity.LOW,
                    "Unknown/suspicious process: " + process.getName()
            );
        }

        return null;
    }

    /**
     * Checks if a process is blacklisted.
     */
    private boolean isBlacklisted(ProcessInfo process) {
        return process.getStatus() == Status.BLOCKED;
    }

    /**
     * Checks if a process exceeds CPU usage threshold.
     */
    private boolean isHighCpu(ProcessInfo process) {
        return process.getCpuUsage() > AppConfig.getInstance().getCpuThreshold();
    }

    /**
     * Checks if a process exceeds memory usage threshold.
     */
    private boolean isHighMemory(ProcessInfo process) {
        return process.getMemoryUsageMB() > AppConfig.getInstance().getMemoryThreshold();
    }

    /**
     * Checks if a process is marked as suspicious.
     */
    private boolean isSuspicious(ProcessInfo process) {
        return process.getStatus() == Status.SUSPICIOUS;
    }

    /**
     * Creates an AlertEvent object from process and rule data.
     * @param process affected process
     * @param type alert type
     * @param severity alert severity level
     * @param message alert description
     * @return constructed AlertEvent
     */
    private AlertEvent createAlert(
            ProcessInfo process,
            AlertType type,
            Severity severity,
            String message
    ) {
        return new AlertEvent(generateAlertId(), process, type, severity, message);
    }

    /**
     * Handles alert registration if it satisfies triggering conditions.
     * @param alert generated alert (nullable)
     * @param process associated process
     */
    private void processAlertIfNeeded(AlertEvent alert, ProcessInfo process) {

        if (alert == null) return;

        String alertKey = buildAlertKey(process, alert.getType());

        if (shouldTriggerAlert(alertKey)) {
            registerAlert(alertKey, alert);
        }
    }

    /**
     * Checks whether an alert should be triggered based on duplication and history capacity constraints.
     * @param alertKey unique identifier for the alert
     * @return true if alert should be triggered
     */
    private boolean shouldTriggerAlert(String alertKey) {
        return !activeAlerts.contains(alertKey)
                && alertHistory.size() < MAX_ALERT_HISTORY;
    }

    /**
     * Registers and processes a valid alert.
     * @param alertKey unique alert identifier
     * @param alert alert event to register
     */
    private void registerAlert(String alertKey, AlertEvent alert) {
        activeAlerts.add(alertKey);
        alertHistory.add(alert);

        notifyAlertListeners(alert);

        if (historyStorage != null) {
            historyStorage.saveAlert(alert);
        }

        System.out.println("[ALERT] " + alert);
    }

    /**
     * Builds a unique alert key for deduplication.
     * @param process process associated with alert
     * @param type type of alert
     * @return unique string key
     */
    private String buildAlertKey(ProcessInfo process, AlertType type) {
        return process.getPid() + "_" + type;
    }

    /**
     * Cleans up active alerts if a process no longer triggers any rule.
     * @param process process being evaluated
     * @param alert current alert (null if none triggered)
     */
    private void cleanupInactiveAlerts(ProcessInfo process, AlertEvent alert) {
        if (alert != null) return;

        long pid = process.getPid();

        activeAlerts.remove(pid + "_HIGH_CPU_USAGE");
        activeAlerts.remove(pid + "_HIGH_MEMORY_USAGE");
        activeAlerts.remove(pid + "_UNKNOWN_PROCESS");
        activeAlerts.remove(pid + "_BLACKLISTED_PROCESS");
    }

    /**
     * Notifies all registered listeners about a new alert.
     * @param alert alert event to broadcast
     */
    private void notifyAlertListeners(AlertEvent alert) {
        for (AlertListener listener : alertListeners) {
            try {
                listener.onAlert(alert);
            } catch (Exception ignored) {
                // silent fail per design
            }
        }
    }

    /**
     * Generates a unique alert ID.
     * @return timestamp-based alert ID
     */
    private long generateAlertId() {
        return System.currentTimeMillis();
    }

    /**
     * Returns a copy of the alert history.
     * @return list of past alert events
     */
    public List<AlertEvent> getAlertHistory() {
        return new ArrayList<>(alertHistory);
    }
}