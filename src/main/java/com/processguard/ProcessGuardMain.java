package com.processguard;

import com.processguard.core.*;
import com.processguard.ui.MainDashboard;
import javafx.application.Application;

public class ProcessGuardMain {

    public static ProcessMonitor processMonitor;
    public static AlertEngine alertEngine;
    public static HistoryStorage historyStorage;
    public static CustomRuleEngine customRuleEngine;

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

        Runtime.getRuntime().addShutdownHook(new Thread(processMonitor::stop));
    }
}