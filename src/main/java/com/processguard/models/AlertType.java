package com.processguard.models;

/**
 * Enumeration of all supported alert types in ProcessGuard.
 * As defined in section 4.2 of the SDD.
 */
public enum AlertType {
    HIGH_CPU_USAGE,
    HIGH_MEMORY_USAGE,
    BLACKLISTED_PROCESS,
    UNKNOWN_PROCESS,
    SUSPICIOUS_PARENT,
    RAPID_SPAWN,
    CUSTOM_RULE_VIOLATION
}