package com.processguard.core;

import com.processguard.models.AlertEvent;
import com.processguard.models.AlertType;
import com.processguard.models.CustomRule;
import com.processguard.models.ProcessInfo;
import com.processguard.models.RuleAction;
import com.processguard.listeners.AlertListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Executes actions for matched rules (alerts, logs, kill).
 */
public class RuleActionExecutor {

    private final HistoryStorage historyStorage;
    private final List<AlertListener> alertListeners = new ArrayList<>();

    /**
     * Constructs RuleActionExecutor with history storage dependency.
     * @param historyStorage storage used to persist alerts
     */
    public RuleActionExecutor(HistoryStorage historyStorage) {
        this.historyStorage = historyStorage;
    }

    /**
     * Registers an alert listener.
     * @param listener listener to notify on alerts
     */
    public void addAlertListener(AlertListener listener) {
        alertListeners.add(listener);
    }

    /**
     * Notifies all registered alert listeners.
     * @param alert alert event
     */
    private void notifyAlertListeners(AlertEvent alert) {
        for (AlertListener listener : alertListeners) {
            listener.onAlert(alert);
        }
    }

    /**
     * Executes rule action for a process.
     * @param process target process
     * @param rule matched custom rule
     */
    public void execute(ProcessInfo process, CustomRule rule) {

        RuleAction action = rule.getAction();

        if (action == RuleAction.LOG_ONLY) {
            System.out.println("[RULE LOG] " + process);
            return;
        }

        AlertEvent alert = new AlertEvent(
                System.currentTimeMillis(),
                process,
                AlertType.CUSTOM_RULE_VIOLATION,
                rule.getSeverity(),
                rule.getMessageTemplate()
        );

        if (historyStorage != null) {
            historyStorage.saveAlert(alert);
        }

        notifyAlertListeners(alert);

        if (action == RuleAction.KILL_PROCESS) {
            try {
                boolean killed = ProcessKiller.kill(process.getPid());

                if (!killed) {
                    System.err.println("Failed to kill process: " + process.getPid());
                }
            } catch (Exception e) {
                System.err.println("Kill action failed: " + e.getMessage());
            }
        }
    }
}