package com.processguard.models;

public enum RuleAction {
    LOG_ONLY,
    ALERT_ONLY,
    KILL_PROCESS;

    public static RuleAction fromString(String value) {
        if (value == null) return ALERT_ONLY;

        try {
            return RuleAction.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ALERT_ONLY; // safe default
        }
    }
}