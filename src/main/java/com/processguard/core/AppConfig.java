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

    // Private constructor for Singleton pattern (synchronized lazy initialization)
    private AppConfig() {
        loadConfig();
    }

    /**
     * Returns the single instance of AppConfig (thread-safe lazy initialization).
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
     * Loads configuration from ~/.processguard/config.json.
     * Creates default config if file does not exist.
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
                    this.blacklist.addAll(loaded.blacklist);

                    this.whitelist.clear();
                    this.whitelist.addAll(loaded.whitelist);

                    this.webPort = loaded.webPort;
                    this.startMinimized = loaded.startMinimized;
                    this.enableSystemTray = loaded.enableSystemTray;

                    this.customRules.clear();
                    this.customRules.addAll(loaded.customRules);
                }
            } else {
                // Create default configuration and save it
                saveConfig();
            }
        } catch (Exception e) {
            System.err.println("Warning: Failed to load config. Using defaults. Error: " + e.getMessage());
            saveConfig(); // Save defaults on failure
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

    // -------------------------------------------------------------------------
    // Getters and Setters (with automatic save on mutation where appropriate)
    // -------------------------------------------------------------------------

    public int getScanIntervalSeconds() {
        return scanIntervalSeconds;
    }

    public void setScanIntervalSeconds(int scanIntervalSeconds) {
        this.scanIntervalSeconds = Math.max(1, scanIntervalSeconds);
        saveConfig();
    }

    public double getCpuThreshold() {
        return cpuThreshold;
    }

    public void setCpuThreshold(double cpuThreshold) {
        this.cpuThreshold = Math.max(0.0, cpuThreshold);
        saveConfig();
    }

    public double getMemoryThreshold() {
        return memoryThreshold;
    }

    public void setMemoryThreshold(double memoryThreshold) {
        this.memoryThreshold = Math.max(0.0, memoryThreshold);
        saveConfig();
    }

    public Set<String> getBlacklist() {
        return new HashSet<>(blacklist); // defensive copy
    }

    public void addToBlacklist(String processName) {
        if (processName != null && !processName.isBlank()) {
            blacklist.add(processName.trim());
            saveConfig();
        }
    }

    public void removeFromBlacklist(String processName) {
        blacklist.remove(processName);
        saveConfig();
    }

    public Set<String> getWhitelist() {
        return new HashSet<>(whitelist); // defensive copy
    }

    public void addToWhitelist(String processName) {
        if (processName != null && !processName.isBlank()) {
            whitelist.add(processName.trim());
            saveConfig();
        }
    }

    public void removeFromWhitelist(String processName) {
        whitelist.remove(processName);
        saveConfig();
    }

    public int getWebPort() {
        return webPort;
    }

    public void setWebPort(int webPort) {
        this.webPort = (webPort > 1024 && webPort < 65536) ? webPort : 8080;
        saveConfig();
    }

    public boolean isStartMinimized() {
        return startMinimized;
    }

    public void setStartMinimized(boolean startMinimized) {
        this.startMinimized = startMinimized;
        saveConfig();
    }

    public boolean isEnableSystemTray() {
        return enableSystemTray;
    }

    public void setEnableSystemTray(boolean enableSystemTray) {
        this.enableSystemTray = enableSystemTray;
        saveConfig();
    }

    public List<CustomRule> getCustomRules() {
        return new ArrayList<>(customRules); // defensive copy
    }

    public void setCustomRules(List<CustomRule> customRules) {
        this.customRules.clear();
        if (customRules != null) {
            this.customRules.addAll(customRules);
        }
        saveConfig();
    }

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
