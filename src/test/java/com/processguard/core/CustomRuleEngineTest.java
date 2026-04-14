package com.processguard.core;

import com.processguard.models.Condition;
import com.processguard.models.CustomRule;
import com.processguard.models.ProcessInfo;
import com.processguard.models.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link CustomRuleEngine}.
 */
class CustomRuleEngineTest {

    private HistoryStorage storage;
    private CustomRuleEngine engine;

    @BeforeEach
    void setUp() {
        storage = new HistoryStorage();
        engine = new CustomRuleEngine(storage);
        // Clear any existing custom rules
        AppConfig.getInstance().clearCustomRules();
    }

    /**
     * Creates a simple process for testing.
     */
    private ProcessInfo createProcess(String name, double cpu, long memMB) {
        return new ProcessInfo(1, name, "", cpu, memMB, -1, Instant.now());
    }

    @Test
    void onSnapshotUpdate_matchingRule_savesAlert() {
        Condition cpuCondition = new Condition("cpuUsage", "GREATER_THAN", "90");
        CustomRule rule = new CustomRule(1, "High CPU", "", true,
                List.of(cpuCondition), "AND", Severity.HIGH, "CPU exceeded 90%", 0, "ALERT");
        AppConfig.getInstance().addCustomRule(rule);

        ProcessInfo highCpu = createProcess("app.exe", 95.0, 100);
        engine.onSnapshotUpdate(List.of(highCpu));

        assertEquals(1, storage.getRecentAlerts().size());
    }

    @Test
    void onSnapshotUpdate_nonMatchingRule_noAlert() {
        Condition cpuCondition = new Condition("cpuUsage", "GREATER_THAN", "90");
        CustomRule rule = new CustomRule(1, "High CPU", "", true,
                List.of(cpuCondition), "AND", Severity.HIGH, "CPU exceeded 90%", 0, "ALERT");
        AppConfig.getInstance().addCustomRule(rule);

        ProcessInfo lowCpu = createProcess("app.exe", 10.0, 100);
        engine.onSnapshotUpdate(List.of(lowCpu));

        assertTrue(storage.getRecentAlerts().isEmpty());
    }

    @Test
    void onSnapshotUpdate_disabledRule_noAlert() {
        Condition cpuCondition = new Condition("cpuUsage", "GREATER_THAN", "10");
        CustomRule rule = new CustomRule(1, "Disabled Rule", "", false,
                List.of(cpuCondition), "AND", Severity.LOW, "msg", 0, "ALERT");
        AppConfig.getInstance().addCustomRule(rule);

        ProcessInfo process = createProcess("app.exe", 50.0, 100);
        engine.onSnapshotUpdate(List.of(process));

        assertTrue(storage.getRecentAlerts().isEmpty());
    }

    @Test
    void onSnapshotUpdate_nameEqualsCondition_matches() {
        Condition nameCondition = new Condition("name", "EQUALS", "chrome");
        CustomRule rule = new CustomRule(1, "Chrome Rule", "", true,
                List.of(nameCondition), "AND", Severity.MEDIUM, "Chrome detected", 0, "ALERT");
        AppConfig.getInstance().addCustomRule(rule);

        ProcessInfo chrome = createProcess("chrome", 10, 500);
        engine.onSnapshotUpdate(List.of(chrome));

        assertEquals(1, storage.getRecentAlerts().size());
    }

    @Test
    void onSnapshotUpdate_nameContainsCondition_matches() {
        Condition nameCondition = new Condition("name", "CONTAINS", "chrom");
        CustomRule rule = new CustomRule(1, "Chrome Rule", "", true,
                List.of(nameCondition), "AND", Severity.LOW, "Chrome-like detected", 0, "ALERT");
        AppConfig.getInstance().addCustomRule(rule);

        ProcessInfo chrome = createProcess("chrome.exe", 10, 500);
        engine.onSnapshotUpdate(List.of(chrome));

        assertEquals(1, storage.getRecentAlerts().size());
    }

    @Test
    void onSnapshotUpdate_andLogic_allConditionsMustMatch() {
        Condition cpuCond = new Condition("cpuUsage", "GREATER_THAN", "50");
        Condition memCond = new Condition("memoryUsageMB", "GREATER_THAN", "400");
        CustomRule rule = new CustomRule(1, "Heavy App", "", true,
                List.of(cpuCond, memCond), "AND", Severity.HIGH, "Heavy app", 0, "ALERT");
        AppConfig.getInstance().addCustomRule(rule);

        // Only CPU matches — should NOT fire
        ProcessInfo onlyCpu = createProcess("app.exe", 60, 100);
        engine.onSnapshotUpdate(List.of(onlyCpu));
        assertTrue(storage.getRecentAlerts().isEmpty());

        // Both match — should fire
        ProcessInfo both = createProcess("app.exe", 60, 500);
        engine.onSnapshotUpdate(List.of(both));
        assertEquals(1, storage.getRecentAlerts().size());
    }

    @Test
    void onSnapshotUpdate_orLogic_anyConditionSuffices() {
        Condition cpuCond = new Condition("cpuUsage", "GREATER_THAN", "90");
        Condition memCond = new Condition("memoryUsageMB", "GREATER_THAN", "900");
        CustomRule rule = new CustomRule(1, "Heavy App", "", true,
                List.of(cpuCond, memCond), "OR", Severity.HIGH, "Heavy app", 0, "ALERT");
        AppConfig.getInstance().addCustomRule(rule);

        // Only CPU matches — should fire with OR
        ProcessInfo onlyCpu = createProcess("app.exe", 95, 100);
        engine.onSnapshotUpdate(List.of(onlyCpu));
        assertEquals(1, storage.getRecentAlerts().size());
    }

    @Test
    void onSnapshotUpdate_memoryLessThan_matches() {
        Condition memCond = new Condition("memoryUsageMB", "LESS_THAN", "50");
        CustomRule rule = new CustomRule(1, "Low Mem", "", true,
                List.of(memCond), "AND", Severity.LOW, "Low memory process", 0, "ALERT");
        AppConfig.getInstance().addCustomRule(rule);

        ProcessInfo lowMem = createProcess("tiny.exe", 1, 10);
        engine.onSnapshotUpdate(List.of(lowMem));
        assertEquals(1, storage.getRecentAlerts().size());
    }
}
