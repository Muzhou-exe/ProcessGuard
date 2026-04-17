package com.processguard.core;

import com.processguard.models.ProcessInfo;
import com.processguard.models.Status;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Responsible for enumerating all running system processes and returning immutable ProcessInfo snapshots.
 * Primary API: Java 9+ ProcessHandle.allProcesses()
 * Fallback for memory/CPU details: Windows tasklist command via ProcessBuilder (as specified in SDD section 2.1).
 *
 * Thread safety: Stateless — safe for concurrent calls.
 */
public class ProcessScanner {

    private static class ProcessMetrics {
        String name;
        double cpu;
        long memoryMB;

        ProcessMetrics(double cpu, long memoryMB) {
            this(null, cpu, memoryMB);
        }

        ProcessMetrics(String name, double cpu, long memoryMB) {
            this.name = name;
            this.cpu = cpu;
            this.memoryMB = memoryMB;
        }
    }

    private static final String OS_WIN = "win";
    private static final String CMD_TASKLIST = "tasklist";
    private static final String CMD_PS = "ps";

    private static final String TASKLIST_FORMAT = "csv";
    private static final String TASKLIST_NOHEADER = "/nh";

    private static final String PS_ARGS_METRICS = "-axo";
    private static final String PS_ARGS_NAME = "-p";
    private static final String PS_NAME_OUTPUT = "-o";
    private static final String PS_NAME_FORMAT = "comm=";

    private final AppConfig config = AppConfig.getInstance();

    private final Map<Long, String> psNameCache = new ConcurrentHashMap<>();
    private final Map<Long, Long> previousCpuMillis = new ConcurrentHashMap<>();
    private long lastScanTimeMs = 0;

    /**
     * Returns snapshot of all running processes.
     * @return list of ProcessInfo objects
     */
    public List<ProcessInfo> scanProcesses() {
        List<ProcessInfo> processes = new ArrayList<>();

        long currentTimeMs = System.currentTimeMillis();
        long elapsedMs = (lastScanTimeMs > 0) ? (currentTimeMs - lastScanTimeMs) : 0;

        Map<Long, ProcessMetrics> metrics =
                isWindows() ? scanMetricsWithTasklist() : scanMetricsWithPs();

        Map<Long, Long> currentCpuMillis = new HashMap<>();

        ProcessHandle.allProcesses().forEach(handle -> {
            try {
                long pid = handle.pid();

                String executablePath = handle.info().command().orElse("");
                String name = executablePath.isBlank()
                        ? "unknown"
                        : executablePath.substring(executablePath.lastIndexOf(java.io.File.separator) + 1);

                if (name.equals("unknown") && !isWindows()) {
                    ProcessInfo psFallback = psLookup(pid);
                    if (psFallback != null) {
                        name = psFallback.getName();
                    }
                }

                long parentPid = handle.parent().map(ProcessHandle::pid).orElse(-1L);
                Instant startTime = handle.info().startInstant().orElse(Instant.EPOCH);

                ProcessMetrics m = metrics.getOrDefault(pid, new ProcessMetrics(0.0, 0));

                if (name.equals("unknown") && m.name != null && !m.name.isBlank()) {
                    name = m.name;
                }

                double cpu = m.cpu;
                if (cpu <= 0.0 && elapsedMs > 0) {
                    cpu = estimateCpu(handle, pid, elapsedMs, currentCpuMillis);
                }

                processes.add(new ProcessInfo(
                        pid,
                        name,
                        executablePath,
                        cpu,
                        m.memoryMB,
                        parentPid,
                        startTime
                ));

            } catch (Exception ignored) {
            }
        });

        previousCpuMillis.clear();
        previousCpuMillis.putAll(currentCpuMillis);
        lastScanTimeMs = currentTimeMs;

        classifyProcesses(processes);
        return processes;
    }

    /**
     * Estimates CPU usage percentage from ProcessHandle.totalCpuDuration() delta.
     * Used on Windows where tasklist does not provide CPU data.
     */
    private double estimateCpu(ProcessHandle handle, long pid,
                               long elapsedMs, Map<Long, Long> currentCpuMillis) {
        try {
            Duration totalCpu = handle.info().totalCpuDuration().orElse(null);
            if (totalCpu == null) return 0.0;

            long currentMs = totalCpu.toMillis();
            currentCpuMillis.put(pid, currentMs);

            Long prevMs = previousCpuMillis.get(pid);
            if (prevMs == null) return 0.0;

            double deltaMs = currentMs - prevMs;
            if (deltaMs < 0) return 0.0;

            return (deltaMs / elapsedMs) * 100.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Windows fallback using tasklist command for process metrics.
     */
    private Map<Long, ProcessMetrics> scanMetricsWithTasklist() {
        Map<Long, ProcessMetrics> metrics = new HashMap<>();

        if (!isWindows()) return metrics;

        try {
            Process process = new ProcessBuilder(
                    CMD_TASKLIST, "/fo", TASKLIST_FORMAT, TASKLIST_NOHEADER
            ).start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\",\"");
                    if (parts.length < 5) continue;

                    String taskName = parts[0].replace("\"", "").trim();
                    long pid = Long.parseLong(parts[1].replace("\"", "").trim());

                    String memStr = parts[4]
                            .replace("\"", "")
                            .replace(",", "")
                            .replace("K", "")
                            .trim();

                    long memoryKB = Long.parseLong(memStr);

                    metrics.put(pid, new ProcessMetrics(taskName, 0.0, memoryKB / 1024));
                }
            }

        } catch (Exception e) {
            System.err.println("tasklist metrics fallback failed: " + e.getMessage());
        }

        return metrics;
    }

    /**
     * macOS/Linux fallback using ps command for process metrics.
     */
    private Map<Long, ProcessMetrics> scanMetricsWithPs() {
        Map<Long, ProcessMetrics> metrics = new HashMap<>();

        try {
            Process process = new ProcessBuilder(
                    CMD_PS, "-axo", "pid=,rss=,%cpu="
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
     * Classifies processes using whitelist/blacklist and thresholds.
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
            } else if (p.getCpuUsage() > config.getCpuThreshold()
                    || p.getMemoryUsageMB() > config.getMemoryThreshold()) {
                p.setStatus(Status.SUSPICIOUS);
            } else {
                p.setStatus(Status.NORMAL);
            }
        }
    }

    /**
     * Checks whether OS is Windows.
     */
    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains(OS_WIN);
    }

    /**
     * Returns PID of the current application process.
     * @return current process ID
     */
    public long getOwnPid() {
        return ProcessHandle.current().pid();
    }

    /**
     * Looks up process name using ps command (macOS/Linux).
     */
    private ProcessInfo psLookup(long pid) {
        if (isWindows()) return null;

        if (psNameCache.containsKey(pid)) {
            String cachedName = psNameCache.get(pid);
            return cachedName != null
                    ? new ProcessInfo(pid, cachedName, "", 0.0, 0, -1L, Instant.EPOCH)
                    : null;
        }

        try {
            Process process = new ProcessBuilder(
                    CMD_PS, PS_ARGS_NAME, String.valueOf(pid), PS_NAME_OUTPUT, PS_NAME_FORMAT
            ).start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (line != null && !line.isBlank()) {
                    String name = line.strip();
                    psNameCache.put(pid, name);
                    return new ProcessInfo(pid, name, "", 0.0, 0, -1L, Instant.EPOCH);
                }
            }

            process.waitFor(1, TimeUnit.SECONDS);

        } catch (Exception e) {
            System.err.println("psLookup failed for PID " + pid + ": " + e.getMessage());
        }

        psNameCache.put(pid, null);
        return null;
    }

}