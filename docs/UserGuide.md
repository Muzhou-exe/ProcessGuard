# ProcessGuard - User Guide

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [System Requirements](#2-system-requirements)
3. [Installation and Quick Start](#3-installation-and-quick-start)
4. [Understanding the Interface](#4-understanding-the-interface)
5. [Process Monitoring](#5-process-monitoring)
6. [Understanding Alerts](#6-understanding-alerts)
7. [Managing Processes](#7-managing-processes)
8. [Custom Rules](#8-custom-rules)
9. [Configuration](#9-configuration)
10. [PDF Report Export](#10-pdf-report-export)
11. [Process Flagging](#11-process-flagging)
12. [Keyboard and Mouse Reference](#12-keyboard-and-mouse-reference)
13. [Data Files and Storage](#13-data-files-and-storage)
14. [Frequently Asked Questions](#14-frequently-asked-questions)
15. [Troubleshooting](#15-troubleshooting)

---

## 1. Introduction

**ProcessGuard** is a desktop application that monitors every program currently running on your computer, identifies suspicious or dangerous ones, and alerts you in real time. Think of it as a security guard watching all the activity inside your computer and raising a flag whenever something looks wrong.

### 1.1 What ProcessGuard Does

- **Watches all running programs:** Every process on your system is listed in a live table that refreshes automatically every few seconds.
- **Colour-codes problems:** Processes that look suspicious are highlighted yellow; processes that match your blocked list are highlighted red.
- **Sends alerts:** When a process uses too much CPU, too much memory, or matches your blocked list, ProcessGuard raises a severity-graded alert.
- **Lets you act immediately:** You can terminate any process directly from the application without opening a separate tool.
- **Remembers history:** All alerts are saved to a file so you can review past incidents.
- **Lets you define your own rules:** Advanced users can write custom detection rules to flag any process by name, path, CPU usage, memory usage, or parent process.

### 1.2 Who This Guide Is For

This guide is for all users of ProcessGuard, from beginners who have never looked at a process list to developers and system administrators who want to understand every feature. No programming knowledge is required.

---

## 2. System Requirements

| Requirement | Minimum |
|---|---|
| **Java** | Java 21 or later |
| **Operating System** | Windows 10 or later, macOS 12 Monterey or later, Ubuntu 20.04 LTS or later |
| **RAM** | 256 MB available for ProcessGuard itself |
| **Disk space** | 50 MB for the JAR file; additional space for reports |
| **Display** | 1280 × 720 minimum (1920 × 1080 recommended) |
| **Permissions** | Ability to read system process information (standard user on most systems) |

### 2.1 Checking Your Java Version

Open a terminal (Command Prompt on Windows, Terminal on macOS/Linux) and run:

```
java -version
```

You should see output like:

```
java version "21.0.2" 2024-01-16 LTS
```

If the version shown is below 21, download Java 21 from [https://adoptium.net](https://adoptium.net) before proceeding.

---

## 3. Installation and Quick Start

### 3.1 Download

Download `ProcessGuard-1.6.jar` from the project releases page.

### 3.2 Running the Application

**Windows:**
```
java -jar ProcessGuard-1.6.jar
```
Or double-click the JAR file if your system has Java associated with `.jar` files.

**macOS:**
```
java -jar ProcessGuard-1.6.jar
```

**Linux:**
```
java -jar ProcessGuard-1.6.jar
```

The main dashboard window will open within a few seconds. ProcessGuard will begin scanning immediately — you do not need to click anything to start seeing processes.

### 3.3 First Run

On first launch, ProcessGuard creates a configuration directory at `~/.processguard/` (where `~` means your home folder). Default settings are applied automatically. You will see all running processes in the table within one scan cycle (approximately 3 seconds).

---

## 4. Understanding the Interface

The ProcessGuard window is divided into four main areas:

```
┌──────────────────────────────────────────────────────────────────────┐
│  [Start] [Stop] [Refresh] [Open Configuration] [Export Report] [X]   │  ← TOOLBAR
├───────────────────────────────────────────────────┬──────────────────┤
│                                                   │  ALERTS          │
│  PID | Name | Path | CPU% | Mem MB | Status | ... │  [12:01:05] 🔴   │
│  ──────────────────────────────────────────────── │  HIGH | chrome   │
│  1234 | chrome.exe | C:\...  | 82.3% | 620 MB |  S │                  │
│  5678 | code.exe   | C:\...  |  3.1% | 320 MB |  N │  Process Details │
│  9999 | bad.exe    | C:\...  |  0.2% |  12 MB |  B │  PID: 1234       │
│  ...                                              │  Name: chrome    │
│                                                   │  CPU: 82.3%      │
│                PROCESS TABLE                      │  Mem: 620 MB     │
│                                                   │                  │
│                                                   │  [Kill Process]  │
├───────────────────────────────────────────────────┴──────────────────┤
│  Suspicious: 2 | Blocked: 1   Processes: 187  CPU: 94.2%  Mem: 3420MB │  ← STATUS BAR
└──────────────────────────────────────────────────────────────────────┘
```

### 4.1 Toolbar (Top)

| Button | What It Does |
|---|---|
| **Start Monitoring** | Begins automatic scanning if it was stopped |
| **Stop Monitoring** | Pauses automatic scanning |
| **Refresh Now** | Triggers an immediate scan right now |
| **Open Configuration** | Opens the Rule Manager dialog for creating custom detection rules |
| **Export Report** | Generates and saves a PDF summary of the current session |
| **Close App** | Exits ProcessGuard |

### 4.2 Process Table (Centre)

The large table in the middle shows every process currently running on your computer. It refreshes automatically every few seconds (default: 3 s). Columns include:

| Column | Meaning |
|---|---|
| **PID** | Process ID — a unique number the OS assigns to every running program |
| **Process Name** | The filename of the program (e.g., `chrome.exe`, `python3`) |
| **Executable Path** | Full file path to the program (e.g., `/usr/bin/python3`) |
| **CPU %** | How much of your processor this process is using right now |
| **Memory MB** | How much RAM this process is using in megabytes |
| **Status** | Classification: NORMAL, SUSPICIOUS, BLOCKED, or WHITELISTED |
| **Parent PID** | The PID of the process that launched this one |
| **Start Time** | When this process started |
| **Captured At** | When ProcessGuard last recorded this process |

**Sorting:** Click any column header to sort by that column. Click again to reverse the sort. By default, processes are sorted by CPU usage (highest first).

### 4.3 Alert Sidebar (Right)

The right panel has two sections:

**Alerts list (top):** Shows the 10 most recent alerts. Each entry shows the time, a coloured severity indicator, and the process name. Newer alerts appear at the top.

**Process Details panel (bottom):** When you click a process row in the table, its detailed information appears here — PID, name, path, CPU, memory, parent PID, status, and any flag reason. The **Kill Process** button below the details terminates the selected process.

### 4.4 Status Bar (Bottom)

A compact summary line showing:

- **Suspicious count** and **Blocked count**
- Total **Processes** currently running
- Total system **CPU %** (sum across all processes)
- Total system **Memory** usage in MB
- **Last Scan** time

---

## 5. Process Monitoring

### 5.1 How Scanning Works

ProcessGuard automatically scans your system on a configurable interval (default: 3 seconds). Each scan cycle:

1. All running processes are enumerated.
2. CPU and memory metrics are collected.
3. Process names are resolved.
4. Each process is classified (NORMAL, SUSPICIOUS, BLOCKED, or WHITELISTED).
5. Newly started and recently exited processes are detected.
6. The table is updated.
7. Alerts are generated if needed.

You can trigger a manual scan at any time by clicking **Refresh Now**.

### 5.2 Process Classification

Every process is automatically assigned one of four status values on each scan:

| Status | What It Means | Row Colour |
|---|---|---|
| **NORMAL** | The process is operating within normal parameters and is not on any list. | No highlight |
| **SUSPICIOUS** | The process is using more CPU or memory than your configured thresholds. | Yellow |
| **BLOCKED** | The process name matches an entry on your blacklist. | Red |
| **WHITELISTED** | You have explicitly marked this process as trusted. | No highlight |

**Priority:** If a process is on both the whitelist and blacklist, the whitelist takes precedence and it will be shown as WHITELISTED.

### 5.3 Understanding CPU and Memory Values

- **CPU %** shows the percentage of total CPU capacity being used by that process at the moment of the last scan. A value of 100% means the process is fully occupying one CPU core.
- **Memory MB** shows the amount of physical RAM currently in use by the process. On macOS/Linux this is the RSS (Resident Set Size).
- Values of **0** for CPU may appear for processes that finished a CPU burst between scans. This is normal.
- Some system processes show **"unknown"** for the executable path because the OS restricts access to that information for security reasons.

---

## 6. Understanding Alerts

### 6.1 Alert Types

ProcessGuard generates alerts for five situations:

| Alert Type | What Triggers It | Default Severity |
|---|---|---|
| **Blacklisted Process** | A process name matches your blacklist | CRITICAL |
| **High CPU Usage** | A process's CPU usage exceeds your CPU threshold | HIGH |
| **High Memory Usage** | A process's memory usage exceeds your memory threshold | MEDIUM |
| **Unknown/Suspicious Process** | A process is classified as SUSPICIOUS | LOW |
| **Custom Rule Violation** | A process matches one of your custom rules | Your choice |

### 6.2 Alert Severity and Colours

| Severity | Colour | Meaning |
|---|---|---|
| **CRITICAL** | Red + Bold | Immediate attention required — a blacklisted process is running |
| **HIGH** | Red | Significant problem — process is consuming excessive CPU |
| **MEDIUM** | Orange | Notable issue — process is using a lot of memory |
| **LOW** | Blue | Worth investigating — process looks unusual |

### 6.3 Alert Deduplication

ProcessGuard is smart about not spamming you. If a process is already flagged for high CPU, you will not receive another "High CPU" alert for the same process until:

1. The process's CPU drops back below the threshold (condition clears), AND
2. The CPU rises above the threshold again (condition reoccurs).

This prevents your alert list from being flooded with the same notification every 3 seconds.

### 6.4 Reading an Alert Entry

An alert entry in the sidebar looks like:

```
[12:05:33] 🔴 HIGH | chrome.exe - High CPU usage detected: chrome.exe
```

- `12:05:33` — time the alert was generated
- `🔴 HIGH` — severity level with colour indicator
- `chrome.exe` — the process that triggered the alert
- The dash followed by text is the alert message

---

## 7. Managing Processes

### 7.1 Viewing Process Details

Click any row in the process table to view its full details in the sidebar panel on the right. You will see:

- PID (process identifier)
- Full process name
- Executable path (where the program lives on disk)
- Current CPU and memory usage
- Parent PID (which process launched this one)
- Status classification

### 7.2 Killing a Process

> **Warning:** Killing a process force-terminates it immediately. Unsaved work in that application will be lost. Always confirm you are terminating the correct process.

**Method 1 — Sidebar Kill Button:**

1. Click the process row in the table.
2. Review the process details in the sidebar.
3. Click the red **Kill Process** button.
4. Read the confirmation dialog carefully.
5. Click **OK** to terminate, or **Cancel** to abort.

**Method 2 — Right-Click Context Menu:**

1. Right-click any row in the process table.
2. Select **Kill Process** from the menu.
3. Confirm in the dialog that appears.

After killing, the process will disappear from the table on the next scan cycle (within the scan interval).

### 7.3 Copying a Process PID

Right-click a row and select **Copy PID**. The process's numeric PID is copied to your clipboard. You can paste it into a terminal to run commands on that process manually.

### 7.4 Understanding Parent Processes

The **Parent PID** column shows which process spawned this one. For example:

- `chrome.exe` (PID 1234) may have a Parent PID of `explorer.exe` (PID 500) — meaning you launched Chrome from the Windows taskbar.
- A process with an unexpected parent (e.g., a document viewer launched by a system process) may indicate malicious activity.

---

## 8. Custom Rules

Custom rules let you define your own detection logic beyond the built-in thresholds. For example: "Alert me whenever any process named `python3` uses more than 70% CPU."

### 8.1 Opening the Rule Manager

Click **Open Configuration** in the toolbar. The Rule Manager dialog opens as a separate, non-blocking window (you can still interact with the main dashboard while it is open).

### 8.2 Creating a Rule

In the Rule Manager:

1. **Rule Name:** Enter a descriptive name (e.g., "Suspicious Python").
2. **Field:** Choose what to check:
    - `name` — process executable name
    - `executablePath` — full path to the executable
    - `cpuUsage` — CPU percentage
    - `memoryUsageMB` — memory in megabytes
3. **Operator:** Choose how to compare:
    - For text fields (`name`, `executablePath`): `CONTAINS` or `EQUALS`
    - For number fields (`cpuUsage`, `memoryUsageMB`): `GREATER_THAN`, `LESS_THAN`, or `EQUALS`
4. **Value:** Enter what to match (e.g., `python3` or `70`).
5. **Action:** Choose what happens when the rule matches:
    - `LOG_ONLY` — write to the console log silently
    - `ALERT_ONLY` — add an alert to the sidebar
    - `KILL_PROCESS` — add an alert **and** automatically kill the process
6. Click **Add Rule**.

### 8.3 Rule Conditions: Text vs. Numbers

**Text matching (`name`, `executablePath`):**

| Operator | Example | Matches |
|---|---|---|
| `CONTAINS` | `name CONTAINS chrome` | `chrome.exe`, `Google Chrome Helper`, `chromedriver` |
| `EQUALS` | `name EQUALS python3` | Only `python3` exactly (case-insensitive) |

**Number matching (`cpuUsage`, `memoryUsageMB`):**

| Operator | Example | Matches |
|---|---|---|
| `GREATER_THAN` | `cpuUsage GREATER_THAN 80` | Any process using more than 80% CPU |
| `LESS_THAN` | `memoryUsageMB LESS_THAN 10` | Any process using less than 10 MB |
| `EQUALS` | `cpuUsage EQUALS 100` | Any process at exactly 100% CPU |

### 8.4 Adding a Sample Rule

Click **Add Sample Rule** to add a pre-built example: "High CPU Chrome" — alerts when any Chrome process uses more than 50% CPU. This is a good starting point to see how rules work.

### 8.5 Drag-and-Drop Rule Creation

A convenient shortcut: **drag any process row from the main table** and **drop it onto the Rule Manager dialog**. The rule form will automatically populate with:

- **Name:** "High usage [process name]"
- **Field:** `name`
- **Operator:** `CONTAINS`
- **Value:** the process name

You only need to adjust the value threshold or action before clicking **Add Rule**.

### 8.6 Enabling and Disabling Rules

In the **Existing Rules** list, select a rule and click **Enable / Disable** to toggle it. Disabled rules are shown with `✗ Disabled` and are completely skipped during evaluation. This lets you temporarily turn off a rule without deleting it.

### 8.7 Deleting a Rule

Select a rule in the list and click **Delete Selected**. The rule is immediately removed and configuration is saved.

### 8.8 Rule Evaluation Order

When a process matches multiple rules, only the **first matching rule** fires an action. Rules are evaluated in the order they appear in the list. If you want a high-priority rule to run first, add it before lower-priority rules.

### 8.9 AND vs. OR Logic

The Rule Manager creates rules with `AND` logic (all conditions must match). If you need more complex rules, you can edit `~/.processguard/config.json` directly and set `"logicOperator": "OR"` on a rule. With `OR` logic, the rule fires if **any** condition matches.

---

## 9. Configuration

### 9.1 Configuration File

ProcessGuard stores its settings in:

```
~/.processguard/config.json
```

Where `~` is your home directory (`C:\Users\YourName` on Windows, `/home/yourname` on Linux/macOS).

This file is created automatically on first launch. You can edit it with any text editor. Changes take effect after restarting ProcessGuard (or within one scan cycle for some settings).

### 9.2 Available Settings

| Setting | Default | Description |
|---|---|---|
| `scanIntervalSeconds` | `3` | How often to scan (in seconds). Minimum: 1. Lower values = more responsive but higher CPU. |
| `cpuThreshold` | `20.0` | CPU % above which a process is flagged as SUSPICIOUS and a HIGH alert fires. |
| `memoryThreshold` | `500.0` | Memory MB above which a process is flagged as SUSPICIOUS and a MEDIUM alert fires. |
| `blacklist` | `[]` | List of process names that always trigger a CRITICAL alert when detected. |
| `whitelist` | `[]` | List of process names that are never alerted on. |
| `customRules` | `[]` | User-defined detection rules (managed via Rule Manager dialog). |
| `webPort` | `8080` | Port for the optional future web interface. |
| `startMinimized` | `false` | Whether to start with the window minimised. |
| `enableSystemTray` | `true` | Whether to show a system tray icon. |

### 9.3 Example Configuration

```json
{
  "scanIntervalSeconds": 5,
  "cpuThreshold": 80.0,
  "memoryThreshold": 1024.0,
  "blacklist": ["malware.exe", "cryptominer.exe"],
  "whitelist": ["chrome.exe", "code.exe", "java.exe"],
  "customRules": [],
  "webPort": 8080,
  "startMinimized": false,
  "enableSystemTray": true
}
```

### 9.4 Setting Up a Blacklist

To block a specific process name, add it (lowercase) to the `blacklist` array in `config.json`:

```json
"blacklist": ["badprocess.exe", "miner.exe"]
```

When ProcessGuard detects a process whose name contains that text (case-insensitive), it will:
1. Classify it as **BLOCKED**.
2. Highlight the row red.
3. Fire a **CRITICAL** severity alert.

### 9.5 Setting Up a Whitelist

To prevent false alerts for a trusted process, add it to the `whitelist` array:

```json
"whitelist": ["chrome.exe", "slack.exe", "code.exe"]
```

Whitelisted processes are never alerted on, even if they exceed CPU or memory thresholds.

### 9.6 Adjusting Thresholds for Your Machine

The default thresholds (20% CPU, 500 MB memory) are intentionally low to be visible in demos. On a typical desktop or laptop, you may want to raise them:

- A modern web browser routinely uses 200–800 MB of memory.
- Video encoding, compiling code, or running games will spike CPU well above 20%.

**Suggested thresholds for a normal workstation:**

| Use Case | CPU Threshold | Memory Threshold |
|---|---|---|
| Office / light work | 50% | 1000 MB |
| Development machine | 80% | 2000 MB |
| Security monitoring | 20% | 500 MB |

---

## 10. PDF Report Export

### 10.1 Generating a Report

Click **Export Report** in the toolbar. ProcessGuard will generate a PDF and save it automatically. A dialog will appear confirming the file path when complete.

Reports are saved to:

```
~/Desktop/ProcessGuardReports/report_YYYYMMDD_HHmmss.pdf
```

For example: `report_20260417_143022.pdf`

### 10.2 What Is in the Report

The PDF report contains two sections:

**Section 1 — Top Repeat Alerts:**  
A table of the alert types that fired most frequently during your session, sorted by count (highest first). Shows alert type, count, and the highest severity observed for each type.

**Section 2 — Heaviest Processes:**  
Two sub-tables showing the top 5 processes by CPU usage and top 5 by memory usage at the time of export, including process name, PID, CPU %, and memory MB.

### 10.3 No Data Available

If you click Export Report before any data has been collected (immediately after launch before the first scan), a message will appear saying "No data available to export." Wait a few seconds and try again.

---

## 11. Process Flagging

### 11.1 What Is Flagging

Flagging is a manual investigation feature. You can mark any process with a custom note to remind yourself to investigate it later. Flagged processes are highlighted in **blue** and display a tooltip with your note.

### 11.2 How to Flag a Process

1. Right-click a process row.
2. Select **Flag Process**.
3. Enter your reason in the text input dialog (e.g., "Unknown process, check later").
4. Click **OK**.

The row turns blue and a 🚩 icon appears in the details panel.

### 11.3 How to Unflag a Process

1. Right-click the flagged (blue-highlighted) row.
2. Select **Unflag Process** (the menu item toggles based on current state).

The blue highlight is removed.

### 11.4 Flagging Behaviour Across Scans

Flags survive across scan cycles — even when the process table refreshes every 3 seconds, your flags are preserved. The flag reason is copied to the updated process entry on each scan.

---

## 12. Keyboard and Mouse Reference

### 12.1 Table Interactions

| Action | How To |
|---|---|
| Sort by column | Click column header; click again to reverse |
| View process details | Click any row |
| Kill process (sidebar) | Click row → click Kill Process button |
| Kill process (table) | Right-click row → Kill Process |
| Copy PID | Right-click row → Copy PID |
| Flag process | Right-click row → Flag Process |
| Drag to rule dialog | Click and drag a row onto the Rule Manager window |

### 12.2 Toolbar Actions

| Button | Keyboard Shortcut |
|---|---|
| Refresh Now | Click only (no keyboard shortcut) |
| Close App | Alt+F4 (Windows/Linux), Cmd+Q (macOS) |

---

## 13. Data Files and Storage

### 13.1 File Locations

All ProcessGuard data is stored locally on your machine. No data is sent to the internet.

| File | Location | Contents |
|---|---|---|
| Configuration | `~/.processguard/config.json` | All settings, blacklist, whitelist, custom rules |
| Process snapshot | `~/.processguard/history_snapshots.json` | Latest process scan (overwritten each cycle) |
| Alert history | `~/.processguard/history_alerts.json` | Up to 1000 recent alerts (oldest evicted when full) |
| PDF reports | `~/Desktop/ProcessGuardReports/` | Exported PDF files |

### 13.2 Alert History Limit

ProcessGuard keeps a maximum of **1000 alerts** in history. Once this limit is reached, the oldest alert is removed to make room for the newest one. This prevents the history file from growing indefinitely.

### 13.3 Resetting Configuration

To start fresh, delete the `~/.processguard/` directory. ProcessGuard will recreate it with default settings on the next launch.

```bash
# Linux / macOS
rm -rf ~/.processguard/

# Windows (in Command Prompt)
rmdir /s %USERPROFILE%\.processguard
```

---

## 14. Frequently Asked Questions

**Q: ProcessGuard shows "0%" CPU for many processes. Is something wrong?**  
A: No. Many processes are idle and genuinely use 0% CPU between scans. CPU usage is sampled at a point in time; a process may spike and return to 0% within the 3-second scan window. On Windows, CPU estimation uses a delta calculation and may show 0% for the first scan cycle.

**Q: Some processes show "unknown" for their name and path. Why?**  
A: Some system processes protect their information from user-level access. ProcessGuard tries several fallback methods to resolve names, but some OS-protected processes will remain labelled "unknown". This is normal and not a bug.

**Q: Why is my browser highlighted yellow?**  
A: The default memory threshold is 500 MB, and modern browsers routinely use far more. Raise the `memoryThreshold` in your `config.json` to a value appropriate for your machine (e.g., 1500 MB for typical browsing).

**Q: I added a process to the blacklist but it isn't being blocked?**  
A: ProcessGuard detects and alerts on blacklisted processes but does not prevent them from running (unless you use a `KILL_PROCESS` custom rule). The `BLOCKED` classification and CRITICAL alert are informational. To automatically terminate a blacklisted process, create a custom rule with the `KILL_PROCESS` action.

**Q: How do I add a process to the blacklist without editing JSON?**  
A: Currently the blacklist can only be edited directly in `~/.processguard/config.json`. Open it in any text editor, add the process name to the `blacklist` array, save, and restart ProcessGuard.

**Q: Will ProcessGuard slow down my computer?**  
A: ProcessGuard is designed to be lightweight. It uses a single background daemon thread for scanning and relies on the OS's built-in process enumeration. On most systems, overhead is less than 1–2% CPU and under 150 MB of memory. Increasing the scan interval (e.g., to 10 s) reduces this further.

**Q: Can I run ProcessGuard on a server without a display?**  
A: Not in the current version — JavaFX requires a display. A headless/CLI mode is planned for a future release.

**Q: Where are my exported PDF reports?**  
A: All reports are saved to `~/Desktop/ProcessGuardReports/`. A dialog shows the exact path after each successful export.

**Q: How do I stop ProcessGuard from alerting on a trusted process?**  
A: Add the process name to the `whitelist` in `config.json`. Whitelisted processes are never alerted on.

**Q: Can I have multiple conditions in a custom rule?**  
A: The Rule Manager dialog creates single-condition rules for simplicity. For multi-condition rules, edit `config.json` directly — add multiple objects to the `conditions` array and set `logicOperator` to `"AND"` or `"OR"`.

---

## 15. Troubleshooting

### 15.1 Application Does Not Launch

**Symptom:** Nothing happens or an error appears when running `java -jar ProcessGuard-1.6.jar`.

**Check Java version:**
```
java -version
```
Must be Java 21 or later. If it shows Java 8, 11, or 17, install Java 21.

**Check JavaFX:**  
Some Java distributions (e.g., Eclipse Temurin without FX) do not include JavaFX. Use a distribution that bundles JavaFX, such as BellSoft Liberica JDK Full Edition or Azul Zulu with FX.

---

### 15.2 Process Table Is Empty

**Symptom:** The application opens but no processes appear in the table.

- Wait 5–10 seconds for the first scan to complete.
- Click **Refresh Now** to trigger an immediate scan.
- On Linux, ensure you have read access to `/proc`. Run `ls /proc/1/` to verify.
- On macOS, System Integrity Protection (SIP) may restrict process enumeration. Try running with `sudo java -jar ProcessGuard-1.6.jar` to test.

---

### 15.3 CPU Values Show 0% for All Processes on Windows

**Symptom:** All processes show `0.0%` CPU on Windows.

This is expected behaviour on the first scan cycle. ProcessGuard's Windows CPU estimation requires two scan results to compute a delta. CPU values will populate from the second scan cycle onwards.

---

### 15.4 Kill Process Fails

**Symptom:** Clicking Kill Process shows "Failed to kill process".

- **Windows:** Some system processes (PID < 100, `System`, `smss.exe`, etc.) are protected by the OS and cannot be killed by a normal user. Run ProcessGuard as Administrator if you need to kill protected processes.
- **macOS/Linux:** Processes owned by root or other users cannot be killed without elevated privileges. Run with `sudo` if needed.
- **Process already exited:** If the process exited between your click and the confirmation, the kill will fail. The process will disappear on the next scan.

---

### 15.5 Configuration Changes Are Not Taking Effect

**Symptom:** You edited `config.json` but ProcessGuard still uses old values.

ProcessGuard reads configuration at startup only. After editing `config.json`, restart the application. The next launch will use the updated values.

---

### 15.6 PDF Export Fails

**Symptom:** An error dialog appears when clicking Export Report.

- Ensure `~/Desktop/` exists. On some Linux configurations the Desktop folder may not exist; create it manually: `mkdir ~/Desktop`.
- Ensure you have write permission to `~/Desktop/ProcessGuardReports/`.
- If the error message mentions a file path with non-ASCII characters (accented letters, CJK characters), the PDF writer may have trouble — rename the path or create a symlink without special characters.