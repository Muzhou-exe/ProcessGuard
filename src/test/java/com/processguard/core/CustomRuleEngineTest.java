package com.processguard.core;

import com.processguard.models.AlertEvent;
import com.processguard.models.Condition;
import com.processguard.models.CustomRule;
import com.processguard.models.ProcessInfo;
import com.processguard.models.RuleAction;
import com.processguard.models.Severity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CustomRuleEngine}.
 *
 * Uses RuleAction enum values (not raw Strings) in all CustomRule constructors.
 */
class CustomRuleEngineTest {

    private HistoryStorage storage;
    private CustomRuleEngine engine;
    private List<AlertEvent> receivedAlerts;

    @BeforeEach
    void setUp() {
        storage = new HistoryStorage();
        engine  = new CustomRuleEngine(storage);
        receivedAlerts = new ArrayList<>();
        engine.addAlertListener(receivedAlerts::add);
        AppConfig.getInstance().clearCustomRules();
    }

    @AfterEach
    void tearDown() {
        AppConfig.getInstance().clearCustomRules();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ProcessInfo proc(String name, double cpu, long memMB) {
        return new ProcessInfo(1, name, "", cpu, memMB, -1, Instant.now());
    }

    private CustomRule alertRule(String field, String op, String value, String logic,
                                 Severity severity) {
        return new CustomRule(1, "TestRule", "", true,
                List.of(new Condition(field, op, value)),
                logic, severity, "triggered", 0, RuleAction.ALERT_ONLY);
    }

    // ── No rules registered ───────────────────────────────────────────────────

    @Test
    void noRules_noAlerts() {
        engine.onSnapshotUpdate(List.of(proc("app.exe", 99, 9999)));
        assertTrue(storage.getRecentAlerts().isEmpty());
    }

    // ── Matching / non-matching ───────────────────────────────────────────────

    @Test
    void matchingRule_alertSavedToStorage() {
        AppConfig.getInstance().addCustomRule(
                alertRule("cpuUsage", "GREATER_THAN", "90", "AND", Severity.HIGH));

        engine.onSnapshotUpdate(List.of(proc("app.exe", 95.0, 100)));

        assertEquals(1, storage.getRecentAlerts().size());
    }

    @Test
    void matchingRule_alertDeliveredToListener() {
        AppConfig.getInstance().addCustomRule(
                alertRule("cpuUsage", "GREATER_THAN", "90", "AND", Severity.HIGH));

        engine.onSnapshotUpdate(List.of(proc("app.exe", 95.0, 100)));

        assertEquals(1, receivedAlerts.size());
    }

    @Test
    void nonMatchingRule_noAlert() {
        AppConfig.getInstance().addCustomRule(
                alertRule("cpuUsage", "GREATER_THAN", "90", "AND", Severity.HIGH));

        engine.onSnapshotUpdate(List.of(proc("app.exe", 10.0, 100)));

        assertTrue(storage.getRecentAlerts().isEmpty());
    }

    @Test
    void disabledRule_noAlert() {
        CustomRule disabled = new CustomRule(1, "Disabled", "", false,
                List.of(new Condition("cpuUsage", "GREATER_THAN", "10")),
                "AND", Severity.LOW, "msg", 0, RuleAction.ALERT_ONLY);
        AppConfig.getInstance().addCustomRule(disabled);

        engine.onSnapshotUpdate(List.of(proc("app.exe", 50.0, 100)));

        assertTrue(storage.getRecentAlerts().isEmpty());
    }

    // ── Field coverage ────────────────────────────────────────────────────────

    @Test
    void nameEquals_matches() {
        AppConfig.getInstance().addCustomRule(
                alertRule("name", "EQUALS", "chrome", "AND", Severity.MEDIUM));

        engine.onSnapshotUpdate(List.of(proc("chrome", 0, 0)));
        assertEquals(1, storage.getRecentAlerts().size());
    }

    @Test
    void nameContains_matches() {
        AppConfig.getInstance().addCustomRule(
                alertRule("name", "CONTAINS", "chrom", "AND", Severity.LOW));

        engine.onSnapshotUpdate(List.of(proc("chrome.exe", 0, 0)));
        assertEquals(1, storage.getRecentAlerts().size());
    }

    @Test
    void memoryLessThan_matches() {
        AppConfig.getInstance().addCustomRule(
                alertRule("memoryUsageMB", "LESS_THAN", "50", "AND", Severity.LOW));

        engine.onSnapshotUpdate(List.of(proc("tiny.exe", 1, 10)));
        assertEquals(1, storage.getRecentAlerts().size());
    }

    // ── AND / OR logic ────────────────────────────────────────────────────────

    @Test
    void andLogic_allConditionsMustMatch_bothMatch_fires() {
        CustomRule r = new CustomRule(1, "Heavy", "", true, List.of(
                new Condition("cpuUsage",      "GREATER_THAN", "50"),
                new Condition("memoryUsageMB", "GREATER_THAN", "400")
        ), "AND", Severity.HIGH, "Heavy app detected", 0, RuleAction.ALERT_ONLY);
        AppConfig.getInstance().addCustomRule(r);

        engine.onSnapshotUpdate(List.of(proc("app.exe", 60, 500)));
        assertEquals(1, storage.getRecentAlerts().size());
    }

    @Test
    void andLogic_onlyOneMeetsCondition_noFire() {
        CustomRule r = new CustomRule(1, "Heavy", "", true, List.of(
                new Condition("cpuUsage",      "GREATER_THAN", "50"),
                new Condition("memoryUsageMB", "GREATER_THAN", "400")
        ), "AND", Severity.HIGH, "msg", 0, RuleAction.ALERT_ONLY);
        AppConfig.getInstance().addCustomRule(r);

        engine.onSnapshotUpdate(List.of(proc("app.exe", 60, 100)));
        assertTrue(storage.getRecentAlerts().isEmpty());
    }

    @Test
    void orLogic_anyConditionSuffices_fires() {
        CustomRule r = new CustomRule(1, "Either", "", true, List.of(
                new Condition("cpuUsage",      "GREATER_THAN", "90"),
                new Condition("memoryUsageMB", "GREATER_THAN", "900")
        ), "OR", Severity.HIGH, "msg", 0, RuleAction.ALERT_ONLY);
        AppConfig.getInstance().addCustomRule(r);

        // only CPU matches
        engine.onSnapshotUpdate(List.of(proc("app.exe", 95, 100)));
        assertEquals(1, storage.getRecentAlerts().size());
    }

    @Test
    void orLogic_neitherConditionMet_noFire() {
        CustomRule r = new CustomRule(1, "Either", "", true, List.of(
                new Condition("cpuUsage",      "GREATER_THAN", "90"),
                new Condition("memoryUsageMB", "GREATER_THAN", "900")
        ), "OR", Severity.HIGH, "msg", 0, RuleAction.ALERT_ONLY);
        AppConfig.getInstance().addCustomRule(r);

        engine.onSnapshotUpdate(List.of(proc("app.exe", 5, 50)));
        assertTrue(storage.getRecentAlerts().isEmpty());
    }

    // ── Multiple processes ────────────────────────────────────────────────────

    @Test
    void multipleProcesses_onlyMatchingOneFires() {
        AppConfig.getInstance().addCustomRule(
                alertRule("cpuUsage", "GREATER_THAN", "90", "AND", Severity.HIGH));

        ProcessInfo low  = new ProcessInfo(1, "low.exe",  "", 10,  0, -1, Instant.now());
        ProcessInfo high = new ProcessInfo(2, "high.exe", "", 95,  0, -1, Instant.now());

        engine.onSnapshotUpdate(List.of(low, high));
        assertEquals(1, storage.getRecentAlerts().size());
    }

    @Test
    void multipleProcesses_allMatch_allFire() {
        AppConfig.getInstance().addCustomRule(
                alertRule("cpuUsage", "GREATER_THAN", "50", "AND", Severity.HIGH));

        ProcessInfo p1 = new ProcessInfo(1, "a.exe", "", 60, 0, -1, Instant.now());
        ProcessInfo p2 = new ProcessInfo(2, "b.exe", "", 70, 0, -1, Instant.now());

        engine.onSnapshotUpdate(List.of(p1, p2));
        assertEquals(2, storage.getRecentAlerts().size());
    }

    // ── Multiple rules – first match wins ────────────────────────────────────

    @Test
    void multipleRules_firstMatchWins_secondRuleSkipped() {
        // Two rules both matching — engine breaks after first hit per process
        CustomRule r1 = new CustomRule(1, "CPU Rule", "", true,
                List.of(new Condition("cpuUsage", "GREATER_THAN", "50")),
                "AND", Severity.HIGH, "cpu-msg", 0, RuleAction.ALERT_ONLY);
        CustomRule r2 = new CustomRule(2, "Name Rule", "", true,
                List.of(new Condition("name", "EQUALS", "app.exe")),
                "AND", Severity.LOW, "name-msg", 0, RuleAction.ALERT_ONLY);

        AppConfig.getInstance().addCustomRule(r1);
        AppConfig.getInstance().addCustomRule(r2);

        engine.onSnapshotUpdate(List.of(proc("app.exe", 60, 0)));

        // Only one alert should fire (the first matching rule)
        assertEquals(1, storage.getRecentAlerts().size());
        assertEquals("cpu-msg", storage.getRecentAlerts().get(0).getMessage());
    }

    // ── onNewProcesses ────────────────────────────────────────────────────────

    @Test
    void onNewProcesses_matchingRule_fires() {
        AppConfig.getInstance().addCustomRule(
                alertRule("name", "EQUALS", "newproc", "AND", Severity.MEDIUM));

        engine.onNewProcesses(List.of(
                new ProcessInfo(99, "newproc", "", 0, 0, -1, Instant.now())));

        assertEquals(1, storage.getRecentAlerts().size());
    }

    // ── onExitedProcesses ─────────────────────────────────────────────────────

    @Test
    void onExitedProcesses_doesNothing() {
        AppConfig.getInstance().addCustomRule(
                alertRule("name", "EQUALS", "gone", "AND", Severity.LOW));

        engine.onExitedProcesses(List.of(
                new ProcessInfo(1, "gone", "", 0, 0, -1, Instant.now())));

        assertTrue(storage.getRecentAlerts().isEmpty());
    }

    // ── LOG_ONLY action ───────────────────────────────────────────────────────

    @Test
    void logOnlyAction_noAlertSaved() {
        CustomRule r = new CustomRule(1, "LogRule", "", true,
                List.of(new Condition("cpuUsage", "GREATER_THAN", "10")),
                "AND", Severity.LOW, "log-msg", 0, RuleAction.LOG_ONLY);
        AppConfig.getInstance().addCustomRule(r);

        engine.onSnapshotUpdate(List.of(proc("app.exe", 50, 0)));

        // LOG_ONLY: no alert should be stored or fired
        assertTrue(storage.getRecentAlerts().isEmpty());
        assertTrue(receivedAlerts.isEmpty());
    }
}
