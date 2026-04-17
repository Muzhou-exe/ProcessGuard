package com.processguard.core;

import com.processguard.models.Condition;
import com.processguard.models.CustomRule;
import com.processguard.models.ProcessInfo;

import java.util.List;

/**
 * Handles rule matching logic ONLY (no side effects).
 */
public class RuleEvaluator {

    /**
     * Checks whether a process matches a given custom rule.
     * @param process target process
     * @param rule custom rule to evaluate
     * @return true if rule matches, false otherwise
     */
    public boolean matches(ProcessInfo process, CustomRule rule) {
        List<Condition> conditions = rule.getConditions();
        if (conditions == null || conditions.isEmpty()) {
            return false;
        }

        String logic = rule.getLogicOperator();

        if ("AND".equalsIgnoreCase(logic)) return matchesAll(process, conditions);
        if ("OR".equalsIgnoreCase(logic)) return matchesAny(process, conditions);

        return false;
    }

    /**
     * Checks if all conditions match.
     * @param process target process
     * @param conditions rule conditions
     * @return true if all match
     */
    private boolean matchesAll(ProcessInfo process, List<Condition> conditions) {
        for (Condition c : conditions) {
            if (!evaluateCondition(process, c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if any condition matches.
     * @param process target process
     * @param conditions rule conditions
     * @return true if any match
     */
    private boolean matchesAny(ProcessInfo process, List<Condition> conditions) {
        for (Condition c : conditions) {
            if (evaluateCondition(process, c)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Evaluates a single condition against a process.
     * @param p process
     * @param c condition
     * @return evaluation result
     */
    private boolean evaluateCondition(ProcessInfo p, Condition c) {
        try {
            return switch (c.getField()) {

                case "name" -> compareString(p.getName(), c.getOperator(), c.getValue());

                case "cpuUsage" -> compareDouble(p.getCpuUsage(), c.getOperator(), parseDouble(c.getValue()));

                case "memoryUsageMB" -> compareDouble(p.getMemoryUsageMB(), c.getOperator(), parseDouble(c.getValue()));

                case "pid" -> compareLong(p.getPid(), c.getOperator(), parseLong(c.getValue()));

                case "parentPid" -> compareLong(p.getParentPid(), c.getOperator(), parseLong(c.getValue()));

                case "executablePath" -> compareString(p.getExecutablePath(), c.getOperator(), c.getValue());

                default -> false;
            };
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Compares string values using rule operator.
     * @param actual actual value
     * @param op operator
     * @param expected expected value
     * @return comparison result
     */
    private boolean compareString(String actual, String op, String expected) {
        if (actual == null) return false;

        return switch (op.toUpperCase()) {
            case "EQUALS" -> actual.equalsIgnoreCase(expected);
            case "CONTAINS" -> actual.toLowerCase().contains(expected.toLowerCase());
            default -> false;
        };
    }

    /**
     * Compares double values using rule operator.
     * @param actual actual value
     * @param op operator
     * @param expected expected value
     * @return comparison result
     */
    private boolean compareDouble(double actual, String op, double expected) {
        return switch (op.toUpperCase()) {
            case "GREATER_THAN" -> actual > expected;
            case "LESS_THAN" -> actual < expected;
            case "EQUALS" -> Math.abs(actual - expected) < 0.0001;
            default -> false;
        };
    }

    /**
     * Compares long values using rule operator.
     * @param actual actual value
     * @param op operator
     * @param expected expected value
     * @return comparison result
     */
    private boolean compareLong(long actual, String op, long expected) {
        return switch (op.toUpperCase()) {
            case "GREATER_THAN" -> actual > expected;
            case "LESS_THAN" -> actual < expected;
            case "EQUALS" -> actual == expected;
            default -> false;
        };
    }

    /**
     * Parses string into double safely.
     * @param v input string
     * @return parsed double or 0 if invalid
     */
    private double parseDouble(String v) {
        try {
            return Double.parseDouble(v);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Parses string into long safely.
     * @param v input string
     * @return parsed long or 0 if invalid
     */
    private long parseLong(String v) {
        try {
            return Long.parseLong(v);
        } catch (Exception e) {
            return 0;
        }
    }
}