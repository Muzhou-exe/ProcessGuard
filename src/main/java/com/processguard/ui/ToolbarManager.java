package com.processguard.ui;

import com.processguard.ProcessGuardMain;
import com.processguard.core.ProcessMonitor;
import com.processguard.core.ReportExporter;
import com.processguard.models.ProcessInfo;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.stage.Stage;
import javafx.scene.layout.HBox;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ToolbarManager {

    public ToolBar createToolbar(Stage primaryStage, ProcessTableManager tableManager) {
        ToolBar toolbar = new ToolBar();

        Button btnStart = new Button("Start Monitoring");
        Button btnStop = new Button("Stop Monitoring");
        Button btnRefresh = new Button("Refresh Now");
        Button btnConfig = new Button("Open Configuration");
        Button btnExport = new Button("Export Report");
        Button closeBtn = new Button("Close App");

        closeBtn.setOnAction(e -> primaryStage.close());

        closeBtn.setStyle(
                "-fx-background-color: #e74c3c;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;"
        );

        ProcessMonitor processMonitor = ProcessGuardMain.processMonitor;

        btnStart.setOnAction(e -> {
            if (processMonitor != null && !processMonitor.isRunning()) {
                processMonitor.start();
            }
        });

        btnStop.setOnAction(e -> {
            if (processMonitor != null && processMonitor.isRunning()) {
                processMonitor.stop();
            }
        });

        btnRefresh.setOnAction(e -> {
            if (processMonitor != null) {
                processMonitor.scanNow();
            }
        });

        // Open Configuration - RuleManagerDialog is in the same package
        btnConfig.setOnAction(e -> {
            RuleManagerDialog dialog = new RuleManagerDialog(primaryStage);
            dialog.show();
        });

        // ====================== Export Report (PDF) ======================
        btnExport.setOnAction(e -> {
            List<ProcessInfo> snapshot = tableManager.getCurrentData();
            List<com.processguard.models.AlertEvent> alerts =
                    ProcessGuardMain.historyStorage != null
                            ? ProcessGuardMain.historyStorage.getRecentAlerts()
                            : List.of();

            if (snapshot.isEmpty() && alerts.isEmpty()) {
                Alert info = new Alert(Alert.AlertType.INFORMATION);
                info.setTitle("Export Report");
                info.setHeaderText(null);
                info.setContentText("No data available to export.");
                info.showAndWait();
                return;
            }

            new Thread(() -> {
                try {
                    Path reportPath = ReportExporter.export(
                            new ArrayList<>(snapshot),
                            new ArrayList<>(alerts)
                    );

                    Platform.runLater(() -> {
                        Alert success = new Alert(Alert.AlertType.INFORMATION);
                        success.setTitle("Report Exported");
                        success.setHeaderText("Report saved successfully");
                        success.setContentText("File location:\n" + reportPath.toAbsolutePath());
                        success.showAndWait();
                    });

                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        Alert error = new Alert(Alert.AlertType.ERROR);
                        error.setTitle("Export Failed");
                        error.setHeaderText("Could not generate report");
                        error.setContentText(ex.getMessage() != null
                                ? ex.getMessage()
                                : ex.toString());
                        error.showAndWait();
                    });
                }
            }).start();
        });

        toolbar.getItems().addAll(btnStart, btnStop, btnRefresh, btnConfig, btnExport, closeBtn);
        return toolbar;
    }
}