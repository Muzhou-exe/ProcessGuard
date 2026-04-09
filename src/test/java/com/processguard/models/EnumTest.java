package com.processguard.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for enum types: {@link Status}, {@link Severity}, {@link AlertType}.
 */
class EnumTest {

    @Test
    void status_hasFourValues() {
        assertEquals(4, Status.values().length);
        Status.valueOf("NORMAL");
        Status.valueOf("SUSPICIOUS");
        Status.valueOf("BLOCKED");
        Status.valueOf("WHITELISTED");
    }

    @Test
    void severity_hasFourValues() {
        assertEquals(4, Severity.values().length);
        Severity.valueOf("LOW");
        Severity.valueOf("MEDIUM");
        Severity.valueOf("HIGH");
        Severity.valueOf("CRITICAL");
    }

    @Test
    void alertType_hasSevenValues() {
        assertEquals(7, AlertType.values().length);
        AlertType.valueOf("HIGH_CPU_USAGE");
        AlertType.valueOf("HIGH_MEMORY_USAGE");
        AlertType.valueOf("BLACKLISTED_PROCESS");
        AlertType.valueOf("UNKNOWN_PROCESS");
        AlertType.valueOf("SUSPICIOUS_PARENT");
        AlertType.valueOf("RAPID_SPAWN");
        AlertType.valueOf("CUSTOM_RULE_VIOLATION");
    }
}
