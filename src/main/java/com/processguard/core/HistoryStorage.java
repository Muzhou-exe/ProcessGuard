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

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private final Path snapshotPath = Paths.get(System.getProperty("user.home"), ".processguard", "history_snapshots.json");
    private final Path alertsPath = Paths.get(System.getProperty("user.home"), ".processguard", "history_alerts.json");

    private final List<ProcessInfo> snapshots = new ArrayList<>();
    private final List<AlertEvent> alerts = new ArrayList<>();

    public void saveSnapshot(List<ProcessInfo> currentSnapshot) {
        snapshots.clear();
        snapshots.addAll(currentSnapshot);

        try {
            Files.createDirectories(snapshotPath.getParent());
            Files.writeString(snapshotPath, gson.toJson(snapshots));
        } catch (Exception ignored) {
            // Silent fail - minimal implementation
        }
    }

    public synchronized void saveAlert(AlertEvent alert) {
        if (alerts.size() >= 1000) {
            alerts.remove(0);
        }

        alerts.add(alert);

        try {
            Files.createDirectories(alertsPath.getParent());
            Files.writeString(alertsPath, gson.toJson(alerts));
        } catch (Exception ignored) {
        }
    }

    public List<ProcessInfo> getRecentSnapshots() {
        return new ArrayList<>(snapshots);
    }

    public List<AlertEvent> getRecentAlerts() {
        return new ArrayList<>(alerts);
    }
}