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
    private final double cpuUsage;
    private final long memoryUsageMB;
    private final long parentPid;
    private final Instant startTime;
    private final Instant capturedAt;

    private Status status;

    private boolean flagged = false;
    private String flagReason = null;

    /**
     * Constructs a new ProcessInfo snapshot.
     * @param pid process id
     * @param name process name/command
     * @param executablePath full path to executable
     * @param cpuUsage cpu usage percentage
     * @param memoryUsageMB memory usage in MB
     * @param parentPid parent process id (-1 if unknown)
     * @param startTime process start time
     */
    public ProcessInfo(long pid, String name, String executablePath,
                       double cpuUsage, long memoryUsageMB,
                       long parentPid, Instant startTime) {

        this.pid = pid;
        this.name = (name != null) ? name : "unknown";
        this.executablePath = (executablePath != null) ? executablePath : "";
        this.cpuUsage = Math.max(0.0, cpuUsage);
        this.memoryUsageMB = Math.max(0, memoryUsageMB);
        this.parentPid = parentPid;
        this.startTime = (startTime != null) ? startTime : Instant.EPOCH;
        this.capturedAt = Instant.now();
        this.status = Status.NORMAL;
    }

    /**
     * Returns process id.
     * @return pid
     */
    public long getPid() {
        return pid;
    }

    /**
     * Returns process name.
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns executable path.
     * @return executable path
     */
    public String getExecutablePath() {
        return executablePath;
    }

    /**
     * Returns cpu usage percentage.
     * @return cpu usage
     */
    public double getCpuUsage() {
        return cpuUsage;
    }

    /**
     * Returns memory usage in MB.
     * @return memory usage
     */
    public long getMemoryUsageMB() {
        return memoryUsageMB;
    }

    /**
     * Returns parent process id.
     * @return parent pid
     */
    public long getParentPid() {
        return parentPid;
    }

    /**
     * Returns process start time.
     * @return start time
     */
    public Instant getStartTime() {
        return startTime;
    }

    /**
     * Returns time snapshot was captured.
     * @return captured time
     */
    public Instant getCapturedAt() {
        return capturedAt;
    }

    /**
     * Returns current classification status.
     * @return status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Updates classification status of this process.
     * @param status new status
     */
    public void setStatus(Status status) {
        this.status = (status != null) ? status : Status.NORMAL;
    }

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

    /**
     * Returns string representation for logging and UI.
     * @return formatted string
     */
    @Override
    public String toString() {
        return String.format("ProcessInfo[pid=%d, name=%s, cpu=%.1f%%, mem=%d MB, status=%s]",
                pid, name, cpuUsage, memoryUsageMB, status);
    }

    /**
     * Creates a defensive copy of this process.
     * @return copied ProcessInfo
     */
    public ProcessInfo copy() {
        ProcessInfo copy = new ProcessInfo(pid, name, executablePath,
                cpuUsage, memoryUsageMB, parentPid, startTime);
        copy.setStatus(this.status);
        return copy;
    }

    /**
     * Returns whether process is flagged.
     * @return true if flagged
     */
    public boolean isFlagged() {
        return flagged;
    }

    /**
     * Returns flag reason.
     * @return flag reason
     */
    public String getFlagReason() {
        return flagReason;
    }

    /**
     * Flags this process with a reason.
     * @param reason flag reason
     */
    public void flag(String reason) {
        this.flagged = true;
        this.flagReason = reason;
    }

    /**
     * Removes flag from this process.
     */
    public void unflag() {
        this.flagged = false;
        this.flagReason = null;
    }
}