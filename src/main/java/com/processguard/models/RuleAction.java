package com.processguard.models;

/**
 * Defines actions executed when a custom rule is triggered.
 */
public enum RuleAction {

    LOG_ONLY,
    ALERT_ONLY,
    KILL_PROCESS;

    /**
     * Parses a string into a RuleAction safely.
     * @param value input string
     * @return parsed RuleAction or ALERT_ONLY as default
     */
    public static RuleAction fromString(String value) {
        if (value == null) return ALERT_ONLY;

        try {
            return RuleAction.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ALERT_ONLY;
        }
    }
}