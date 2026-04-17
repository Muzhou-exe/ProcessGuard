# ProcessGuard User Guide

---

## 1. Introduction

ProcessGuard is a desktop system monitoring application that tracks running processes and identifies suspicious behaviour in real time. It helps users detect abnormal CPU usage, memory spikes, and unknown applications running on their device.

### Target Users

- Developers monitoring applications during development and testing
- System administrators who need a lightweight monitoring tool
- Students learning operating systems or security concepts

### System Requirements

- Java 21 or later
- Windows, macOS, or Linux
- Sufficient permissions to read system process information

---

## 2. Quick Start

1. Ensure you have Java 21 installed. Verify by running:
   ```
   java -version
   ```

2. Download the latest `ProcessGuard-1.6.jar` from the [Releases](https://github.com/Muzhou-exe/ProcessGuard/releases) page.

3. Run the application:
   ```
   java -jar ProcessGuard-1.6.jar
   ```

4. The main dashboard window will appear, displaying all running processes on your system.

---

## 3. Features

### 3.1 Main Dashboard

The main dashboard is divided into four sections:

- **Toolbar** (top): Contains control buttons for monitoring, configuration, and report export.
- **Process Table** (centre): Displays all running processes with detailed information.
- **Alert Sidebar** (right): Shows recent alerts and a process details panel with a kill button.
- **Status Bar** (bottom): Displays summary statistics including process count, CPU/memory totals, and suspicious/blocked counts.

### 3.2 Process Monitoring

ProcessGuard continuously scans all running system processes at a configurable interval (default: 3 seconds).

Each process displays the following information:

| Column | Description |
|---|---|
| PID | Process ID |
| Process Name | Name of the executable |
| Executable Path | Full path to the executable |
| CPU % | Current CPU usage percentage |
| Memory MB | Memory usage in megabytes |
| Status | Classification: NORMAL, SUSPICIOUS, BLOCKED, or WHITELISTED |
| Parent PID | Parent process ID |
| Start Time | When the process started |
| Captured At | When the snapshot was taken |

The table supports **column sorting** by clicking column headers. By default, processes are sorted by CPU usage (descending).

### 3.3 Process Classification

Each process is automatically classified into one of four statuses:

| Status | Meaning | Row Colour |
|---|---|---|
| NORMAL | Process is safe and operating normally | No highlight |
| SUSPICIOUS | Process exceeds CPU or memory thresholds | Yellow |
| BLOCKED | Process matches the blacklist | Red |
| WHITELISTED | Process is explicitly trusted by the user | No highlight |

### 3.4 Alerts

ProcessGuard generates alerts for the following conditions:

| Alert Type | Severity | Trigger |
|---|---|---|
| Blacklisted Process | CRITICAL | Process name matches the blacklist |
| High CPU Usage | HIGH | CPU usage exceeds the configured threshold |
| High Memory Usage | MEDIUM | Memory usage exceeds the configured threshold |
| Unknown/Suspicious Process | LOW | Process is classified as SUSPICIOUS |
| Custom Rule Violation | Configurable | Process matches a user-defined custom rule |

Alerts appear in the **Alert Sidebar** on the right side of the dashboard. The 10 most recent alerts are displayed, with severity-based colour coding:

- **HIGH/CRITICAL** — Red text with bold styling
- **MEDIUM** — Orange text
- **LOW** — Blue text

Duplicate alerts for the same process and alert type are suppressed until the condition clears and reoccurs.

### 3.5 Process Termination

ProcessGuard allows you to terminate running processes in two ways:

1. **Right-click context menu**: Right-click any process row in the table and select **Kill Process**.
2. **Sidebar kill button**: Click a process row to view its details in the sidebar, then click the red **Kill Process** button.

Both methods display a confirmation dialog before terminating the process. On Windows, the process is terminated using `taskkill /F`; on macOS/Linux, `kill -9` is used.

### 3.6 Process Details Panel

Click any row in the process table to view detailed information in the sidebar panel, including:

- PID, process name, and executable path
- Current CPU and memory usage
- Parent PID and process status

The context menu also provides **Copy PID** (copies the PID to clipboard) and **View Details** options.

### 3.7 Custom Rules

Users can define custom monitoring rules through the **Rule Manager UI** or the configuration file. Each rule consists of:

- **Conditions**: What to check (process name, executable path, CPU usage, memory usage, PID, or parent PID)
- **Logic Operator**: `AND` (all conditions must match) or `OR` (any condition suffices)
- **Severity**: LOW, MEDIUM, HIGH, or CRITICAL
- **Action**: The action to take when the rule is triggered

**Supported condition fields:**

| Field | Operators | Description |
|---|---|---|
| `name` | `EQUALS`, `CONTAINS` | Process name |
| `executablePath` | `EQUALS`, `CONTAINS` | Full executable path |
| `cpuUsage` | `GREATER_THAN`, `LESS_THAN`, `EQUALS` | CPU usage percentage |
| `memoryUsageMB` | `GREATER_THAN`, `LESS_THAN`, `EQUALS` | Memory usage in MB |
| `pid` | `GREATER_THAN`, `LESS_THAN`, `EQUALS` | Process ID |
| `parentPid` | `GREATER_THAN`, `LESS_THAN`, `EQUALS` | Parent process ID |

**Supported actions:**

| Action | Description |
|---|---|
| `LOG_ONLY` | Log the violation to the console without generating an alert |
| `ALERT_ONLY` | Generate an alert in the sidebar and save to alert history |
| `KILL_PROCESS` | Generate an alert and automatically terminate the matching process |

### 3.8 Rule Manager UI

Click **Open Configuration** in the toolbar to open the Rule Manager dialog. The dialog provides:

- **Create New Rule**: Fill in the rule name, condition field, operator, match value, and action, then click **Add Rule**.
- **Add Sample Rule**: Click to add a pre-configured example rule (detects Chrome with high CPU).
- **Delete Selected**: Remove the selected rule from the configuration.
- **Enable / Disable**: Toggle the selected rule on or off without deleting it.

The existing rules list shows each rule's name and enabled status.

**Drag-and-drop support**: Drag a process row from the main process table and drop it onto the Rule Manager dialog. The rule form will automatically populate with the process name, making it easy to create rules for specific processes.

### 3.9 PDF Report Export

Click **Export Report** in the toolbar to generate a PDF summary report. The report includes:

1. **Top Repeat Alerts** — Alert types that fired most frequently during the session, with counts and severity levels.
2. **Heaviest Processes** — Top 5 processes ranked by CPU usage and top 5 ranked by memory usage from the current snapshot.

Reports are saved to `~/Desktop/ProcessGuardReports/` with a timestamped filename (e.g., `report_20260417_143022.pdf`). A confirmation dialog shows the full file path after export.

### 3.10 Monitoring Controls

The toolbar provides the following controls:

| Button | Function |
|---|---|
| Start Monitoring | Begins periodic process scanning |
| Stop Monitoring | Stops periodic scanning |
| Refresh Now | Performs an immediate scan |
| Open Configuration | Opens the Rule Manager dialog for creating and managing custom rules |
| Export Report | Generates and saves a PDF summary report |

---

## 4. Configuration

ProcessGuard stores its configuration in `~/.processguard/config.json`. The file is created automatically on first run with default values.

### 4.1 Configuration Options

| Option | Default | Description |
|---|---|---|
| `scanIntervalSeconds` | 3 | Time between scans in seconds (minimum: 1) |
| `cpuThreshold` | 20.0 | CPU usage percentage to trigger alerts |
| `memoryThreshold` | 500.0 | Memory usage in MB to trigger alerts |
| `blacklist` | [] | List of blocked process names |
| `whitelist` | [] | List of trusted process names |
| `customRules` | [] | List of user-defined custom rules |

### 4.2 Example Configuration

```json
{
  "scanIntervalSeconds": 5,
  "cpuThreshold": 90.0,
  "memoryThreshold": 1024.0,
  "blacklist": ["malware.exe", "suspicious.exe"],
  "whitelist": ["chrome.exe", "code.exe"],
  "customRules": []
}
```

### 4.3 Custom Rule Example

```json
{
  "customRules": [
    {
      "id": 1,
      "name": "High CPU Chrome",
      "description": "Alert when Chrome uses too much CPU",
      "enabled": true,
      "conditions": [
        { "field": "name", "operator": "CONTAINS", "value": "chrome" },
        { "field": "cpuUsage", "operator": "GREATER_THAN", "value": "50" }
      ],
      "logicOperator": "AND",
      "severity": "HIGH",
      "messageTemplate": "Chrome is using excessive CPU",
      "cooldownSeconds": 60,
      "action": "ALERT_ONLY"
    }
  ]
}
```

---

## 5. Data Storage

ProcessGuard stores monitoring data in `~/.processguard/`:

| File | Contents |
|---|---|
| `config.json` | Application configuration and custom rules |
| `history_snapshots.json` | Latest process snapshot |
| `history_alerts.json` | Alert history (max 1000 entries) |

All data is stored locally in JSON format. No external database is required.

---

## 6. FAQ

**Q: ProcessGuard shows "unknown" for some process names. Why?**
A: Some system processes restrict access to their executable path. ProcessGuard falls back to alternative methods (e.g., the `ps` command on macOS/Linux) but some processes may still appear as "unknown".

**Q: How do I add a process to the blacklist?**
A: Edit `~/.processguard/config.json` and add the process name (lowercase) to the `blacklist` array. Restart ProcessGuard for changes to take effect.

**Q: Can I change the scan interval while the application is running?**
A: Currently, the scan interval is read from the configuration file at startup. Restart ProcessGuard after changing the value in `config.json`.

**Q: Where are exported PDF reports saved?**
A: Reports are saved to `~/Desktop/ProcessGuardReports/` with a timestamped filename.

**Q: What happens when I kill a process?**
A: ProcessGuard sends a force-kill signal to the operating system (`taskkill /F` on Windows, `kill -9` on macOS/Linux). The process will be removed from the table on the next scan cycle. A confirmation dialog always appears before termination.

**Q: Can I create rules from the UI instead of editing JSON?**
A: Yes. Click **Open Configuration** in the toolbar to open the Rule Manager dialog, where you can create, delete, and toggle custom rules. You can also drag a process from the table into the dialog to auto-fill the rule form.
