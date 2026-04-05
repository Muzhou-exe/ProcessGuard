package com.processguard;

import com.processguard.core.*;
import com.processguard.listeners.*;

public class ProcessGuardMain {
    public static void main(String[] args) {
        AppConfig config = AppConfig.getInstance();
        HistoryStorage storage = new HistoryStorage();
        ProcessMonitor monitor = new ProcessMonitor(storage);

        // Register observers (Observer pattern)
        AlertEngine alertEngine = new AlertEngine(storage);
        CustomRuleEngine customRuleEngine = new CustomRuleEngine(storage);
        monitor.addListener(alertEngine);
        monitor.addListener(customRuleEngine);

        // Start all layers
        //new MainDashboard(monitor).launch();
        //new SystemTrayManager(monitor);
        //new WebServer(monitor).start();
        //new CliInterface().parse(args); // picocli support

        monitor.start();
        Runtime.getRuntime().addShutdownHook(new Thread(monitor::stop));
    }
}