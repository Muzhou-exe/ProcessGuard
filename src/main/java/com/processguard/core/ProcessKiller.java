package com.processguard.core;

public class ProcessKiller {

    /**
     * Force kills a process by PID.
     * Cross-platform: Windows + macOS + Linux
     */
    public static boolean kill(long pid) {
        try {
            String os = System.getProperty("os.name").toLowerCase();

            ProcessBuilder pb;

            if (os.contains("win")) {
                pb = new ProcessBuilder("taskkill", "/PID", String.valueOf(pid), "/F");
            } else {
                pb = new ProcessBuilder("kill", "-9", String.valueOf(pid));
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