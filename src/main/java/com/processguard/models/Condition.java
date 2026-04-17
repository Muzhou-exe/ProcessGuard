package com.processguard.models;

/**
 * Represents a single condition in a user-defined custom rule.
 * Used by CustomRuleEngine (section 4.3 of the SDD).
 */
public class Condition {

    private final String field;
    private final String operator;
    private final String value;

    /**
     * Creates a condition used for rule evaluation.
     * @param field target field to evaluate
     * @param operator comparison operator
     * @param value comparison value
     */
    public Condition(String field, String operator, String value) {
        this.field = field != null ? field.trim() : "";
        this.operator = operator != null ? operator.trim().toUpperCase() : "";
        this.value = value != null ? value.trim() : "";
    }

    /**
     * Returns condition field.
     * @return field name
     */
    public String getField() {
        return field;
    }

    /**
     * Returns comparison operator.
     * @return operator
     */
    public String getOperator() {
        return operator;
    }

    /**
     * Returns comparison value.
     * @return value
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns string representation of condition.
     * @return formatted condition
     */
    @Override
    public String toString() {
        return field + " " + operator + " " + value;
    }
}