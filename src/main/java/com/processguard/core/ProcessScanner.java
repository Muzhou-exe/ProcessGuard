package com.processguard.core;

import com.processguard.models.ProcessInfo;
import com.processguard.models.Status;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.Set;

/**
 * Responsible for enumerating all running system processes and returning immutable ProcessInfo snapshots.
 * Primary API: Java 9+ ProcessHandle.allProcesses()
 * Fallback for memory/CPU details: Windows tasklist command via ProcessBuilder (as specified in SDD section 2.1).
 *
 * Thread safety: Stateless — safe for concurrent calls.
 */
public class ProcessScanner {

    private final AppConfig config = AppConfig.getInstance();

    /**
     * Returns a snapshot of all currently running processes.
     *
     * @return List of immutable ProcessInfo objects
     */
    public List<ProcessInfo> scanProcesses() {
        List<ProcessInfo> processes = new ArrayList<>();

        try {
            // Primary method: Java ProcessHandle API (cross-platform)
            processes = ProcessHandle.allProcesses()
                    .map(this::convertToProcessInfo)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("Warning: ProcessHandle scan failed. Falling back to platform-specific method.");
        }

        // If ProcessHandle returned very few processes or failed, use fallback for Windows
        if (processes.isEmpty() || isWindows() && processes.size() < 50) {
            List<ProcessInfo> fallbackProcesses = scanWithTasklist();
            if (!fallbackProcesses.isEmpty()) {
                processes = fallbackProcesses;
            }
        }

        // Classify processes based on config (blacklist/whitelist)
        classifyProcesses(processes);

        return processes;
    }

    /**
     * Converts a ProcessHandle to ProcessInfo.
     */
    private ProcessInfo convertToProcessInfo(ProcessHandle handle) {
        try {
            long pid = handle.pid();
            String name = handle.info().command().orElse("unknown")
                    .substring(handle.info().command().orElse("unknown").lastIndexOf(java.io.File.separator) + 1);

            String executablePath = handle.info().command().orElse("");

            // CPU and Memory usage estimation (ProcessHandle has limited support)
            double cpuUsage = estimateCpuUsage(handle);
            long memoryUsageMB = estimateMemoryUsageMB(handle);
            long parentPid = handle.parent().map(ProcessHandle::pid).orElse(-1L);
            Instant startTime = handle.info().startInstant().orElse(Instant.EPOCH);

            return new ProcessInfo(pid, name, executablePath, cpuUsage, memoryUsageMB, parentPid, startTime);

        } catch (Exception e) {
            // Fallback for any process that fails to read
            return new ProcessInfo(handle.pid(), "unknown", "", 0.0, 0L, -1L, Instant.EPOCH);
        }
    }

    /**
     * Basic CPU usage estimation (ProcessHandle does not provide direct % usage).
     * Returns a placeholder value; can be enhanced with JMX or external tools if needed.
     */
    private double estimateCpuUsage(ProcessHandle handle) {
        try {
            // Rough estimation using OS-specific methods can be added here in future
            return 0.0; // Placeholder - will be improved in monitoring layer if needed
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Estimates memory usage in MB using available ProcessHandle info.
     */
    private long estimateMemoryUsageMB(ProcessHandle handle) {
        try {
            // ProcessHandle.info().totalCpuDuration() or other metrics are limited
            // For better accuracy, tasklist fallback is used on Windows
            return 0L; // Placeholder
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * Windows-specific fallback using 'tasklist' command for better CPU/Memory details.
     */
    private List<ProcessInfo> scanWithTasklist() {
        List<ProcessInfo> processes = new ArrayList<>();

        if (!isWindows()) {
            return processes;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder("tasklist", "/fo", "csv", "/nh");
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\",\"");
                    if (parts.length >= 5) {
                        try {
                            String imageName = parts[0].replace("\"", "").trim();
                            String pidStr = parts[1].replace("\"", "").trim();
                            long pid = Long.parseLong(pidStr);

                            // Memory (K) -> convert to MB
                            String memStr = parts[4].replace("\"", "").replace(",", "").replace("K", "").trim();
                            long memoryKB = Long.parseLong(memStr);
                            long memoryMB = memoryKB / 1024;

                            String name = imageName.contains(".")
                                    ? imageName.substring(0, imageName.lastIndexOf('.'))
                                    : imageName;

                            processes.add(new ProcessInfo(pid, name, imageName, 0.0, memoryMB, -1L, Instant.EPOCH));
                        } catch (Exception ignored) {
                            // Skip malformed lines
                        }
                    }
                }
            }

            process.waitFor(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("Warning: tasklist fallback failed: " + e.getMessage());
        }

        return processes;
    }

    /**
     * Classifies each process based on blacklist/whitelist from AppConfig.
     */
    private void classifyProcesses(List<ProcessInfo> processes) {
        Set<String> blacklist = config.getBlacklist();
        Set<String> whitelist = config.getWhitelist();

        for (ProcessInfo p : processes) {
            String processName = p.getName().toLowerCase();

            if (whitelist.contains(processName) || whitelist.contains(p.getExecutablePath())) {
                p.setStatus(Status.WHITELISTED);
            } else if (blacklist.contains(processName) || blacklist.contains(p.getExecutablePath())) {
                p.setStatus(Status.BLOCKED);
            } else if (p.getCpuUsage() > config.getCpuThreshold() ||
                    p.getMemoryUsageMB() > config.getMemoryThreshold()) {
                p.setStatus(Status.SUSPICIOUS);
            } else {
                p.setStatus(Status.NORMAL);
            }
        }
    }

    /**
     * Checks if the current operating system is Windows.
     */
    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    /**
     * Returns the current process ID of ProcessGuard itself (useful for self-exclusion if needed).
     */
    public long getOwnPid() {
        return ProcessHandle.current().pid();
    }
}