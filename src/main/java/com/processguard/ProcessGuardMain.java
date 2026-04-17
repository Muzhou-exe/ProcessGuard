package com.processguard;

import com.processguard.core.AlertEngine;
import com.processguard.core.CustomRuleEngine;
import com.processguard.core.HistoryStorage;
import com.processguard.core.ProcessMonitor;
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
    }
}