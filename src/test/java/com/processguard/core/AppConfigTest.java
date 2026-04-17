package com.processguard.core;

import com.processguard.models.CustomRule;
import com.processguard.models.RuleAction;
import com.processguard.models.Severity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AppConfig}.
 *
 * AppConfig is a singleton, so each test saves/restores any values it mutates
 * to avoid cross-test pollution.
 */
class AppConfigTest {

    private AppConfig config;

    // Saved originals restored in @AfterEach
    private int origScanInterval;
    private double origCpu;
    private double origMem;
    private int origPort;
    private boolean origMinimized;
    private boolean origTray;

    @BeforeEach
    void saveOriginals() {
        config = AppConfig.getInstance();
        origScanInterval = config.getScanIntervalSeconds();
        origCpu          = config.getCpuThreshold();
        origMem          = config.getMemoryThreshold();
        origPort         = config.getWebPort();
        origMinimized    = config.isStartMinimized();
        origTray         = config.isEnableSystemTray();
        config.clearCustomRules();
    }

    @AfterEach
    void restoreOriginals() {
        config.setScanIntervalSeconds(origScanInterval);
        config.setCpuThreshold(origCpu);
        config.setMemoryThreshold(origMem);
        config.setWebPort(origPort);
        config.setStartMinimized(origMinimized);
        config.setEnableSystemTray(origTray);
        config.clearCustomRules();
        // Clean up any test entries added to lists
        config.getBlacklist().forEach(config::removeFromBlacklist);
        config.getWhitelist().forEach(config::removeFromWhitelist);
    }

    // ── Singleton ─────────────────────────────────────────────────────────────

    @Test
    void getInstance_returnsSameInstance() {
        assertSame(AppConfig.getInstance(), AppConfig.getInstance());
    }

    @Test
    void getInstance_returnsNonNull() {
        assertNotNull(AppConfig.getInstance());
    }

    // ── Default values ────────────────────────────────────────────────────────

    @Test
    void defaultScanInterval_isAtLeastOne() {
        assertTrue(config.getScanIntervalSeconds() >= 1);
    }

    @Test
    void defaultCpuThreshold_isPositive() {
        assertTrue(config.getCpuThreshold() > 0);
    }

    @Test
    void defaultMemoryThreshold_isPositive() {
        assertTrue(config.getMemoryThreshold() > 0);
    }

    @Test
    void defaultCollections_areNotNull() {
        assertNotNull(config.getBlacklist());
        assertNotNull(config.getWhitelist());
        assertNotNull(config.getCustomRules());
    }

    // ── scanIntervalSeconds ───────────────────────────────────────────────────

    @Test
    void setScanInterval_validValue_stored() {
        config.setScanIntervalSeconds(10);
        assertEquals(10, config.getScanIntervalSeconds());
    }

    @Test
    void setScanInterval_zero_clampedToOne() {
        config.setScanIntervalSeconds(0);
        assertEquals(1, config.getScanIntervalSeconds());
    }

    @Test
    void setScanInterval_negative_clampedToOne() {
        config.setScanIntervalSeconds(-5);
        assertEquals(1, config.getScanIntervalSeconds());
    }

    // ── cpuThreshold ──────────────────────────────────────────────────────────

    @Test
    void setCpuThreshold_validValue_stored() {
        config.setCpuThreshold(75.0);
        assertEquals(75.0, config.getCpuThreshold(), 0.001);
    }

    @Test
    void setCpuThreshold_negative_clampedToZero() {
        config.setCpuThreshold(-10.0);
        assertEquals(0.0, config.getCpuThreshold(), 0.001);
    }

    @Test
    void setCpuThreshold_zero_stored() {
        config.setCpuThreshold(0.0);
        assertEquals(0.0, config.getCpuThreshold(), 0.001);
    }

    // ── memoryThreshold ───────────────────────────────────────────────────────

    @Test
    void setMemoryThreshold_validValue_stored() {
        config.setMemoryThreshold(1024.0);
        assertEquals(1024.0, config.getMemoryThreshold(), 0.001);
    }

    @Test
    void setMemoryThreshold_negative_clampedToZero() {
        config.setMemoryThreshold(-200.0);
        assertEquals(0.0, config.getMemoryThreshold(), 0.001);
    }

    // ── blacklist ─────────────────────────────────────────────────────────────

    @Test
    void addToBlacklist_entryAppearsInSet() {
        config.addToBlacklist("badapp.exe");
        assertTrue(config.getBlacklist().contains("badapp.exe"));
        config.removeFromBlacklist("badapp.exe");
    }

    @Test
    void removeFromBlacklist_entryDisappears() {
        config.addToBlacklist("badapp.exe");
        config.removeFromBlacklist("badapp.exe");
        assertFalse(config.getBlacklist().contains("badapp.exe"));
    }

