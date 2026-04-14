package com.processguard.core;

import com.processguard.listeners.ProcessListener;
import com.processguard.models.*;

import java.util.List;

/**
 * Evaluates user-defined custom rules against system processes.
 * Processes rule conditions and generates alerts for violations.
 */
public class CustomRuleEngine implements ProcessListener {

    private static final String LOGIC_AND = "AND";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_CPU = "cpuUsage";
    private static final String FIELD_MEMORY = "memoryUsageMB";
    private static final String OP_EQUALS = "EQUALS";
    private static final String OP_CONTAINS = "CONTAINS";
    private static final String OP_GT = "GREATER_THAN";
    private static final String OP_LT = "LESS_THAN";

    private final HistoryStorage historyStorage;
    private final AppConfig config = AppConfig.getInstance();

    /**
     * Constructs CustomRuleEngine with history storage.
     * @param historyStorage storage for generated alerts
     */
    public CustomRuleEngine(HistoryStorage historyStorage) {
        this.historyStorage = historyStorage;
    }

    /**
     * Evaluates rules on newly detected processes.
     * @param newProcesses list of new processes
     */
    @Override
    public void onNewProcesses(List<ProcessInfo> newProcesses) {
        evaluateCustomRules(newProcesses);
    }

    /**
     * No action required when processes exit.
     * @param exitedProcesses list of exited processes
     */
    @Override
    public void onExitedProcesses(List<ProcessInfo> exitedProcesses) {
    }

    /**
     * Evaluates rules on full system snapshot.
     * @param currentSnapshot current process list
     */
    @Override
    public void onSnapshotUpdate(List<ProcessInfo> currentSnapshot) {
        evaluateCustomRules(currentSnapshot);
    }

    /**
     * Evaluates all custom rules against given processes.
     * @param processes list of processes to evaluate
     */
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

                    break;
                }
            }
        }
    }

    /**
     * Checks if a process matches a custom rule.
     * @param process process to evaluate
     * @param rule rule definition
     * @return true if rule matches
     */
    private boolean matchesRule(ProcessInfo process, CustomRule rule) {
        boolean result = rule.getLogicOperator().equalsIgnoreCase(LOGIC_AND);

        for (Condition condition : rule.getConditions()) {
            boolean match = evaluateCondition(process, condition);

            if (rule.getLogicOperator().equalsIgnoreCase(LOGIC_AND)) {
                result &= match;
            } else {
                result |= match;
            }
        }

        return result;
    }

    /**
     * Evaluates a single condition against a process.
     * @param process process being evaluated
     * @param condition condition to check
     * @return true if condition matches
     */
    private boolean evaluateCondition(ProcessInfo process, Condition condition) {

        String field = condition.getField();
        String operator = condition.getOperator();
        String value = condition.getValue();

        switch (field) {

            case FIELD_NAME:
                return compareString(process.getName(), operator, value);

            case FIELD_CPU:
                return compareDouble(process.getCpuUsage(), operator, safeParse(value));

            case FIELD_MEMORY:
                return compareDouble(process.getMemoryUsageMB(), operator, safeParse(value));

            default:
                return false;
        }
    }

    /**
     * Compares string values using rule operator.
     * @param actual actual value
     * @param operator comparison operator
     * @param expected expected value
     * @return true if condition matches
     */
    private boolean compareString(String actual, String operator, String expected) {

        switch (operator) {

            case OP_EQUALS:
                return actual.equalsIgnoreCase(expected);

            case OP_CONTAINS:
                return actual.toLowerCase().contains(expected.toLowerCase());

            default:
                return false;
        }
    }

    /**
     * Compares numeric values using rule operator.
     * @param actual actual value
     * @param operator comparison operator
     * @param expected expected value
     * @return true if condition matches
     */
    private boolean compareDouble(double actual, String operator, double expected) {

        switch (operator) {

            case OP_GT:
                return actual > expected;

            case OP_LT:
                return actual < expected;

            case OP_EQUALS:
                return actual == expected;

            default:
                return false;
        }
    }

    /**
     * Safely parses string to double, returns 0 if invalid.
     * @param value input string
     * @return parsed double or 0
     */
    private double safeParse(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return 0.0;
        }
    }
}