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
    private final CustomRule triggeringRule;   // null for normal alerts
    private boolean acknowledged;

    // =========================================================
    // NORMAL ALERT CONSTRUCTOR
    // =========================================================
    public AlertEvent(
            long id,
            ProcessInfo process,
            AlertType type,
            Severity severity,
            String message
    ) {
        this(id, process, type, severity, message, null);
    }

    // =========================================================
    // CUSTOM RULE ALERT CONSTRUCTOR
    // =========================================================
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

    // =========================================================
    // GETTERS
    // =========================================================
    public long getId() {
        return id;
    }

    public ProcessInfo getProcess() {
        return process;
    }

    public AlertType getType() {
        return type;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getMessage() {
        return message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public CustomRule getTriggeringRule() {
        return triggeringRule;
    }

    public boolean isAcknowledged() {
        return acknowledged;
    }

    // =========================================================
    // SETTERS
    // =========================================================
    public void setAcknowledged(boolean acknowledged) {
        this.acknowledged = acknowledged;
    }

    // =========================================================
    // UI DISPLAY HELPER
    // =========================================================
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