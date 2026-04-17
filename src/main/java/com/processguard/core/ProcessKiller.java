package com.processguard.core;

/**
 * Utility class responsible for terminating system processes by PID.
 * Supports Windows, macOS, and Linux.
 */
public class ProcessKiller {

    private static final String OS_WIN = "win";
    private static final String CMD_TASKKILL = "taskkill";
    private static final String FLAG_PID = "/PID";
    private static final String FLAG_FORCE = "/F";

    private static final String CMD_KILL = "kill";
    private static final String FLAG_KILL = "-9";

    /**
     * Force kills a process by PID using OS-specific commands.
     * @param pid process ID to terminate
     * @return true if process was successfully killed
     */
    public static boolean kill(long pid) {

        if (pid <= 0) return false;

        try {
            String os = System.getProperty("os.name").toLowerCase();

            ProcessBuilder pb;

            if (os.contains(OS_WIN)) {
                pb = new ProcessBuilder(CMD_TASKKILL, FLAG_PID, String.valueOf(pid), FLAG_FORCE);
            } else {
                pb = new ProcessBuilder(CMD_KILL, FLAG_KILL, String.valueOf(pid));
            }

            Process process = pb.start();
            int exitCode = process.waitFor();

            return exitCode == 0;

        } catch (Exception e) {
            System.err.println("Failed to kill process " + pid + ": " + e.getMessage());
            return false;
        }
    }
}