package com.processguard.models;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a single alert event.
 * Supports both normal alerts and custom-rule-triggered alerts.
 */
public class AlertEvent {

    private final long id;
    private final ProcessInfo process;
    private final AlertType type;
    private final Severity severity;
    private final String message;
    private final Instant timestamp;
    private final CustomRule triggeringRule;
    private boolean acknowledged;

    /**
     * Creates a normal alert event without a triggering custom rule.
     * @param id alert id
     * @param process associated process
     * @param type alert type
     * @param severity alert severity
     * @param message alert message
     */
    public AlertEvent(
            long id,
            ProcessInfo process,
            AlertType type,
            Severity severity,
            String message
    ) {
        this(id, process, type, severity, message, null);
    }

    /**
     * Creates an alert event with an optional triggering custom rule.
     * @param id alert id
     * @param process associated process
     * @param type alert type
     * @param severity alert severity
     * @param message alert message
     * @param triggeringRule rule that triggered alert (nullable)
     */
    public AlertEvent(
            long id,
            ProcessInfo process,
            AlertType type,
            Severity severity,
            String message,
            CustomRule triggeringRule
    ) {
        this.id = id;
        this.process = Objects.requireNonNull(process, "ProcessInfo cannot be null");
        this.type = Objects.requireNonNull(type, "AlertType cannot be null");
        this.severity = Objects.requireNonNull(severity, "Severity cannot be null");

        this.message = (message != null && !message.isBlank())
                ? message
                : type.name() + " detected for process " + process.getName();

        this.triggeringRule = triggeringRule;
        this.timestamp = Instant.now();
        this.acknowledged = false;
    }

    /**
     * Returns alert id.
     * @return alert id
     */
    public long getId() {
        return id;
    }

    /**
     * Returns associated process.
     * @return process info
     */
    public ProcessInfo getProcess() {
        return process;
    }

    /**
     * Returns alert type.
     * @return alert type
     */
    public AlertType getType() {
        return type;
    }

    /**
     * Returns severity level.
     * @return severity
     */
    public Severity getSeverity() {
        return severity;
    }

    /**
     * Returns alert message.
     * @return message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns timestamp of alert creation.
     * @return timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Returns triggering custom rule if present.
     * @return triggering rule or null
     */
    public CustomRule getTriggeringRule() {
        return triggeringRule;
    }

    /**
     * Checks whether alert has been acknowledged.
     * @return acknowledged state
     */
    public boolean isAcknowledged() {
        return acknowledged;
    }

    /**
     * Sets acknowledged state.
     * @param acknowledged acknowledgement flag
     */
    public void setAcknowledged(boolean acknowledged) {
        this.acknowledged = acknowledged;
    }

    /**
     * Returns UI display message.
     * @return formatted message
     */
    public String getDisplayMessage() {
        if (triggeringRule != null) {
            return triggeringRule.getName() + " - " + message;
        }
        return message;
    }

    @Override
    public String toString() {
        return "[" + severity + "] " + getDisplayMessage();
    }
}