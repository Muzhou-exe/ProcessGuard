package com.processguard.ui;

import com.processguard.core.AppConfig;
import com.processguard.models.ProcessInfo;
import com.processguard.models.Status;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Manages the bottom status bar UI in the ProcessGuard dashboard.
 * Displays real-time system summary metrics such as CPU usage, memory usage, and process counts.
 */
public class StatusBarManager {

    private final HBox statusBar = new HBox(10);
    private final Label scanIntervalLabel = new Label("Scan Interval: 3s");
    private final Label totalProcessesLabel = new Label("Processes: 0");
    private final Label lastScanLabel = new Label("Last Scan: N/A");
    private final Label cpuSummaryLabel = new Label("CPU: 0%");
    private final Label memSummaryLabel = new Label("Memory: 0MB");

    /**
     * Initializes the status bar UI components and layout.
     */
    public StatusBarManager() {
        statusBar.setPadding(new Insets(5));
        statusBar.getChildren().addAll(
                scanIntervalLabel, totalProcessesLabel, lastScanLabel,
                cpuSummaryLabel, memSummaryLabel
        );
    }

    /**
     * Updates the status bar using the latest process snapshot.
     * @param snapshot list of current running processes
     */
    public void updateStatus(List<ProcessInfo> snapshot) {
        totalProcessesLabel.setText("Processes: " + snapshot.size());

        double totalCpu = snapshot.stream().mapToDouble(ProcessInfo::getCpuUsage).sum();
        long totalMem = snapshot.stream().mapToLong(ProcessInfo::getMemoryUsageMB).sum();

        long suspiciousCount = snapshot.stream()
                .filter(p -> p.getStatus() == Status.SUSPICIOUS ||
                        p.getCpuUsage() > AppConfig.getInstance().getCpuThreshold() ||
                        p.getMemoryUsageMB() > AppConfig.getInstance().getMemoryThreshold())
                .count();

        long blockedCount = snapshot.stream()
                .filter(p -> p.getStatus() == Status.BLOCKED)
                .count();

        cpuSummaryLabel.setText(String.format("CPU: %.1f%%", totalCpu));
        memSummaryLabel.setText("Memory: " + totalMem + " MB");
        lastScanLabel.setText("Last Scan: " +
                DateTimeFormatter.ofPattern("HH:mm:ss").format(LocalTime.now()));
        scanIntervalLabel.setText(String.format("Suspicious: %d | Blocked: %d",
                suspiciousCount, blockedCount));
    }

    /**
     * Returns the JavaFX status bar container node.
     * @return HBox containing status labels
     */
    public HBox getStatusBar() {
        return statusBar;
    }
}