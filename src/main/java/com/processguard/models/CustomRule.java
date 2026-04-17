package com.processguard.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * User-defined custom rule for process monitoring.
 * Matches section 4.3 of the Software Design Document (SDD).
 */
public class CustomRule {

    private final long id;
    private final String name;
    private final String description;
    private boolean enabled;
    private final List<Condition> conditions;
    private final String logicOperator;
    private final Severity severity;
    private final String messageTemplate;
    private final int cooldownSeconds;
    private final RuleAction action;

    /**
     * Creates a custom rule for process monitoring.
     * @param id rule id
     * @param name rule name
     * @param description rule description
     * @param enabled whether rule is enabled
     * @param conditions list of conditions
     * @param logicOperator AND/OR logic operator
     * @param severity rule severity
     * @param messageTemplate alert message template
     * @param cooldownSeconds cooldown period in seconds
     * @param action action to execute when rule matches
     */
    public CustomRule(
            long id,
            String name,
            String description,
            boolean enabled,
            List<Condition> conditions,
            String logicOperator,
            Severity severity,
            String messageTemplate,
            int cooldownSeconds,
            RuleAction action
    ) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "Rule name cannot be null");
        this.description = description != null ? description : "";
        this.enabled = enabled;
        this.conditions = conditions != null
                ? new ArrayList<>(conditions)
                : new ArrayList<>();

        this.logicOperator =
                "OR".equalsIgnoreCase(logicOperator) ? "OR" : "AND";

        this.severity =
                Objects.requireNonNull(severity, "Severity cannot be null");

        this.messageTemplate =
                (messageTemplate != null && !messageTemplate.isBlank())
                        ? messageTemplate
                        : "Custom rule '" + name + "' violated";

        this.cooldownSeconds = Math.max(0, cooldownSeconds);

        this.action = (action != null) ? action : RuleAction.ALERT_ONLY;
    }

    /**
     * Returns rule id.
     * @return rule id
     */
    public long getId() { return id; }

    /**
     * Returns rule name.
     * @return name
     */
    public String getName() { return name; }

    /**
     * Returns rule description.
     * @return description
     */
    public String getDescription() { return description; }

    /**
     * Returns whether rule is enabled.
     * @return true if enabled
     */
    public boolean isEnabled() { return enabled; }

    /**
     * Sets enabled state.
     * @param enabled enabled flag
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Returns rule conditions.
     * @return list of conditions (defensive copy)
     */
    public List<Condition> getConditions() {
        return new ArrayList<>(conditions);
    }

    /**
     * Returns logic operator (AND/OR).
     * @return logic operator
     */
    public String getLogicOperator() {
        return logicOperator;
    }

    /**
     * Returns severity level.
     * @return severity
     */
    public Severity getSeverity() {
        return severity;
    }

    /**
     * Returns message template.
     * @return message template
     */
    public String getMessageTemplate() {
        return messageTemplate;
    }

    /**
     * Returns cooldown period in seconds.
     * @return cooldown seconds
     */
    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    /**
     * Returns rule action.
     * @return rule action
     */
    public RuleAction getAction() {
        return action;
    }

    /**
     * Returns string representation of rule.
     * @return formatted string
     */
    @Override
    public String toString() {
        return String.format(
                "CustomRule[id=%d, name=%s, enabled=%s, severity=%s, action=%s]",
                id, name, enabled, severity, action
        );
    }
}