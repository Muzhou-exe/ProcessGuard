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
    private final String logicOperator; // AND / OR
    private final Severity severity;
    private final String messageTemplate;
    private final int cooldownSeconds;
    private final RuleAction action;

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

        this.action = (action != null) ? action : RuleAction.ALERT_ONLY;    }

    // =========================================================
    // GETTERS / SETTERS
    // =========================================================

    public long getId() { return id; }

    public String getName() { return name; }

    public String getDescription() { return description; }

    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<Condition> getConditions() {
        return new ArrayList<>(conditions);
    }

    public String getLogicOperator() {
        return logicOperator;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getMessageTemplate() {
        return messageTemplate;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public RuleAction getAction() {
        return action;
    }

    // =========================================================
    // DEBUG / LOGGING
    // =========================================================

    @Override
    public String toString() {
        return String.format(
                "CustomRule[id=%d, name=%s, enabled=%s, severity=%s, action=%s]",
                id, name, enabled, severity, action
        );
    }
}