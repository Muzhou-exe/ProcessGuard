package com.processguard.core;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link AppConfig}.
 */
class AppConfigTest {

    @Test
    void getInstance_returnsSameInstance() {
        AppConfig a = AppConfig.getInstance();
        AppConfig b = AppConfig.getInstance();
        assertSame(a, b);
    }

    @Test
    void getInstance_returnsNonNull() {
        assertNotNull(AppConfig.getInstance());
    }

    @Test
    void defaultValues_areReasonable() {
        AppConfig config = AppConfig.getInstance();

        assertTrue(config.getScanIntervalSeconds() >= 1);
        assertTrue(config.getCpuThreshold() > 0);
        assertTrue(config.getMemoryThreshold() > 0);
        assertNotNull(config.getBlacklist());
        assertNotNull(config.getWhitelist());
        assertNotNull(config.getCustomRules());
    }

    @Test
    void setScanIntervalSeconds_clampedToMinOne() {
        AppConfig config = AppConfig.getInstance();
        int original = config.getScanIntervalSeconds();

        config.setScanIntervalSeconds(0);
        assertEquals(1, config.getScanIntervalSeconds());

        config.setScanIntervalSeconds(-5);
        assertEquals(1, config.getScanIntervalSeconds());

        // Restore
        config.setScanIntervalSeconds(original);
    }

    @Test
    void setCpuThreshold_clampedToNonNegative() {
        AppConfig config = AppConfig.getInstance();
        double original = config.getCpuThreshold();

        config.setCpuThreshold(-10.0);
        assertEquals(0.0, config.getCpuThreshold(), 0.001);

        // Restore
        config.setCpuThreshold(original);
    }

    @Test
    void blacklist_addAndRemove() {
        AppConfig config = AppConfig.getInstance();

        config.addToBlacklist("testprocess.exe");
        assertTrue(config.getBlacklist().contains("testprocess.exe"));

        config.removeFromBlacklist("testprocess.exe");
        assertFalse(config.getBlacklist().contains("testprocess.exe"));
    }

    @Test
    void whitelist_addAndRemove() {
        AppConfig config = AppConfig.getInstance();

        config.addToWhitelist("safeapp.exe");
        assertTrue(config.getWhitelist().contains("safeapp.exe"));

        config.removeFromWhitelist("safeapp.exe");
        assertFalse(config.getWhitelist().contains("safeapp.exe"));
    }

    @Test
    void getBlacklist_returnsDefensiveCopy() {
        AppConfig config = AppConfig.getInstance();
        Set<String> copy = config.getBlacklist();
        copy.add("shouldNotAppear");

        assertFalse(config.getBlacklist().contains("shouldNotAppear"));
    }

    @Test
    void setWebPort_invalidPort_defaultsTo8080() {
        AppConfig config = AppConfig.getInstance();
        int original = config.getWebPort();

        config.setWebPort(80);
        assertEquals(8080, config.getWebPort());

        // Restore
        config.setWebPort(original);
    }
}
