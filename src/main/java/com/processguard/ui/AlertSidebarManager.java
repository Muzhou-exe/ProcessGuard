package com.processguard.ui;

import com.processguard.core.ProcessKiller;
import com.processguard.models.AlertEvent;
import com.processguard.models.ProcessInfo;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.scene.control.ScrollPane;

import java.util.Optional;

/**
 * Manages the alert sidebar UI and process interaction logic.
 */
public class AlertSidebarManager {

    private final VBox sidebar;
    private final ListView<AlertEvent> alertList;
    private final ObservableList<AlertEvent> alertData = FXCollections.observableArrayList();
    private final Button killButton;

    private final Label detailsLabel;
    private final ScrollPane detailsScroll;

    private ObservableList<ProcessInfo> masterData;

    private Long selectedPid;

    /**
     * Constructs the alert sidebar manager and initializes UI components.
     */
    public AlertSidebarManager() {
        this.sidebar = new VBox(10);
        this.alertList = new ListView<>();
        this.detailsLabel = new Label("Select a process from the table...");
        detailsScroll = new ScrollPane(detailsLabel);
        this.killButton = new Button("Kill Process");

        detailsScroll.setFitToWidth(true);
        detailsScroll.setFitToHeight(false);
        detailsScroll.setPrefViewportHeight(250);
        detailsScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        detailsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        detailsScroll.setStyle("-fx-background-color: transparent;");

        initializeSidebar();
    }

    /**
     * Initializes sidebar layout and UI components.
     */
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

        killButton.setDisable(true);
        killButton.setStyle("""
            -fx-background-color: #ff4d4d;
            -fx-text-fill: white;
            -fx-font-weight: bold;
        """);
        killButton.setOnAction(e -> killSelectedProcess());

        sidebar.getChildren().addAll(alertsTitle, alertList, detailsTitle, detailsScroll, killButton);
        sidebar.setPrefWidth(320);
        sidebar.setPadding(new Insets(10));
    }

    /**
     * Sets up custom cell rendering for alert list.
     */
    private void setupAlertCellFactory() {
        alertList.setCellFactory(lv -> new ListCell<>() {
            /**
             * Updates cell display for alert items.
             * @param alert alert event
             * @param empty whether cell is empty
             */
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

    /**
     * Sets master process data list.
     * @param data process list
     */
    public void setMasterData(ObservableList<ProcessInfo> data) {
        this.masterData = data;
    }

    /**
     * Adds alert to sidebar.
     * @param alert alert event
     */
    public void addAlert(AlertEvent alert) {
        Platform.runLater(() -> {
            alertData.add(0, alert);
            if (alertData.size() > 10) {
                alertData.remove(10, alertData.size());
            }
            // Note: throttling logic can be added here if still needed
        });
    }

    /**
     * Selects process and updates details panel.
     * @param p selected process
     */
    public void selectProcess(ProcessInfo p) {
        this.selectedPid = (p != null) ? p.getPid() : null;

        if (p == null) {
            killButton.setDisable(true);
            detailsLabel.setText("Select a process from the table...");
            return;
        }

        killButton.setDisable(false);
        updateDetailsPanel();
    }

    /**
     * Updates process details display.
     */
    private void updateDetailsPanel() {
        if (selectedPid == null) {
            detailsLabel.setText("Select a process from the table...");
            return;
        }

        // 🔥 IMPORTANT: fetch latest object
        ProcessInfo p = findLatestProcess();

        if (p == null) {
            detailsLabel.setText("Process no longer exists.");
            return;
        }

        StringBuilder text = new StringBuilder();

        text.append("""
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

        if (p.isFlagged()) {
            text.append("\n🚩 FLAGGED\n");
            text.append("Reason: ").append(p.getFlagReason()).append("\n");
        }

        detailsLabel.setText(text.toString());
    }

    /**
     * Finds latest process instance from master data.
     * @return latest process or null
     */
    private ProcessInfo findLatestProcess() {
        if (masterData == null) return null;

        return masterData.stream()
                .filter(p -> p.getPid() == selectedPid)
                .findFirst()
                .orElse(null);
    }

    /**
     * Kills currently selected process.
     */
    private void killSelectedProcess() {
        if (selectedPid == null) return;

        ProcessInfo p = findLatestProcess();
        if (p == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Process Kill");
        confirm.setHeaderText("Terminate Process?");
        confirm.setContentText("Are you sure you want to kill:\n\n" +
                p.getName() + " (PID " + p.getPid() + ")?\n\n" +
                "This action cannot be undone.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        killButton.setDisable(true);

        new Thread(() -> {
            boolean success = ProcessKiller.kill(p.getPid());
            Platform.runLater(() -> {
                String suffix = success ? "\n\n Process killed successfully" : "\n\n Failed to kill process";
                detailsLabel.setText(detailsLabel.getText() + suffix);
                selectedPid = null;
                killButton.setDisable(true);
            });
        }).start();
    }

    /**
     * Returns sidebar UI component.
     * @return sidebar container
     */
    public VBox getSidebar() {
        return sidebar;
    }

    /**
     * Kills process from external caller.
     * @param p process to kill
     */
    public void killProcess(ProcessInfo p) {
        // Called from table context menu
        selectProcess(p);
        killSelectedProcess();
    }
}