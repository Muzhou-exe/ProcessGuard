package com.processguard.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.processguard.models.AlertEvent;
import com.processguard.models.ProcessInfo;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists snapshots and alerts to JSON files (No SQL as requested).
 * Replaces SQLite with pure Java JSON storage.
 * Matches section 2.6 of the SDD with JSON adaptation.
 */
public class HistoryStorage {

    private static final int MAX_ALERT_HISTORY = 1000;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private final Path snapshotPath = Paths.get(System.getProperty("user.home"), ".processguard", "history_snapshots.json");
    private final Path alertsPath = Paths.get(System.getProperty("user.home"), ".processguard", "history_alerts.json");

    private final List<ProcessInfo> snapshots = new ArrayList<>();
    private final List<AlertEvent> alerts = new ArrayList<>();

    /**
     * Saves current process snapshot to memory and disk.
     * @param currentSnapshot list of current system processes
     */
    public synchronized void saveSnapshot(List<ProcessInfo> currentSnapshot) {
        snapshots.clear();
        snapshots.addAll(currentSnapshot);

        try {
            Files.createDirectories(snapshotPath.getParent());
            Files.writeString(snapshotPath, gson.toJson(snapshots));
        } catch (Exception ignored) {
        }
    }

    /**
     * Saves alert event to memory and disk with size limit enforcement.
     * @param alert alert event to store
     */
    public synchronized void saveAlert(AlertEvent alert) {
        if (alerts.size() >= MAX_ALERT_HISTORY) {
            alerts.remove(0);
        }

        alerts.add(alert);

        try {
            Files.createDirectories(alertsPath.getParent());
            Files.writeString(alertsPath, gson.toJson(alerts));
        } catch (Exception ignored) {
        }
    }

    /**
     * Returns list of stored snapshots.
     * @return list of process snapshots
     */
    public synchronized List<ProcessInfo> getRecentSnapshots() {
        return new ArrayList<>(snapshots);
    }

    /**
     * Returns list of stored alerts.
     * @return list of alert events
     */
    public synchronized List<AlertEvent> getRecentAlerts() {
        return new ArrayList<>(alerts);
    }
}