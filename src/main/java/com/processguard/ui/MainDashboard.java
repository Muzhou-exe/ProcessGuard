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

    private Label scanIntervalLabel, totalProcessesLabel, lastScanLabel, cpuSummaryLabel, memSummaryLabel;

    private ProcessMonitor processMonitor;
    private AlertEngine alertEngine;
    private HistoryStorage historyStorage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

        // --- Top Toolbar ---
        ToolBar toolbar = new ToolBar();
        Button btnStart = new Button("Start Monitoring");
        Button btnStop = new Button("Stop Monitoring");
        Button btnRefresh = new Button("Refresh Now");
        Button btnConfig = new Button("Open Configuration");
        Button btnExport = new Button("Export Report");

        btnStart.setOnAction(e -> {
            if (!processMonitor.isRunning()) {
                processMonitor.start();
            }
        });

        btnStop.setOnAction(e -> {
            if (processMonitor.isRunning()) {
                processMonitor.stop();
            }
        });

        btnRefresh.setOnAction(e -> processMonitor.scanNow());

        btnStart.disableProperty().bind(
                Bindings.createBooleanBinding(
                        () -> processMonitor.isRunning()
                )
        );

        btnStop.disableProperty().bind(
                Bindings.createBooleanBinding(
                        () -> !processMonitor.isRunning()
                )
        );

        toolbar.getItems().addAll(btnStart, btnStop, btnRefresh, btnConfig, btnExport);

        // --- Table Setup ---
        processTable = new TableView<>();

        // 1. Master list
        masterData = FXCollections.observableArrayList();

        // 2. Wrap in SortedList
        SortedList<ProcessInfo> sortedData = new SortedList<>(masterData);
        sortedData.comparatorProperty().bind(processTable.comparatorProperty());

        // 3. Assign TableView
        processTable.setItems(sortedData);

        // --- Background updater ---
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            // 1. Get fresh snapshot from your ProcessMonitor / backend
            List<ProcessInfo> latestProcesses = ProcessMonitor.getCurrentProcesses(); // <-- returns List<ProcessInfo>

            // 2. Update masterData on FX thread
            Platform.runLater(() -> masterData.setAll(latestProcesses));
        }, 0, 1, TimeUnit.SECONDS);

        // --- Columns ---
        String[] colNames = {"PID", "Process Name", "Executable Path", "CPU %", "Memory MB", "Status", "Parent PID", "Start Time", "Captured At"};
        String[] propertyNames = {"pid", "name", "executablePath", "cpuUsage", "memoryUsageMB", "status", "parentPid", "startTime", "capturedAt"};

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
                            setText(empty || item == null ? "" : String.format("%.1f %%", item));
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
                case "startTime", "capturedAt" -> {
                    TableColumn<ProcessInfo, Instant> col = new TableColumn<>(name);
                    col.setCellValueFactory(new PropertyValueFactory<>(prop));
                    col.setCellFactory(tc -> new TableCell<>() {
                        @Override
                        protected void updateItem(Instant item, boolean empty) {
                            super.updateItem(item, empty);
                            setText(empty || item == null ? "" :
                                    DateTimeFormatter.ofPattern("HH:mm:ss")
                                            .withZone(ZoneId.systemDefault())
                                            .format(item));
                        }
                    });
                    columnMap.put(prop, col);
                }
                default -> {
                    TableColumn<ProcessInfo, Object> col = new TableColumn<>(name);
                    col.setCellValueFactory(new PropertyValueFactory<>(prop));
                    columnMap.put(prop, col);
                }
            }
        }

        // Add columns in desired order
        processTable.getColumns().clear();
        String[] desiredOrder = {"pid", "name", "cpuUsage", "memoryUsageMB", "status", "executablePath", "parentPid", "startTime", "capturedAt"};
        for (String key : desiredOrder) {
            if (columnMap.containsKey(key)) processTable.getColumns().add(columnMap.get(key));
        }

        // --- Default Sorting ---
        TableColumn<ProcessInfo, Double> cpuCol = (TableColumn<ProcessInfo, Double>) columnMap.get("cpuUsage");
        TableColumn<ProcessInfo, Long> memCol = (TableColumn<ProcessInfo, Long>) columnMap.get("memoryUsageMB");

        if (cpuCol != null && memCol != null) {
            cpuCol.setSortType(TableColumn.SortType.DESCENDING);
            memCol.setSortType(TableColumn.SortType.DESCENDING);
            processTable.getSortOrder().clear();
            processTable.getSortOrder().add(cpuCol);
            processTable.getSortOrder().add(memCol);
            processTable.sort();
        }

        // --- Row highlighting ---
        processTable.setRowFactory(tv -> new TableRow<ProcessInfo>() {
            @Override
            protected void updateItem(ProcessInfo item, boolean empty) {
                super.updateItem(item, empty);
                setStyle(""); // reset

                if (item == null || empty) return;

                // Thresholds
                double cpuThreshold = AppConfig.getInstance().getCpuThreshold();
                double memThreshold = AppConfig.getInstance().getMemoryThreshold();

                // Determine effective status (without mutating)
                Status effectiveStatus = item.getStatus();
                if (effectiveStatus != Status.BLOCKED && effectiveStatus != Status.SUSPICIOUS) {
                    if (item.getCpuUsage() > cpuThreshold || item.getMemoryUsageMB() > memThreshold) {
                        effectiveStatus = Status.SUSPICIOUS;
                    } else {
                        effectiveStatus = Status.NORMAL;
                    }
                }

                // Apply highlighting
                switch (effectiveStatus) {
                    case BLOCKED -> setStyle("-fx-background-color: rgba(255,0,0,0.3)");
                    case SUSPICIOUS -> setStyle("-fx-background-color: rgba(255,255,0,0.3)");
                    case NORMAL -> setStyle("");
                }
            }
        });

        // --- Right Sidebar: Alert Dashboard ---
        alertList = new ListView<>();
        alertData = FXCollections.observableArrayList();
        alertList.setItems(alertData);
        VBox alertSidebar = new VBox(new Label("Alerts"), alertList);
        alertSidebar.setPrefWidth(300);
        alertSidebar.setPadding(new Insets(5));

        // --- Bottom Status Bar ---
        HBox statusBar = new HBox(10);
        statusBar.setPadding(new Insets(5));
        scanIntervalLabel = new Label("Scan Interval: 3s");
        totalProcessesLabel = new Label("Processes: 0");
        lastScanLabel = new Label("Last Scan: N/A");
        cpuSummaryLabel = new Label("CPU: 0%");
        memSummaryLabel = new Label("Memory: 0MB");
        statusBar.getChildren().addAll(scanIntervalLabel, totalProcessesLabel, lastScanLabel, cpuSummaryLabel, memSummaryLabel);

        // --- Main Layout ---
        BorderPane root = new BorderPane();
        root.setTop(toolbar);
        root.setCenter(processTable);
        root.setRight(alertSidebar);
        root.setBottom(statusBar);

        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setScene(scene);
        primaryStage.setTitle("ProcessGuard v1.6 – Live Process Monitor");
        primaryStage.show();

        // Register with Observers
        this.historyStorage = ProcessGuardMain.historyStorage;
        this.processMonitor = ProcessGuardMain.processMonitor;
        this.alertEngine = ProcessGuardMain.alertEngine;

        processMonitor.addListener(this);
        alertEngine.addAlertListener(this);
    }

    // Helper to convert column names to camelCase for PropertyValueFactory
    private String camelCase(String col) {
        return col.toLowerCase().replace(" ", "");
    }

    // --- Observer Callbacks ---
    @Override
    public void onSnapshotUpdate(List<ProcessInfo> snapshot) {
        Platform.runLater(() -> {
            // Update table data
            masterData.setAll(snapshot);

            // --- Bottom bar updates ---
            totalProcessesLabel.setText("Processes: " + snapshot.size());
            lastScanLabel.setText(
                    "Last Scan: " +
                            DateTimeFormatter.ofPattern("HH:mm:ss")
                                    .withZone(ZoneId.systemDefault())
                                    .format(Instant.now())
            );

            double totalCpu = snapshot.stream()
                    .mapToDouble(ProcessInfo::getCpuUsage)
                    .sum();

            long totalMem = snapshot.stream()
                    .mapToLong(ProcessInfo::getMemoryUsageMB)
                    .sum();

            cpuSummaryLabel.setText(String.format("CPU: %.1f %%", totalCpu));
            memSummaryLabel.setText("Memory: " + totalMem + " MB");
        });
    }

    @Override
    public void onAlert(AlertEvent alert) {
        Platform.runLater(() -> {
            alertData.add(0, alert); // newest at top
            if (alertData.size() > 50) alertData.remove(50); // cap at 50
        });
    }

    @Override
    public void onExitedProcesses(List<ProcessInfo> exitedProcesses) {
        // Optional: remove from table or update UI
        Platform.runLater(() -> {
            processData.removeAll(exitedProcesses);
        });
    }

    @Override
    public void onNewProcesses(List<ProcessInfo> newProcesses) {
        Platform.runLater(() -> {
            processData.addAll(newProcesses);
        });
    }
}