package com.processguard.ui;

// Project-specific imports
import com.processguard.ProcessGuardMain;
import com.processguard.core.ProcessMonitor;
import com.processguard.listeners.AlertListener;
import com.processguard.listeners.ProcessListener;
import com.processguard.models.AlertEvent;
import com.processguard.models.ProcessInfo;
import com.processguard.core.AlertEngine;
import com.processguard.core.HistoryStorage;
import com.processguard.models.Status;
import com.processguard.core.AppConfig;
import com.processguard.core.ProcessKiller;
import com.processguard.ui.RuleManagerDialog;

// JavaFX imports
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.input.DataFormat;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ListCell;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.binding.Bindings;

// Java standard library
import java.time.Instant;
import java.time.ZoneId;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Comparator;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class MainDashboard extends Application implements ProcessListener, AlertListener {

    private TableView<ProcessInfo> processTable;
    private ObservableList<ProcessInfo> processData;

    private ListView<AlertEvent> alertList;
    private ObservableList<AlertEvent> alertData;
    ObservableList<ProcessInfo> masterData;

    private Label scanIntervalLabel, totalProcessesLabel, lastScanLabel, cpuSummaryLabel, memSummaryLabel, detailsLabel;

    private ProcessMonitor processMonitor;
    private AlertEngine alertEngine;
    private HistoryStorage historyStorage;

    private ProcessInfo selectedProcess;
    private Button killButton;

    private VBox alertSidebar;

    private long lastAlertUIUpdate = 0;
    private static final long ALERT_UI_THROTTLE_MS = 30_000;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

        // Register backend references FIRST
        this.historyStorage = ProcessGuardMain.historyStorage;
        this.processMonitor = ProcessGuardMain.processMonitor;
        this.alertEngine = ProcessGuardMain.alertEngine;

        // =========================
        // TABLE SETUP
        // =========================
        processTable = new TableView<>();
        masterData = FXCollections.observableArrayList();

        SortedList<ProcessInfo> sortedData = new SortedList<>(masterData);
        sortedData.comparatorProperty().bind(processTable.comparatorProperty());
        processTable.setItems(sortedData);

        // =========================
        // TOOLBAR FIRST
        // =========================
        ToolBar toolbar = new ToolBar();
        Button btnStart = new Button("Start Monitoring");
        Button btnStop = new Button("Stop Monitoring");
        Button btnRefresh = new Button("Refresh Now");
        Button btnConfig = new Button("Open Configuration");
        Button btnExport = new Button("Export Report");

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

        btnConfig.setOnAction(e -> {
            RuleManagerDialog dialog = new RuleManagerDialog(primaryStage);
            dialog.show();
        });

        toolbar.getItems().addAll(
                btnStart, btnStop, btnRefresh, btnConfig, btnExport
        );

        // =========================
        // COLUMNS
        // =========================
        String[] colNames = {
                "PID", "Process Name", "Executable Path",
                "CPU %", "Memory MB", "Status",
                "Parent PID", "Start Time", "Captured At"
        };

        String[] propertyNames = {
                "pid", "name", "executablePath",
                "cpuUsage", "memoryUsageMB", "status",
                "parentPid", "startTime", "capturedAt"
        };

        Map<String, TableColumn<ProcessInfo, ?>> columnMap = new HashMap<>();

        for (int i = 0; i < colNames.length; i++) {
            String prop = propertyNames[i];
            String name = colNames[i];

            switch (prop) {
                case "cpuUsage" -> {
                    TableColumn<ProcessInfo, Double> col = new TableColumn<>(name);
                    col.setCellValueFactory(new PropertyValueFactory<>("cpuUsage"));
                    col.setCellFactory(tc -> new TableCell<>() {
                        @Override
                        protected void updateItem(Double item, boolean empty) {
                            super.updateItem(item, empty);
                            setText(empty || item == null ? "" :
                                    String.format("%.1f %%", item));
                        }
                    });
                    col.setComparator(Double::compare);
                    columnMap.put(prop, col);
                }

                case "memoryUsageMB" -> {
                    TableColumn<ProcessInfo, Long> col = new TableColumn<>(name);
                    col.setCellValueFactory(new PropertyValueFactory<>("memoryUsageMB"));
                    col.setCellFactory(tc -> new TableCell<>() {
                        @Override
                        protected void updateItem(Long item, boolean empty) {
                            super.updateItem(item, empty);
                            setText(empty || item == null ? "" : item + " MB");
                        }
                    });
                    col.setComparator(Long::compare);
                    columnMap.put(prop, col);
                }

                default -> {
                    TableColumn<ProcessInfo, Object> col = new TableColumn<>(name);
                    col.setCellValueFactory(new PropertyValueFactory<>(prop));
                    columnMap.put(prop, col);
                }
            }
        }

        processTable.getColumns().addAll(columnMap.values());

        // Default sorting
        TableColumn<ProcessInfo, Double> cpuCol =
                (TableColumn<ProcessInfo, Double>) columnMap.get("cpuUsage");

        if (cpuCol != null) {
            cpuCol.setSortType(TableColumn.SortType.DESCENDING);
            processTable.getSortOrder().add(cpuCol);
            processTable.sort();
        }

        // =========================
        // ROW FACTORY
        // =========================
        processTable.setRowFactory(tv -> {
            TableRow<ProcessInfo> row = new TableRow<>();

            // =========================
            // RIGHT CLICK MENU
            // =========================
            ContextMenu contextMenu = new ContextMenu();

            MenuItem killItem = new MenuItem("Kill Process");
            MenuItem copyPidItem = new MenuItem("Copy PID");
            MenuItem detailsItem = new MenuItem("View Details");

            // -------------------------
            // Drag for custom rules
            // -------------------------
            row.setOnDragDetected(e -> {
                if (row.isEmpty()) {
                    return;
                }

                ProcessInfo p = row.getItem();
                if (p == null) {
                    return;
                }

                Dragboard db = row.startDragAndDrop(TransferMode.COPY);
                ClipboardContent content = new ClipboardContent();

                // Primary string (used by the drop handler in RuleManagerDialog)
                content.putString(p.getName());

                // Custom data using proper DataFormat
                content.put(new DataFormat("process/pid"), p.getPid());
                content.put(new DataFormat("process/cpu"), p.getCpuUsage());
                content.put(new DataFormat("process/mem"), p.getMemoryUsageMB());
                content.put(new DataFormat("process/name"), p.getName());   // optional but useful

                db.setContent(content);
                e.consume();
            });

            // -------------------------
            // Kill process
            // -------------------------
            killItem.setOnAction(e -> {
                ProcessInfo p = row.getItem();
                if (p == null) return;

                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Confirm Kill");
                confirm.setHeaderText("Kill Process");
                confirm.setContentText(
                        "Kill " + p.getName() + " (PID " + p.getPid() + ")?"
                );

                Optional<ButtonType> result = confirm.showAndWait();
                if (result.isEmpty() || result.get() != ButtonType.OK) return;

                new Thread(() -> ProcessKiller.kill(p.getPid())).start();
            });

            // -------------------------
            // Copy PID
            // -------------------------
            copyPidItem.setOnAction(e -> {
                ProcessInfo p = row.getItem();
                if (p == null) return;

                ClipboardContent content = new ClipboardContent();
                content.putString(String.valueOf(p.getPid()));
                Clipboard.getSystemClipboard().setContent(content);
            });

            // -------------------------
            // View details
            // -------------------------
            detailsItem.setOnAction(e -> {
                ProcessInfo p = row.getItem();
                if (p == null) return;

                selectedProcess = p;
                killButton.setDisable(false);
                updateDetailsPanel(p);
            });

            contextMenu.getItems().addAll(killItem, copyPidItem, detailsItem);

            // only show menu for non-empty rows
            row.contextMenuProperty().bind(
                    Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(contextMenu)
            );

            // =========================
            // ROW CLICK -> DETAILS PANEL
            // =========================
            row.setOnMouseClicked(e -> {
                if (!row.isEmpty()) {
                    selectedProcess = row.getItem();
                    killButton.setDisable(false);
                    updateDetailsPanel(selectedProcess);
                }
            });

            // =========================
            // HIGHLIGHTING
            // =========================
            row.itemProperty().addListener((obs, oldItem, item) -> {
                row.setStyle("");

                if (item == null) return;

                double cpuThreshold = AppConfig.getInstance().getCpuThreshold();
                double memThreshold = AppConfig.getInstance().getMemoryThreshold();

                Status effectiveStatus = item.getStatus();

                if (effectiveStatus == Status.NORMAL &&
                        (item.getCpuUsage() > cpuThreshold ||
                                item.getMemoryUsageMB() > memThreshold)) {
                    effectiveStatus = Status.SUSPICIOUS;
                }

                switch (effectiveStatus) {
                    case BLOCKED ->
                            row.setStyle("-fx-background-color: rgba(255,0,0,0.3);");
                    case SUSPICIOUS ->
                            row.setStyle("-fx-background-color: rgba(255,255,0,0.3);");
                    default ->
                            row.setStyle("");
                }
            });

            return row;
        });

        processTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, selected) -> {

                    selectedProcess = selected;

                    if (selected == null) {
                        killButton.setDisable(true);
                        detailsLabel.setText("Select a process from the table...");
                        return;
                    }

                    killButton.setDisable(false);

                    detailsLabel.setText(String.format("""
                    PID: %d
                    Name: %s
                    Path: %s
                    CPU: %.1f%%
                    Memory: %d MB
                    Parent PID: %d
                    Status: %s
                    """,
                            selected.getPid(),
                            selected.getName(),
                            selected.getExecutablePath(),
                            selected.getCpuUsage(),
                            selected.getMemoryUsageMB(),
                            selected.getParentPid(),
                            selected.getStatus()
                    ));
                }
        );

        // =========================
        // ALERT + DETAILS SIDEBAR
        // =========================

        alertList = new ListView<>();
        alertData = FXCollections.observableArrayList();
        alertList.setItems(alertData);

        Label alertsTitle = new Label("Alerts");
        alertsTitle.setStyle("-fx-font-weight: bold;");

        Label detailsTitle = new Label("Process Details");
        detailsTitle.setStyle("-fx-font-weight: bold;");

        // ====================== IMPROVED ALERT LIST RENDERING ======================
        alertList.setCellFactory(lv -> new ListCell<AlertEvent>() {
            @Override
            protected void updateItem(AlertEvent alert, boolean empty) {
                super.updateItem(alert, empty);

                if (empty || alert == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                StringBuilder display = new StringBuilder();

                // Timestamp
                String time = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
                        .format(alert.getTimestamp().atZone(java.time.ZoneId.systemDefault()));

                display.append("[").append(time).append("] ");

                // Severity with color
                switch (alert.getSeverity()) {
                    case HIGH:
                        display.append("🔴 HIGH | ");
                        setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
                        break;
                    case MEDIUM:
                        display.append("🟠 MEDIUM | ");
                        setStyle("-fx-text-fill: #f57c00;");
                        break;
                    default:
                        display.append("🟡 LOW | ");
                        setStyle("-fx-text-fill: #1976d2;");
                        break;
                }

                // Process name
                display.append(alert.getProcess().getName());

                // Show rule name if this alert came from a custom rule
                if (alert.getTriggeringRule() != null) {
                    display.append("  →  Rule: ").append(alert.getTriggeringRule().getName());
                }

                // Main message
                if (alert.getMessage() != null && !alert.getMessage().isBlank()) {
                    display.append(" - ").append(alert.getMessage());
                }

                setText(display.toString());
            }
        });

        detailsLabel = new Label("Select a process from the table...");
        detailsLabel.setWrapText(true);
        detailsLabel.setPadding(new Insets(10));
        detailsLabel.setStyle("""
            -fx-font-family: monospace;
            -fx-border-color: lightgray;
            -fx-border-width: 1;
            -fx-background-color: #fafafa;
        """);
        detailsLabel.setPrefHeight(250);

        // Kill button
        killButton = new Button("Kill Process");
        killButton.setDisable(true);

        killButton.setStyle("""
            -fx-background-color: #ff4d4d;
            -fx-text-fill: white;
            -fx-font-weight: bold;
        """);

        killButton.setOnAction(e -> {
            if (selectedProcess == null) return;

            long pid = selectedProcess.getPid();
            String name = selectedProcess.getName();

            // =========================
            // CONFIRMATION DIALOG
            // =========================
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Process Kill");
            confirm.setHeaderText("Terminate Process?");
            confirm.setContentText(
                    "Are you sure you want to kill:\n\n" +
                            name + " (PID " + pid + ")?\n\n" +
                            "This action cannot be undone."
            );

            Optional<ButtonType> result = confirm.showAndWait();

            if (result.isEmpty() || result.get() != ButtonType.OK) {
                return; // user cancelled
            }

            killButton.setDisable(true);

            // =========================
            // KILL PROCESS (background thread)
            // =========================
            new Thread(() -> {
                boolean success = ProcessKiller.kill(pid);

                Platform.runLater(() -> {
                    if (success) {
                        detailsLabel.setText(detailsLabel.getText()
                                + "\n\n Process killed successfully");
                    } else {
                        detailsLabel.setText(detailsLabel.getText()
                                + "\n\n Failed to kill process");
                    }

                    selectedProcess = null;
                    killButton.setDisable(true);
                });
            }).start();
        });

        alertSidebar = new VBox(
                10,
                alertsTitle,
                alertList,
                detailsTitle,
                detailsLabel,
                killButton
        );

        alertSidebar.setPrefWidth(320);
        alertSidebar.setPadding(new Insets(10));

        // =========================
        // STATUS BAR
        // =========================
        HBox statusBar = new HBox(10);
        statusBar.setPadding(new Insets(5));

        scanIntervalLabel = new Label("Scan Interval: 3s");
        totalProcessesLabel = new Label("Processes: 0");
        lastScanLabel = new Label("Last Scan: N/A");
        cpuSummaryLabel = new Label("CPU: 0%");
        memSummaryLabel = new Label("Memory: 0MB");

        statusBar.getChildren().addAll(
                scanIntervalLabel,
                totalProcessesLabel,
                lastScanLabel,
                cpuSummaryLabel,
                memSummaryLabel
        );

        // =========================
        // MAIN LAYOUT
        // =========================
        BorderPane root = new BorderPane();
        root.setTop(toolbar);
        root.setCenter(processTable);
        root.setRight(alertSidebar);
        root.setBottom(statusBar);

        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setScene(scene);
        primaryStage.setTitle("ProcessGuard v1.6 – Live Process Monitor");
        primaryStage.show();

        processMonitor.addListener(this);
        alertEngine.addAlertListener(this);
        ProcessGuardMain.customRuleEngine.addAlertListener(this);
    }

    // Helper to convert column names to camelCase for PropertyValueFactory
    private String camelCase(String col) {
        return col.toLowerCase().replace(" ", "");
    }

    // --- Observer Callbacks ---
    @Override
    public void onSnapshotUpdate(List<ProcessInfo> snapshot) {
        Platform.runLater(() -> {

            // Update live table
            masterData.setAll(snapshot);

            // =========================
            // Bottom bar metrics
            // =========================
            totalProcessesLabel.setText("Processes: " + snapshot.size());

            double totalCpu = snapshot.stream()
                    .mapToDouble(ProcessInfo::getCpuUsage)
                    .sum();

            long totalMem = snapshot.stream()
                    .mapToLong(ProcessInfo::getMemoryUsageMB)
                    .sum();

            long suspiciousCount = snapshot.stream()
                    .filter(p ->
                            p.getStatus() == Status.SUSPICIOUS ||
                                    p.getCpuUsage() > AppConfig.getInstance().getCpuThreshold() ||
                                    p.getMemoryUsageMB() > AppConfig.getInstance().getMemoryThreshold()
                    )
                    .count();

            long blockedCount = snapshot.stream()
                    .filter(p -> p.getStatus() == Status.BLOCKED)
                    .count();

            cpuSummaryLabel.setText(String.format(
                    "CPU: %.1f%%", totalCpu
            ));

            memSummaryLabel.setText(
                    "Memory: " + totalMem + " MB"
            );

            lastScanLabel.setText(
                    "Last Scan: " +
                            DateTimeFormatter.ofPattern("HH:mm:ss")
                                    .format(LocalTime.now())
            );

            scanIntervalLabel.setText(
                    String.format(
                            "Suspicious: %d | Blocked: %d",
                            suspiciousCount,
                            blockedCount
                    )
            );
        });
    }

    @Override
    public void onAlert(AlertEvent alert) {
        Platform.runLater(() -> {
            alertData.add(0, alert);

            // Keep only latest 10 alerts
            if (alertData.size() > 10) {
                alertData.remove(10, alertData.size());
            }

            // Throttle UI refresh to every 30 seconds to avoid too-fast updates
            long now = System.currentTimeMillis();
            if (now - lastAlertUIUpdate > ALERT_UI_THROTTLE_MS) {
                alertList.scrollTo(0);
                lastAlertUIUpdate = now;
            }
        });
    }

    @Override
    public void onExitedProcesses(List<ProcessInfo> exitedProcesses) {
        Platform.runLater(() -> {
            masterData.removeAll(exitedProcesses);
        });
    }

    @Override
    public void onNewProcesses(List<ProcessInfo> newProcesses) {
        Platform.runLater(() -> {
            masterData.addAll(newProcesses);
        });
    }

    private void updateDetailsPanel(ProcessInfo p) {
        if (p == null) {
            detailsLabel.setText("Select a process from the table...");
            return;
        }

        detailsLabel.setText("""
        PID: %d
        Name: %s
        Path: %s
        CPU: %.1f%%
        Memory: %d MB
        Parent PID: %d
        Status: %s
        """.formatted(
                p.getPid(),
                p.getName(),
                p.getExecutablePath(),
                p.getCpuUsage(),
                p.getMemoryUsageMB(),
                p.getParentPid(),
                p.getStatus()
        ));
    }
}