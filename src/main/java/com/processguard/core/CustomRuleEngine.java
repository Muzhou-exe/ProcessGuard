package com.processguard.core;

import com.processguard.listeners.ProcessListener;
import com.processguard.models.*;

import java.util.List;

/**
 * Evaluates user-defined rules (JSON-configured) against processes.
 * Matches section 2.4 of the SDD.
 */
public class CustomRuleEngine implements ProcessListener {

    private final HistoryStorage historyStorage;
    private final AppConfig config = AppConfig.getInstance();

    public CustomRuleEngine(HistoryStorage historyStorage) {
        this.historyStorage = historyStorage;
    }

    @Override
    public void onNewProcesses(List<ProcessInfo> newProcesses) {
        evaluateCustomRules(newProcesses);
    }

    @Override
    public void onExitedProcesses(List<ProcessInfo> exitedProcesses) {
        // No action on exit (minimal implementation)
    }

    @Override
    public void onSnapshotUpdate(List<ProcessInfo> currentSnapshot) {
        evaluateCustomRules(currentSnapshot);
    }

    private void evaluateCustomRules(List<ProcessInfo> processes) {
        List<CustomRule> rules = config.getCustomRules();

        for (ProcessInfo process : processes) {
            for (CustomRule rule : rules) {
                if (!rule.isEnabled()) continue;

                if (matchesRule(process, rule)) {
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
                    break; // One alert per process per cycle
                }
            }
        }
    }

    private boolean matchesRule(ProcessInfo process, CustomRule rule) {
        boolean result = rule.getLogicOperator().equalsIgnoreCase("AND");

        for (Condition condition : rule.getConditions()) {
            boolean match = evaluateCondition(process, condition);

            if (rule.getLogicOperator().equalsIgnoreCase("AND")) {
                result &= match;
            } else {
                result |= match;
            }
        }

        return result;
    }

    private boolean evaluateCondition(ProcessInfo process, Condition condition) {
        String field = condition.getField();
        String operator = condition.getOperator();
        String value = condition.getValue();

        switch (field) {
            case "name":
                return compareString(process.getName(), operator, value);

            case "cpuUsage":
                return compareDouble(process.getCpuUsage(), operator, Double.parseDouble(value));

            case "memoryUsageMB":
                return compareDouble(process.getMemoryUsageMB(), operator, Double.parseDouble(value));

            default:
                return false;
        }
    }

    private boolean compareString(String actual, String operator, String expected) {
        switch (operator) {
            case "EQUALS":
                return actual.equalsIgnoreCase(expected);
            case "CONTAINS":
                return actual.toLowerCase().contains(expected.toLowerCase());
            default:
                return false;
        }
    }

    private boolean compareDouble(double actual, String operator, double expected) {
        switch (operator) {
            case "GREATER_THAN":
                return actual > expected;
            case "LESS_THAN":
                return actual < expected;
            case "EQUALS":
                return actual == expected;
            default:
                return false;
        }
    }
}