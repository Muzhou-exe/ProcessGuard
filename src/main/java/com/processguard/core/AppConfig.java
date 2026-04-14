package com.processguard.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.processguard.models.CustomRule;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Centralized configuration manager for ProcessGuard.
 * Implements a thread-safe Singleton with JSON-based persistence.
 */
public class AppConfig {

    private static volatile AppConfig instance;

    private static final int MIN_SCAN_INTERVAL = 1;
    private static final double MIN_THRESHOLD = 0.0;
    private static final int DEFAULT_WEB_PORT = 8080;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Path configPath = Paths.get(System.getProperty("user.home"), ".processguard", "config.json");

    private int scanIntervalSeconds = 3;
    private double cpuThreshold = 20.0;
    private double memoryThreshold = 500.0;

    private final Set<String> blacklist = new HashSet<>();
    private final Set<String> whitelist = new HashSet<>();
    private List<CustomRule> customRules = new ArrayList<>();

    private int webPort = DEFAULT_WEB_PORT;
    private boolean startMinimized = false;
    private boolean enableSystemTray = true;

    private AppConfig() {
        loadConfig();
    }

    /**
     * Returns singleton instance of AppConfig.
     * @return AppConfig instance
     */
    public static AppConfig getInstance() {
        if (instance == null) {
            synchronized (AppConfig.class) {
                if (instance == null) instance = new AppConfig();
            }
        }
        return instance;
    }

    /**
     * Loads configuration from disk or creates default if missing.
     */
    private void loadConfig() {
        if (!Files.exists(configPath)) {
            saveConfig();
            return;
        }

        try {
            AppConfig loaded = readConfigFromFile();
            if (loaded != null) applyLoadedConfig(loaded);
        } catch (Exception e) {
            System.err.println("Warning: Failed to load config. " + e.getMessage());
            saveConfig();
        }
    }

    /**
     * Reads config JSON from file.
     * @return deserialized AppConfig
     */
    private AppConfig readConfigFromFile() throws Exception {
        String json = Files.readString(configPath);
        return gson.fromJson(json, AppConfig.class);
    }

    /**
     * Applies loaded configuration values.
     * @param loaded loaded config object
     */
    private void applyLoadedConfig(AppConfig loaded) {
        scanIntervalSeconds = loaded.scanIntervalSeconds;
        cpuThreshold = loaded.cpuThreshold;
        memoryThreshold = loaded.memoryThreshold;

        blacklist.clear();
        blacklist.addAll(loaded.blacklist);

        whitelist.clear();
        whitelist.addAll(loaded.whitelist);

        webPort = loaded.webPort;
        startMinimized = loaded.startMinimized;
        enableSystemTray = loaded.enableSystemTray;

        customRules.clear();
        if (loaded.customRules != null) customRules.addAll(loaded.customRules);
    }

    /**
     * Saves current configuration to disk.
     */
    public void saveConfig() {
        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, gson.toJson(this));
        } catch (Exception e) {
            System.err.println("Error saving config: " + e.getMessage());
        }
    }

    /**
     * Returns scan interval in seconds.
     * @return scan interval
     */
    public int getScanIntervalSeconds() {
        return scanIntervalSeconds;
    }

    /**
     * Sets scan interval in seconds.
     * @param scanIntervalSeconds scan interval value
     */
    public void setScanIntervalSeconds(int scanIntervalSeconds) {
        this.scanIntervalSeconds = Math.max(MIN_SCAN_INTERVAL, scanIntervalSeconds);
        saveConfig();
    }

    /**
     * Returns CPU usage threshold.
     * @return CPU threshold
     */
    public double getCpuThreshold() {
        return cpuThreshold;
    }

    /**
     * Sets CPU usage threshold.
     * @param cpuThreshold CPU threshold value
     */
    public void setCpuThreshold(double cpuThreshold) {
        this.cpuThreshold = Math.max(MIN_THRESHOLD, cpuThreshold);
        saveConfig();
    }

    /**
     * Returns memory usage threshold.
     * @return memory threshold
     */
    public double getMemoryThreshold() {
        return memoryThreshold;
    }

    /**
     * Sets memory usage threshold.
     * @param memoryThreshold memory threshold value
     */
    public void setMemoryThreshold(double memoryThreshold) {
        this.memoryThreshold = Math.max(MIN_THRESHOLD, memoryThreshold);
        saveConfig();
    }

    /**
     * Returns blacklist copy.
     * @return blacklist set
     */
    public Set<String> getBlacklist() {
        return new HashSet<>(blacklist);
    }

    /**
     * Adds process name to blacklist.
     * @param processName process name
     */
    public void addToBlacklist(String processName) {
        if (isValid(processName)) {
            blacklist.add(processName.trim());
            saveConfig();
        }
    }

    /**
     * Removes process name from blacklist.
     * @param processName process name
     */
    public void removeFromBlacklist(String processName) {
        blacklist.remove(processName);
        saveConfig();
    }

    /**
     * Returns whitelist copy.
     * @return whitelist set
     */
    public Set<String> getWhitelist() {
        return new HashSet<>(whitelist);
    }

    /**
     * Adds process name to whitelist.
     * @param processName process name
     */
    public void addToWhitelist(String processName) {
        if (isValid(processName)) {
            whitelist.add(processName.trim());
            saveConfig();
        }
    }

    /**
     * Removes process name from whitelist.
     * @param processName process name
     */
    public void removeFromWhitelist(String processName) {
        whitelist.remove(processName);
        saveConfig();
    }

    /**
     * Returns web server port.
     * @return port number
     */
    public int getWebPort() {
        return webPort;
    }

    /**
     * Sets web server port.
     * @param webPort port number
     */
    public void setWebPort(int webPort) {
        this.webPort = (webPort > 1024 && webPort < 65536) ? webPort : DEFAULT_WEB_PORT;
        saveConfig();
    }

    /**
     * Returns whether app starts minimized.
     * @return true if minimized
     */
    public boolean isStartMinimized() {
        return startMinimized;
    }

    /**
     * Sets start minimized flag.
     * @param startMinimized flag
     */
    public void setStartMinimized(boolean startMinimized) {
        this.startMinimized = startMinimized;
        saveConfig();
    }

    /**
     * Returns whether system tray is enabled.
     * @return system tray flag
     */
    public boolean isEnableSystemTray() {
        return enableSystemTray;
    }

    /**
     * Sets system tray usage.
     * @param enableSystemTray flag
     */
    public void setEnableSystemTray(boolean enableSystemTray) {
        this.enableSystemTray = enableSystemTray;
        saveConfig();
    }

    /**
     * Returns custom rules list.
     * @return list of custom rules
     */
    public List<CustomRule> getCustomRules() {
        return new ArrayList<>(customRules);
    }

    /**
     * Replaces all custom rules.
     * @param customRules new rules list
     */
    public void setCustomRules(List<CustomRule> customRules) {
        this.customRules.clear();
        if (customRules != null) this.customRules.addAll(customRules);
        saveConfig();
    }

    /**
     * Adds a custom rule.
     * @param rule custom rule
     */
    public void addCustomRule(CustomRule rule) {
        if (rule != null) {
            customRules.add(rule);
            saveConfig();
        }
    }

    /**
     * Clears all custom rules.
     */
    public void clearCustomRules() {
        customRules.clear();
        saveConfig();
    }

    /**
     * Validates process name.
     * @param processName input string
     * @return true if valid
     */
    private boolean isValid(String processName) {
        return processName != null && !processName.isBlank();
    }
}