package com.processguard.listeners;

import com.processguard.models.ProcessInfo;
import java.util.List;

/**
 * Observer interface for process monitoring events.
 * ProcessMonitor notifies implementations of this interface.
 * As defined in sections 2.2 and 3.1 of the SDD.
 *
 * Three minimal methods only.
 */
public interface ProcessListener {

    /**
     * Called when new processes are detected.
     */
    void onNewProcesses(List<ProcessInfo> newProcesses);

    /**
     * Called when processes have exited.
     */
    void onExitedProcesses(List<ProcessInfo> exitedProcesses);

    /**
     * Called with the full current snapshot after each scan cycle.
     */
    void onSnapshotUpdate(List<ProcessInfo> currentSnapshot);
}