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
import java.util.Map;
import java.util.HashMap;

/**
 * Responsible for enumerating all running system processes and returning immutable ProcessInfo snapshots.
 * Primary API: Java 9+ ProcessHandle.allProcesses()
 * Fallback for memory/CPU details: Windows tasklist command via ProcessBuilder (as specified in SDD section 2.1).
 *
 * Thread safety: Stateless — safe for concurrent calls.
 */
public class ProcessScanner {

    private static class ProcessMetrics {
        double cpu;
        long memoryMB;

        ProcessMetrics(double cpu, long memoryMB) {
            this.cpu = cpu;
            this.memoryMB = memoryMB;
        }
    }

    private final AppConfig config = AppConfig.getInstance();

    // Cache for ps lookups: PID -> process name
    private final Map<Long, String> psNameCache = new HashMap<>();

    /**
     * Returns a snapshot of all currently running processes.
     *
     * @return List of immutable ProcessInfo objects
     */
    public List<ProcessInfo> scanProcesses() {
        List<ProcessInfo> processes = new ArrayList<>();

        Map<Long, ProcessMetrics> metrics =
                isWindows() ? scanMetricsWithTasklist() : scanMetricsWithPs();

        ProcessHandle.allProcesses().forEach(handle -> {
            try {
                long pid = handle.pid();

                String executablePath = handle.info().command().orElse("");
                String name = executablePath.isBlank()
                        ? "unknown"
                        : executablePath.substring(executablePath.lastIndexOf(java.io.File.separator) + 1);

                // Fallback if name is unknown
                if (name.equals("unknown")) {
                    ProcessInfo psFallback = psLookup(handle.pid());
                    name = psFallback != null ? psFallback.getName() : "unknown";
                }

                long parentPid = handle.parent().map(ProcessHandle::pid).orElse(-1L);
                Instant startTime = handle.info().startInstant().orElse(Instant.EPOCH);

                ProcessMetrics m = metrics.getOrDefault(pid, new ProcessMetrics(0.0, 0));

                processes.add(new ProcessInfo(
                        pid,
                        name,
                        executablePath,
                        m.cpu,
                        m.memoryMB,
                        parentPid,
                        startTime
                ));

            } catch (Exception ignored) {
            }
        });

        classifyProcesses(processes);
        return processes;
    }

    /**
     * Windows-specific fallback using 'tasklist' command for better CPU/Memory details.
     */
    private Map<Long, ProcessMetrics> scanMetricsWithTasklist() {
        Map<Long, ProcessMetrics> metrics = new HashMap<>();

        if (!isWindows()) return metrics;

        try {
            Process process = new ProcessBuilder(
                    "tasklist", "/fo", "csv", "/nh"
            ).start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\",\"");
                    if (parts.length < 5) continue;

                    long pid = Long.parseLong(parts[1].replace("\"", "").trim());

                    String memStr = parts[4]
                            .replace("\"", "")
                            .replace(",", "")
                            .replace("K", "")
                            .trim();

                    long memoryKB = Long.parseLong(memStr);

                    metrics.put(pid, new ProcessMetrics(0.0, memoryKB / 1024));
                }
            }

        } catch (Exception e) {
            System.err.println("tasklist metrics fallback failed: " + e.getMessage());
        }

        return metrics;
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

    private Map<Long, ProcessMetrics> scanMetricsWithPs() {
        Map<Long, ProcessMetrics> metrics = new HashMap<>();

        try {
            Process process = new ProcessBuilder(
                    "ps", "-axo", "pid=,rss=,%cpu="
            ).start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.strip();
                    if (line.isEmpty()) continue;

                    String[] parts = line.split("\\s+");
                    if (parts.length < 3) continue;

                    long pid = Long.parseLong(parts[0]);
                    long rssKB = Long.parseLong(parts[1]);
                    double cpu = Double.parseDouble(parts[2]);

                    metrics.put(pid, new ProcessMetrics(cpu, rssKB / 1024));
                }
            }

        } catch (Exception e) {
            System.err.println("ps metrics fallback failed: " + e.getMessage());
        }

        return metrics;
    }

    /**
     * Fallback to get process name for a PID using 'ps' command (macOS/Linux).
     */
    private ProcessInfo psLookup(long pid) {
        if (isWindows()) return null; // Use tasklist fallback for Windows

        // Return from cache if available
        if (psNameCache.containsKey(pid)) {
            String cachedName = psNameCache.get(pid);
            return cachedName != null ? new ProcessInfo(pid, cachedName, "", 0.0, 0, -1L, Instant.EPOCH) : null;
        }

        try {
            Process process = new ProcessBuilder("ps", "-p", String.valueOf(pid), "-o", "comm=").start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (line != null && !line.isBlank()) {
                    String name = line.strip();
                    psNameCache.put(pid, name); // save to cache
                    return new ProcessInfo(pid, name, "", 0.0, 0, -1L, Instant.EPOCH);
                }
            }
            process.waitFor(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("psLookup failed for PID " + pid + ": " + e.getMessage());
        }

        psNameCache.put(pid, null); // mark as unknown to avoid retrying
        return null;
    }
}