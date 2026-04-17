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

    public RuleActionExecutor(HistoryStorage historyStorage) {
        this.historyStorage = historyStorage;
    }

    public void addAlertListener(AlertListener listener) {
        alertListeners.add(listener);
    }

    private void notifyAlertListeners(AlertEvent alert) {
        for (AlertListener listener : alertListeners) {
            listener.onAlert(alert);
        }
    }

    public void execute(ProcessInfo process, CustomRule rule) {

        RuleAction action = rule.getAction();

        // =========================
        // LOG ONLY
        // =========================
        if (action == RuleAction.LOG_ONLY) {
            System.out.println("[RULE LOG] " + process);
            return;
        }

        // =========================
        // ALERT (default)
        // =========================
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

        // =========================
        // KILL PROCESS
        // =========================
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