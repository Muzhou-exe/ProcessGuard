package com.processguard.core;

import com.processguard.listeners.ProcessListener;
import com.processguard.listeners.AlertListener;
import com.processguard.models.*;

import java.util.List;

/**
 * Custom Rule Engine (Orchestrator)
 * Keeps API stable but delegates logic cleanly.
 */
public class CustomRuleEngine implements ProcessListener {

    private final AppConfig config = AppConfig.getInstance();

    private final RuleEvaluator evaluator;
    private final RuleActionExecutor executor;

    public CustomRuleEngine(HistoryStorage historyStorage) {
        this.evaluator = new RuleEvaluator();
        this.executor = new RuleActionExecutor(historyStorage);
    }

    @Override
    public void onNewProcesses(List<ProcessInfo> newProcesses) {
        evaluate(newProcesses);
    }

    @Override
    public void onExitedProcesses(List<ProcessInfo> exitedProcesses) {
        // no-op
    }

    @Override
    public void onSnapshotUpdate(List<ProcessInfo> snapshot) {
        evaluate(snapshot);
    }

    public void addAlertListener(AlertListener listener) {
        executor.addAlertListener(listener);
    }

    private void evaluate(List<ProcessInfo> processes) {
        System.out.println("CustomRuleEngine evaluating " + processes.size() + " processes");

        List<CustomRule> rules = config.getCustomRules();
        if (rules == null || rules.isEmpty()) return;

        for (ProcessInfo process : processes) {
            for (CustomRule rule : rules) {

                if (!rule.isEnabled()) continue;

                if (evaluator.matches(process, rule)) {
                    executor.execute(process, rule);
                    break; // same behavior as before
                }
            }
        }
    }
}