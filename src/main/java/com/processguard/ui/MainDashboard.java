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

    @Override
    public void start(Stage primaryStage) {
        // Register backend references
        this.historyStorage = ProcessGuardMain.historyStorage;
        this.processMonitor = ProcessGuardMain.processMonitor;
        this.alertEngine = ProcessGuardMain.alertEngine;

        // Build UI components
        BorderPane root = new BorderPane();
        root.setTop(toolbarManager.createToolbar(primaryStage, tableManager));
        root.setCenter(tableManager.getTable());
        root.setRight(alertSidebarManager.getSidebar());
        root.setBottom(statusBarManager.getStatusBar());

        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setScene(scene);
        primaryStage.setTitle("ProcessGuard v1.6 – Live Process Monitor");
        primaryStage.show();

        // Register listeners
        processMonitor.addListener(this);
        alertEngine.addAlertListener(this);
        if (ProcessGuardMain.customRuleEngine != null) {
            ProcessGuardMain.customRuleEngine.addAlertListener(this);
        }

        // Wire components
        tableManager.setAlertSidebarManager(alertSidebarManager);
        tableManager.setStatusBarManager(statusBarManager);

        root.setTop(toolbarManager.createToolbar(primaryStage, tableManager));
    }

    // ====================== Observer Callbacks ======================
    @Override
    public void onSnapshotUpdate(java.util.List<ProcessInfo> snapshot) {
        javafx.application.Platform.runLater(() -> {
            tableManager.updateTable(snapshot);
            statusBarManager.updateStatus(snapshot);
        });
    }

    @Override
    public void onAlert(AlertEvent alert) {
        alertSidebarManager.addAlert(alert);
    }

    @Override
    public void onExitedProcesses(java.util.List<ProcessInfo> exitedProcesses) {
        javafx.application.Platform.runLater(() -> {
            tableManager.removeProcesses(exitedProcesses);
        });
    }

    @Override
    public void onNewProcesses(java.util.List<ProcessInfo> newProcesses) {
        javafx.application.Platform.runLater(() -> {
            tableManager.addProcesses(newProcesses);
        });
    }
}