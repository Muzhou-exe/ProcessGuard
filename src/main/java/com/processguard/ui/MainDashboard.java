package com.processguard.ui;

import com.processguard.ProcessGuardMain;
import com.processguard.core.ProcessMonitor;
import com.processguard.core.AlertEngine;
import com.processguard.core.HistoryStorage;
import com.processguard.listeners.AlertListener;
import com.processguard.listeners.ProcessListener;
import com.processguard.models.AlertEvent;
import com.processguard.models.ProcessInfo;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * Main JavaFX dashboard for ProcessGuard.
 * Coordinates UI components and connects them to backend services.
 */
public class MainDashboard extends Application
        implements ProcessListener, AlertListener {

    private final ProcessTableManager tableManager = new ProcessTableManager();
    private final AlertSidebarManager alertSidebarManager = new AlertSidebarManager();
    private final StatusBarManager statusBarManager = new StatusBarManager();
    private final ToolbarManager toolbarManager = new ToolbarManager();

    private ProcessMonitor processMonitor;
    private AlertEngine alertEngine;
    private HistoryStorage historyStorage;

    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Initializes and starts the main JavaFX UI.
     * @param primaryStage primary application window
     */
    @Override
    public void start(Stage primaryStage) {
        this.historyStorage = ProcessGuardMain.historyStorage;
        this.processMonitor = ProcessGuardMain.processMonitor;
        this.alertEngine = ProcessGuardMain.alertEngine;

        primaryStage.setOnCloseRequest(e -> {
            if (processMonitor != null) {
                processMonitor.stop();
            }
            javafx.application.Platform.exit();
            System.exit(0);
        });

        tableManager.connectSidebar(alertSidebarManager);

        BorderPane root = new BorderPane();
        root.setTop(toolbarManager.createToolbar(primaryStage, tableManager));
        root.setCenter(tableManager.getTable());
        root.setRight(alertSidebarManager.getSidebar());
        root.setBottom(statusBarManager.getStatusBar());

        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setScene(scene);
        primaryStage.setTitle("ProcessGuard v1.6 – Live Process Monitor");
        primaryStage.show();

        processMonitor.addListener(this);
        alertEngine.addAlertListener(this);
        if (ProcessGuardMain.customRuleEngine != null) {
            ProcessGuardMain.customRuleEngine.addAlertListener(this);
        }

        tableManager.setAlertSidebarManager(alertSidebarManager);
        tableManager.setStatusBarManager(statusBarManager);
    }

    /**
     * Handles snapshot updates from ProcessMonitor.
     * @param snapshot current process snapshot
     */
    @Override
    public void onSnapshotUpdate(java.util.List<ProcessInfo> snapshot) {
        javafx.application.Platform.runLater(() -> {
            tableManager.updateTable(snapshot);
            statusBarManager.updateStatus(snapshot);
        });
    }

    /**
     * Handles alerts from AlertEngine.
     * @param alert alert event
     */
    @Override
    public void onAlert(AlertEvent alert) {
        alertSidebarManager.addAlert(alert);
    }

    /**
     * Handles exited processes.
     * @param exitedProcesses list of exited processes
     */
    @Override
    public void onExitedProcesses(java.util.List<ProcessInfo> exitedProcesses) {
        javafx.application.Platform.runLater(() -> {
            tableManager.removeProcesses(exitedProcesses);
        });
    }

    /**
     * Handles newly detected processes.
     * @param newProcesses list of new processes
     */
    @Override
    public void onNewProcesses(java.util.List<ProcessInfo> newProcesses) {
        javafx.application.Platform.runLater(() -> {
            tableManager.addProcesses(newProcesses);
        });
    }
}