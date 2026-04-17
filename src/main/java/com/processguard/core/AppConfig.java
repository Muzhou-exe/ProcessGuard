package com.processguard.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.processguard.models.CustomRule;
import com.processguard.models.Severity;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Centralized configuration manager for ProcessGuard.
 * Implemented as a thread-safe Singleton with JSON persistence.
 * Matches section 2.5 of the Software Design Document (SDD).
 */
public class AppConfig {

    private static volatile AppConfig instance;

    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private final Path configPath = Paths.get(System.getProperty("user.home"), ".processguard", "config.json");

    // Configuration fields as defined in the SDD
    private int scanIntervalSeconds = 3;
    private double cpuThreshold = 20.0;
    private double memoryThreshold = 500.0;
    private final Set<String> blacklist = new HashSet<>();
    private final Set<String> whitelist = new HashSet<>();
    private int webPort = 8080;
    private boolean startMinimized = false;
    private boolean enableSystemTray = true;
    private List<CustomRule> customRules = new ArrayList<>();

    /**
     * Private constructor for Singleton pattern (synchronized lazy initialization).
     */
    private AppConfig() {
        loadConfig();
    }

    /**
     * Returns the single instance of AppConfig (thread-safe lazy initialization).
     * @return singleton instance of AppConfig
     */
    public static AppConfig getInstance() {
        if (instance == null) {
            synchronized (AppConfig.class) {
                if (instance == null) {
                    instance = new AppConfig();
                }
            }
        }
        return instance;
    }

    /**
     * Loads configuration from ~/.processguard/config.json or creates defaults if missing.
     */
    private void loadConfig() {
        try {
            if (Files.exists(configPath)) {
                String json = Files.readString(configPath);
                ConfigState loaded = gson.fromJson(json, ConfigState.class);
                if (loaded != null) {
                    this.scanIntervalSeconds = loaded.scanIntervalSeconds;
                    this.cpuThreshold = loaded.cpuThreshold;
                    this.memoryThreshold = loaded.memoryThreshold;

                    this.blacklist.clear();
                    if (loaded.blacklist != null) {
                        this.blacklist.addAll(loaded.blacklist);
                    }

                    this.whitelist.clear();
                    if (loaded.whitelist != null) {
                        this.whitelist.addAll(loaded.whitelist);
                    }

                    this.webPort = loaded.webPort;
                    this.startMinimized = loaded.startMinimized;
                    this.enableSystemTray = loaded.enableSystemTray;

                    this.customRules.clear();
                    if (loaded.customRules != null) {
                        this.customRules.addAll(loaded.customRules);
                    }
                }
            } else {
                saveConfig();
            }
        } catch (Exception e) {
            System.err.println("Warning: Failed to load config. Using defaults. Error: " + e.getMessage());
            saveConfig();
        }
    }

    /**
     * Saves current configuration to ~/.processguard/config.json.
     */
    public void saveConfig() {
        try {
            Files.createDirectories(configPath.getParent());
            ConfigState state = new ConfigState();

            state.scanIntervalSeconds = scanIntervalSeconds;
            state.cpuThreshold = cpuThreshold;
            state.memoryThreshold = memoryThreshold;
            state.blacklist = blacklist;
            state.whitelist = whitelist;
            state.webPort = webPort;
            state.startMinimized = startMinimized;
            state.enableSystemTray = enableSystemTray;
            state.customRules = customRules;

            String json = gson.toJson(state);
            Files.writeString(configPath, json);
        } catch (Exception e) {
            System.err.println("Error: Failed to save configuration. " + e.getMessage());
        }
    }

    /**
     * Returns scan interval in seconds.
     * @return scan interval seconds
     */
    public int getScanIntervalSeconds() {
        return scanIntervalSeconds;
    }

    /**
     * Sets scan interval in seconds.
     * @param scanIntervalSeconds interval value (minimum 1)
     */
    public void setScanIntervalSeconds(int scanIntervalSeconds) {
        this.scanIntervalSeconds = Math.max(1, scanIntervalSeconds);
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
     * @param cpuThreshold threshold value (minimum 0)
     */
    public void setCpuThreshold(double cpuThreshold) {
        this.cpuThreshold = Math.max(0.0, cpuThreshold);
        saveConfig();
    }

    /**
     * Returns memory usage threshold in MB.
     * @return memory threshold
     */
    public double getMemoryThreshold() {
        return memoryThreshold;
    }

    /**
     * Sets memory usage threshold in MB.
     * @param memoryThreshold threshold value (minimum 0)
     */
    public void setMemoryThreshold(double memoryThreshold) {
        this.memoryThreshold = Math.max(0.0, memoryThreshold);
        saveConfig();
    }

    /**
     * Returns blacklist copy.
     * @return set of blacklisted process names
     */
    public Set<String> getBlacklist() {
        return new HashSet<>(blacklist);
    }

    /**
     * Adds process to blacklist.
     * @param processName process name
     */
    public void addToBlacklist(String processName) {
        if (processName != null && !processName.isBlank()) {
            blacklist.add(processName.trim());
            saveConfig();
        }
    }

    /**
     * Removes process from blacklist.
     * @param processName process name
     */
    public void removeFromBlacklist(String processName) {
        blacklist.remove(processName);
        saveConfig();
    }

    /**
     * Returns whitelist copy.
     * @return set of whitelisted process names
     */
    public Set<String> getWhitelist() {
        return new HashSet<>(whitelist);
    }

    /**
     * Adds process to whitelist.
     * @param processName process name
     */
    public void addToWhitelist(String processName) {
        if (processName != null && !processName.isBlank()) {
            whitelist.add(processName.trim());
            saveConfig();
        }
    }

    /**
     * Removes process from whitelist.
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
     * @param webPort port (1024–65535)
     */
    public void setWebPort(int webPort) {
        this.webPort = (webPort > 1024 && webPort < 65536) ? webPort : 8080;
        saveConfig();
    }

    /**
     * Returns whether app starts minimized.
     * @return start minimized flag
     */
    public boolean isStartMinimized() {
        return startMinimized;
    }

    /**
     * Sets start minimized flag.
     * @param startMinimized flag value
     */
    public void setStartMinimized(boolean startMinimized) {
        this.startMinimized = startMinimized;
        saveConfig();
    }

    /**
     * Returns whether system tray is enabled.
     * @return system tray enabled flag
     */
    public boolean isEnableSystemTray() {
        return enableSystemTray;
    }

    /**
     * Sets system tray enabled flag.
     * @param enableSystemTray flag value
     */
    public void setEnableSystemTray(boolean enableSystemTray) {
        this.enableSystemTray = enableSystemTray;
        saveConfig();
    }

    /**
     * Returns custom rules copy.
     * @return list of custom rules
     */
    public List<CustomRule> getCustomRules() {
        return new ArrayList<>(customRules);
    }

    /**
     * Sets custom rules list.
     * @param customRules list of rules
     */
    public void setCustomRules(List<CustomRule> customRules) {
        this.customRules.clear();
        if (customRules != null) {
            this.customRules.addAll(customRules);
        }
        saveConfig();
    }

    /**
     * Adds a custom rule.
     * @param rule custom rule
     */
    public void addCustomRule(CustomRule rule) {
        if (rule != null) {
            this.customRules.add(rule);
            saveConfig();
        }
    }

    /**
     * Clears all custom rules and saves the configuration.
     */
    public void clearCustomRules() {
        this.customRules.clear();
        saveConfig();
    }
}