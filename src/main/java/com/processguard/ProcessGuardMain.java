package com.processguard;

import com.processguard.core.AlertEngine;
import com.processguard.core.CustomRuleEngine;
import com.processguard.core.HistoryStorage;
import com.processguard.core.ProcessMonitor;
import com.processguard.ui.MainDashboard;
import javafx.application.Application;

/**
 * Entry point for ProcessGuard application that initializes core services and launches the UI.
 */
public class ProcessGuardMain {

    public static ProcessMonitor processMonitor;
    public static AlertEngine alertEngine;
    public static HistoryStorage historyStorage;
    public static CustomRuleEngine customRuleEngine;

    /**
     * Main method that initializes system components and starts the application.
     * @param args command-line arguments
     */
    public static void main(String[] args) {

        historyStorage = new HistoryStorage();
        processMonitor = new ProcessMonitor(historyStorage);
        alertEngine = new AlertEngine(historyStorage);
        customRuleEngine = new CustomRuleEngine(historyStorage);

        processMonitor.addListener(alertEngine);
        processMonitor.addListener(customRuleEngine);

        processMonitor.start();

        // Launch GUI
        Application.launch(MainDashboard.class, args);
    }
}