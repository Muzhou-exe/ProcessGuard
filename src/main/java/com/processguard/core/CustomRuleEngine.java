package com.processguard.core;

import com.processguard.listeners.ProcessListener;
import com.processguard.models.*;
import com.processguard.core.ProcessKiller;
import com.processguard.models.RuleAction;
import com.processguard.listeners.AlertListener;
import java.util.ArrayList;

import java.util.List;

/**
 * Custom Rule Engine
 * - Evaluates user-defined rules against process snapshots
 * - Stateless per evaluation cycle (safe + predictable)
 */
public class CustomRuleEngine implements ProcessListener {

    private final HistoryStorage historyStorage;
    private final AppConfig config = AppConfig.getInstance();

    private final List<AlertListener> alertListeners = new ArrayList<>();

    public CustomRuleEngine(HistoryStorage historyStorage) {
        this.historyStorage = historyStorage;
    }

    // =========================================================
    // PROCESS LISTENER CALLBACKS
    // =========================================================

    public void addAlertListener(AlertListener listener) {
        alertListeners.add(listener);
    }

    private void notifyAlertListeners(AlertEvent alert) {
        for (AlertListener listener : alertListeners) {
            listener.onAlert(alert);
        }
    }

    @Override
    public void onNewProcesses(List<ProcessInfo> newProcesses) {
        evaluate(newProcesses);
    }

    @Override
    public void onExitedProcesses(List<ProcessInfo> exitedProcesses) {
        // no-op (could be used for audit logs later)
    }

    @Override
    public void onSnapshotUpdate(List<ProcessInfo> snapshot) {
        evaluate(snapshot);
    }

    // =========================================================
    // CORE ENGINE
    // =========================================================

    private void evaluate(List<ProcessInfo> processes) {
        System.out.println("CustomRuleEngine evaluating " + processes.size() + " processes");

        List<CustomRule> rules = config.getCustomRules();
        if (rules == null || rules.isEmpty()) return;

        for (ProcessInfo process : processes) {
            for (CustomRule rule : rules) {

                if (!rule.isEnabled()) continue;

                System.out.println("ACTION = " + rule.getAction()
                        + " for rule " + rule.getName());

                if (matches(process, rule)) {

                    RuleAction action = rule.getAction();

                    // =========================
                    // 1. LOG ONLY
                    // =========================
                    if (action == RuleAction.LOG_ONLY) {
                        System.out.println("[RULE LOG] " + process);

                        // =========================
                        // 2. ALERT ONLY
                        // =========================
                    } else if (action == null || action == RuleAction.ALERT_ONLY) {
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
                        notifyAlertListeners(alert);  // ← FIX: notify regardless of storage

                        // =========================
                        // 3. KILL PROCESS
                        // =========================
                    } else if (action == RuleAction.KILL_PROCESS) {
                        AlertEvent alert = new AlertEvent(
                                System.currentTimeMillis(),
                                process,
                                AlertType.CUSTOM_RULE_VIOLATION,
                                rule.getSeverity(),
                                rule.getDescription() != null ? rule.getDescription() : "Custom rule violated",
                                rule
                        );

                        if (historyStorage != null) {
                            historyStorage.saveAlert(alert);
                        }
                        notifyAlertListeners(alert);

                        try {
                            boolean killed = ProcessKiller.kill(process.getPid());
                            if (!killed) {
                                System.err.println("Failed to kill process: " + process.getPid());
                            }
                        } catch (Exception e) {
                            System.err.println("Kill action failed: " + e.getMessage());
                        }
                    }

                    break;
                }
            }
        }
    }

    // =========================================================
    // RULE MATCHING
    // =========================================================

    private boolean matches(ProcessInfo process, CustomRule rule) {

        List<Condition> conditions = rule.getConditions();
        if (conditions == null || conditions.isEmpty()) {
            return false;
        }

        String logic = rule.getLogicOperator();

        if ("AND".equalsIgnoreCase(logic)) return matchesAll(process, conditions);
        if ("OR".equalsIgnoreCase(logic)) return matchesAny(process, conditions);

        return false;
    }

    private boolean matchesAll(ProcessInfo process, List<Condition> conditions) {
        for (Condition c : conditions) {
            if (!evaluateCondition(process, c)) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesAny(ProcessInfo process, List<Condition> conditions) {
        for (Condition c : conditions) {
            if (evaluateCondition(process, c)) {
                return true;
            }
        }
        return false;
    }

    // =========================================================
    // CONDITION EVALUATION
    // =========================================================

    private boolean evaluateCondition(ProcessInfo p, Condition c) {

        String field = c.getField();
        String op = c.getOperator();
        String value = c.getValue();

        try {
            return switch (field) {

                case "name" -> compareString(p.getName(), op, value);

                case "cpuUsage" -> compareDouble(p.getCpuUsage(), op, parseDouble(value));

                case "memoryUsageMB" -> compareDouble(p.getMemoryUsageMB(), op, parseDouble(value));

                case "pid" -> compareLong(p.getPid(), op, parseLong(value));

                case "parentPid" -> compareLong(p.getParentPid(), op, parseLong(value));

                case "executablePath" -> compareString(p.getExecutablePath(), op, value);

                default -> false;
            };
        } catch (Exception e) {
            // NEVER crash engine due to bad rule config
            return false;
        }
    }

    // =========================================================
    // TYPE COMPARISONS
    // =========================================================

    private boolean compareString(String actual, String op, String expected) {
        if (actual == null) return false;

        return switch (op.toUpperCase()) {
            case "EQUALS" -> actual.equalsIgnoreCase(expected);
            case "CONTAINS" -> actual.toLowerCase().contains(expected.toLowerCase());
            default -> false;
        };
    }

    private boolean compareDouble(double actual, String op, double expected) {
        return switch (op.toUpperCase()) {
            case "GREATER_THAN" -> actual > expected;
            case "LESS_THAN" -> actual < expected;
            case "EQUALS" -> Math.abs(actual - expected) < 0.0001;
            default -> false;
        };
    }

    private boolean compareLong(long actual, String op, long expected) {
        return switch (op.toUpperCase()) {
            case "GREATER_THAN" -> actual > expected;
            case "LESS_THAN" -> actual < expected;
            case "EQUALS" -> Math.abs(actual - expected) < 0.0001;
            default -> false;
        };
    }

    // =========================================================
    // SAFE PARSING
    // =========================================================

    private double parseDouble(String v) {
        try {
            return Double.parseDouble(v);
        } catch (Exception e) {
            return 0;
        }
    }

    private long parseLong(String v) {
        try {
            return Long.parseLong(v);
        } catch (Exception e) {
            return 0;
        }
    }
}