    @Test
    void addToBlacklist_null_ignoredSilently() {
        int before = config.getBlacklist().size();
        config.addToBlacklist(null);
        assertEquals(before, config.getBlacklist().size());
    }

    @Test
    void addToBlacklist_blank_ignoredSilently() {
        int before = config.getBlacklist().size();
        config.addToBlacklist("   ");
        assertEquals(before, config.getBlacklist().size());
    }

    @Test
    void getBlacklist_returnsDefensiveCopy() {
        Set<String> copy = config.getBlacklist();
        copy.add("shouldNotPersist");
        assertFalse(config.getBlacklist().contains("shouldNotPersist"));
    }

    // ── whitelist ─────────────────────────────────────────────────────────────

    @Test
    void addToWhitelist_entryAppearsInSet() {
        config.addToWhitelist("safe.exe");
        assertTrue(config.getWhitelist().contains("safe.exe"));
        config.removeFromWhitelist("safe.exe");
    }

    @Test
    void removeFromWhitelist_entryDisappears() {
        config.addToWhitelist("safe.exe");
        config.removeFromWhitelist("safe.exe");
        assertFalse(config.getWhitelist().contains("safe.exe"));
    }

    @Test
    void addToWhitelist_null_ignoredSilently() {
        int before = config.getWhitelist().size();
        config.addToWhitelist(null);
        assertEquals(before, config.getWhitelist().size());
    }

    @Test
    void getWhitelist_returnsDefensiveCopy() {
        Set<String> copy = config.getWhitelist();
        copy.add("shouldNotPersist");
        assertFalse(config.getWhitelist().contains("shouldNotPersist"));
    }

    // ── webPort ───────────────────────────────────────────────────────────────

    @Test
    void setWebPort_validPort_stored() {
        config.setWebPort(9090);
        assertEquals(9090, config.getWebPort());
    }

    @Test
    void setWebPort_reservedPort80_defaultsTo8080() {
        config.setWebPort(80);
        assertEquals(8080, config.getWebPort());
    }

    @Test
    void setWebPort_portAbove65535_defaultsTo8080() {
        config.setWebPort(70000);
        assertEquals(8080, config.getWebPort());
    }

    @Test
    void setWebPort_portAtBoundary1024_defaultsTo8080() {
        // 1024 is NOT > 1024, so it should default
        config.setWebPort(1024);
        assertEquals(8080, config.getWebPort());
    }

    @Test
    void setWebPort_port1025_stored() {
        config.setWebPort(1025);
        assertEquals(1025, config.getWebPort());
    }

    // ── boolean flags ─────────────────────────────────────────────────────────

    @Test
    void setStartMinimized_true_stored() {
        config.setStartMinimized(true);
        assertTrue(config.isStartMinimized());
    }

    @Test
    void setStartMinimized_false_stored() {
        config.setStartMinimized(false);
        assertFalse(config.isStartMinimized());
    }

    @Test
    void setEnableSystemTray_false_stored() {
        config.setEnableSystemTray(false);
        assertFalse(config.isEnableSystemTray());
    }

    // ── customRules ───────────────────────────────────────────────────────────

    @Test
    void addCustomRule_appearsInList() {
        CustomRule rule = new CustomRule(1, "Test", "", true, null,
                "AND", Severity.LOW, "msg", 0, RuleAction.ALERT_ONLY);
        config.addCustomRule(rule);
        assertEquals(1, config.getCustomRules().size());
    }

    @Test
    void addCustomRule_null_ignoredSilently() {
        config.addCustomRule(null);
        assertTrue(config.getCustomRules().isEmpty());
    }

    @Test
    void clearCustomRules_emptiesTheList() {
        config.addCustomRule(new CustomRule(1, "R1", "", true, null,
                "AND", Severity.LOW, "msg", 0, RuleAction.ALERT_ONLY));
        config.clearCustomRules();
        assertTrue(config.getCustomRules().isEmpty());
    }

    @Test
    void setCustomRules_replacesExistingList() {
        config.addCustomRule(new CustomRule(1, "Old", "", true, null,
                "AND", Severity.LOW, "msg", 0, RuleAction.ALERT_ONLY));

        CustomRule newRule = new CustomRule(2, "New", "", true, null,
                "AND", Severity.HIGH, "msg", 0, RuleAction.ALERT_ONLY);
        config.setCustomRules(List.of(newRule));

        List<CustomRule> rules = config.getCustomRules();
        assertEquals(1, rules.size());
        assertEquals("New", rules.get(0).getName());
    }

    @Test
    void getCustomRules_returnsDefensiveCopy() {
        config.addCustomRule(new CustomRule(1, "Rule", "", true, null,
                "AND", Severity.LOW, "msg", 0, RuleAction.ALERT_ONLY));

        List<CustomRule> copy = config.getCustomRules();
        copy.clear();

        assertEquals(1, config.getCustomRules().size());
    }
}
