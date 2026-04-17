package com.processguard.ui;

import com.processguard.core.ProcessKiller;
import com.processguard.models.AlertEvent;
import com.processguard.models.ProcessInfo;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Optional;

public class AlertSidebarManager {

    private final VBox sidebar;
    private final ListView<AlertEvent> alertList;
    private final ObservableList<AlertEvent> alertData = FXCollections.observableArrayList();
    private final Label detailsLabel;
    private final Button killButton;

    private ProcessInfo selectedProcess;

    public AlertSidebarManager() {
        this.sidebar = new VBox(10);
        this.alertList = new ListView<>();
        this.detailsLabel = new Label("Select a process from the table...");
        this.killButton = new Button("Kill Process");

        initializeSidebar();
    }

    private void initializeSidebar() {
        alertList.setItems(alertData);
        setupAlertCellFactory();

        Label alertsTitle = new Label("Alerts");
        alertsTitle.setStyle("-fx-font-weight: bold;");

        Label detailsTitle = new Label("Process Details");
        detailsTitle.setStyle("-fx-font-weight: bold;");

        detailsLabel.setWrapText(true);
        detailsLabel.setPadding(new Insets(10));
        detailsLabel.setStyle("""
            -fx-font-family: monospace;
            -fx-border-color: lightgray;
            -fx-border-width: 1;
            -fx-background-color: #fafafa;
        """);
        detailsLabel.setPrefHeight(250);

        killButton.setDisable(true);
        killButton.setStyle("""
            -fx-background-color: #ff4d4d;
            -fx-text-fill: white;
            -fx-font-weight: bold;
        """);
        killButton.setOnAction(e -> killSelectedProcess());

        sidebar.getChildren().addAll(alertsTitle, alertList, detailsTitle, detailsLabel, killButton);
        sidebar.setPrefWidth(320);
        sidebar.setPadding(new Insets(10));
    }

    private void setupAlertCellFactory() {
        alertList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(AlertEvent alert, boolean empty) {
                super.updateItem(alert, empty);
                if (empty || alert == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                StringBuilder display = new StringBuilder();
                String time = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
                        .format(alert.getTimestamp().atZone(java.time.ZoneId.systemDefault()));

                display.append("[").append(time).append("] ");

                switch (alert.getSeverity()) {
                    case HIGH -> {
                        display.append("🔴 HIGH | ");
                        setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
                    }
                    case MEDIUM -> {
                        display.append("🟠 MEDIUM | ");
                        setStyle("-fx-text-fill: #f57c00;");
                    }
                    default -> {
                        display.append("🟡 LOW | ");
                        setStyle("-fx-text-fill: #1976d2;");
                    }
                }

                display.append(alert.getProcess().getName());

                if (alert.getTriggeringRule() != null) {
                    display.append("  →  Rule: ").append(alert.getTriggeringRule().getName());
                }
                if (alert.getMessage() != null && !alert.getMessage().isBlank()) {
                    display.append(" - ").append(alert.getMessage());
                }

                setText(display.toString());
            }
        });
    }

    public void addAlert(AlertEvent alert) {
        Platform.runLater(() -> {
            alertData.add(0, alert);
            if (alertData.size() > 10) {
                alertData.remove(10, alertData.size());
            }
            // Note: throttling logic can be added here if still needed
        });
    }

    public void selectProcess(ProcessInfo p) {
        this.selectedProcess = p;
        if (p == null) {
            killButton.setDisable(true);
            detailsLabel.setText("Select a process from the table...");
            return;
        }

        killButton.setDisable(false);
        updateDetailsPanel(p);
    }

    private void updateDetailsPanel(ProcessInfo p) {
        detailsLabel.setText("""
            PID: %d
            Name: %s
            Path: %s
            CPU: %.1f%%
            Memory: %d MB
            Parent PID: %d
            Status: %s
            """.formatted(
                p.getPid(), p.getName(), p.getExecutablePath(),
                p.getCpuUsage(), p.getMemoryUsageMB(),
                p.getParentPid(), p.getStatus()
        ));
    }

    private void killSelectedProcess() {
        if (selectedProcess == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Process Kill");
        confirm.setHeaderText("Terminate Process?");
        confirm.setContentText("Are you sure you want to kill:\n\n" +
                selectedProcess.getName() + " (PID " + selectedProcess.getPid() + ")?\n\n" +
                "This action cannot be undone.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        killButton.setDisable(true);

        new Thread(() -> {
            boolean success = ProcessKiller.kill(selectedProcess.getPid());
            Platform.runLater(() -> {
                String suffix = success ? "\n\n Process killed successfully" : "\n\n Failed to kill process";
                detailsLabel.setText(detailsLabel.getText() + suffix);
                selectedProcess = null;
                killButton.setDisable(true);
            });
        }).start();
    }

    public VBox getSidebar() {
        return sidebar;
    }

    public void killProcess(ProcessInfo p) {
        // Called from table context menu
        selectProcess(p);
        killSelectedProcess();
    }
}