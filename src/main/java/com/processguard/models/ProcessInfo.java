package com.processguard.models;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable snapshot of a system process (except for status, which can be updated during classification).
 * Follows the data model defined in section 4.1 of the SDD.
 */
public class ProcessInfo {

    private final long pid;
    private final String name;
    private final String executablePath;
    private final double cpuUsage;          // percentage (0.0 - 100.0+)
    private final long memoryUsageMB;       // Resident Set Size in MB
    private final long parentPid;
    private final Instant startTime;
    private final Instant capturedAt;

    private Status status;                  // mutable only for classification

    private boolean flagged = false;
    private String flagReason = "";

    /**
     * Constructs a new ProcessInfo snapshot.
     *
     * @param pid              Process ID
     * @param name             Process name/command
     * @param executablePath   Full path to the executable (if available)
     * @param cpuUsage         CPU usage percentage
     * @param memoryUsageMB    Memory usage in megabytes
     * @param parentPid        Parent process ID (-1 if not available)
     * @param startTime        Process start time (Instant.EPOCH if unknown)
     */
    public ProcessInfo(long pid, String name, String executablePath,
                       double cpuUsage, long memoryUsageMB,
                       long parentPid, Instant startTime) {

        this.pid = pid;
        this.name = (name != null) ? name : "unknown";
        this.executablePath = (executablePath != null) ? executablePath : "";
        this.cpuUsage = Math.max(0.0, cpuUsage);                    // ensure non-negative
        this.memoryUsageMB = Math.max(0, memoryUsageMB);
        this.parentPid = parentPid;
        this.startTime = (startTime != null) ? startTime : Instant.EPOCH;
        this.capturedAt = Instant.now();
        this.status = Status.NORMAL;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public long getPid() {
        return pid;
    }

    public String getName() {
        return name;
    }

    public String getExecutablePath() {
        return executablePath;
    }

    public double getCpuUsage() {
        return cpuUsage;
    }

    public long getMemoryUsageMB() {
        return memoryUsageMB;
    }

    public long getParentPid() {
        return parentPid;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }

    public Status getStatus() {
        return status;
    }

    // -------------------------------------------------------------------------
    // Mutable method (only status can be changed after creation)
    // -------------------------------------------------------------------------

    /**
     * Updates the classification status of this process.
     * Called by ProcessMonitor / AlertEngine during rule evaluation.
     */
    public void setStatus(Status status) {
        this.status = (status != null) ? status : Status.NORMAL;
    }

    // -------------------------------------------------------------------------
    // Equality & Hashing (equality based solely on PID, per SDD)
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProcessInfo)) return false;
        ProcessInfo that = (ProcessInfo) o;
        return pid == that.pid;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pid);
    }

    // -------------------------------------------------------------------------
    // Useful helper methods
    // -------------------------------------------------------------------------

    /**
     * Returns a concise string representation suitable for logging and UI.
     */
    @Override
    public String toString() {
        return String.format("ProcessInfo[pid=%d, name=%s, cpu=%.1f%%, mem=%d MB, status=%s]",
                pid, name, cpuUsage, memoryUsageMB, status);
    }

    /**
     * Creates a defensive copy with the same data (useful for snapshots).
     */
    public ProcessInfo copy() {
        ProcessInfo copy = new ProcessInfo(pid, name, executablePath,
                cpuUsage, memoryUsageMB, parentPid, startTime);
        copy.setStatus(this.status);
        return copy;
    }

    public boolean isFlagged() {
        return flagged;
    }

    public String getFlagReason() {
        return flagReason;
    }

    /**
     * Marks this process as flagged with a reason.
     */
    public void flag(String reason) {
        this.flagged = true;
        this.flagReason = (reason != null) ? reason : "";
    }

    /**
     * Removes flag from this process.
     */
    public void unflag() {
        this.flagged = false;
        this.flagReason = "";
    }
}