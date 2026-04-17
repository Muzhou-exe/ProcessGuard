package com.processguard.core;

import com.processguard.listeners.AlertListener;
import com.processguard.listeners.ProcessListener;
import com.processguard.models.CustomRule;
import com.processguard.models.ProcessInfo;

import java.util.List;

/**
 * Custom Rule Engine (Orchestrator)
 * Keeps API stable but delegates logic cleanly.
 */
public class CustomRuleEngine implements ProcessListener {

    private final AppConfig config = AppConfig.getInstance();

    private final RuleEvaluator evaluator;
    private final RuleActionExecutor executor;

    /**
     * Constructs CustomRuleEngine with history storage dependency.
     * @param historyStorage storage used by rule actions
     */
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

    /**
     * Registers an alert listener for rule-triggered alerts.
     * @param listener alert listener
     */
    public void addAlertListener(AlertListener listener) {
        executor.addAlertListener(listener);
    }

    /**
     * Evaluates processes against enabled custom rules.
     * @param processes list of processes to evaluate
     */
    private void evaluate(List<ProcessInfo> processes) {
        System.out.println("CustomRuleEngine evaluating " + processes.size() + " processes");

        List<CustomRule> rules = config.getCustomRules();
        if (rules == null || rules.isEmpty()) return;

        for (ProcessInfo process : processes) {
            for (CustomRule rule : rules) {

                if (!rule.isEnabled()) continue;

                if (evaluator.matches(process, rule)) {
                    executor.execute(process, rule);
                    break;
                }
            }
        }
    }
}