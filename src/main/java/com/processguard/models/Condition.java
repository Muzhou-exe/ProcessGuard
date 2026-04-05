package com.processguard.models;

/**
 * Represents a single condition in a user-defined custom rule.
 * Used by CustomRuleEngine (section 4.3 of the SDD).
 */
public class Condition {

    private final String field;      // e.g., "name", "cpuUsage", "memoryUsageMB", etc.
    private final String operator;   // e.g., "EQUALS", "GREATER_THAN", "REGEX_MATCH", etc.
    private final String value;      // value to compare against (String for flexibility)

    public Condition(String field, String operator, String value) {
        this.field = field != null ? field.trim() : "";
        this.operator = operator != null ? operator.trim().toUpperCase() : "";
        this.value = value != null ? value.trim() : "";
    }

    public String getField() {
        return field;
    }

    public String getOperator() {
        return operator;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return field + " " + operator + " " + value;
    }
}