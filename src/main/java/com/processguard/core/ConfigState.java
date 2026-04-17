package com.processguard.core;

import com.processguard.models.CustomRule;
import java.util.List;
import java.util.Set;

/**
 * Serializable configuration state used for JSON persistence in AppConfig.
 */
public class ConfigState {

    public int scanIntervalSeconds;
    public double cpuThreshold;
    public double memoryThreshold;
    public Set<String> blacklist;
    public Set<String> whitelist;
    public int webPort;
    public boolean startMinimized;
    public boolean enableSystemTray;
    public List<CustomRule> customRules;
}