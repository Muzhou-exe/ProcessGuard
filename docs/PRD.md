# ProcessGuard - Product Requirements Document (PRD)

---

## Table of Contents

1. [Product Overview](#1-product-overview)
2. [Problem Statement](#2-problem-statement)
3. [Target Users and Stakeholders](#3-target-users-and-stakeholders)
4. [User Stories](#4-user-stories)
5. [Functional Requirements](#5-functional-requirements)
6. [Non-Functional Requirements](#6-non-functional-requirements)
7. [Assumptions and Dependencies](#7-assumptions-and-dependencies)
8. [Constraints](#8-constraints)
9. [Out of Scope](#9-out-of-scope)
10. [Future Enhancements](#10-future-enhancements)
11. [Success Metrics](#11-success-metrics)
12. [Glossary](#12-glossary)

---

## 1. Product Overview

**ProcessGuard** is a cross-platform desktop application written in Java 21 that monitors all running operating system processes in real time. It continuously scans the system, classifies each process using configurable rules, and alerts the user when suspicious or dangerous activity is detected.

Unlike raw system utilities such as Task Manager (Windows) or `top` (Unix), ProcessGuard combines intelligent classification, persistent alert history, user-defined custom rules, and a graphical dashboard into a single lightweight application. Users gain visibility into what is running on their machines, receive actionable alerts, and can take corrective action — including terminating processes — directly from the interface.

**Key capabilities at a glance:**

| Capability | Description |
|---|---|
| Live process monitoring | Scans all system processes every configurable interval (default: 3 s) |
| Intelligent classification | Classifies processes as NORMAL, SUSPICIOUS, BLOCKED, or WHITELISTED |
| Alert engine | Generates graded severity alerts for CPU, memory, blacklisted, and suspicious processes |
| Custom rule engine | Lets users define condition-based rules with AND / OR logic and configurable actions |
| Process termination | Kill any process directly from the dashboard with a confirmation dialog |
| PDF report export | Generates a timestamped summary PDF with alert frequency and resource-heavy processes |
| Persistent history | Stores process snapshots and alert history in JSON files under `~/.processguard/` |
| Cross-platform | Supports Windows, macOS, and Linux with platform-specific OS command fallbacks |

---

## 2. Problem Statement

### 2.1 Background

Modern computers routinely run dozens to hundreds of background processes at any moment. Many of these are legitimate OS services or application helpers; however, some may be malicious programs, runaway processes consuming excessive resources, or unknown applications installed without the user's awareness.

### 2.2 Gaps in Existing Tools

Standard system utilities (e.g., Task Manager, Activity Monitor, `htop`) present raw process data to the user. They do not:

- Classify processes by risk level
- Generate alerts when thresholds are exceeded
- Allow user-defined detection rules
- Persist monitoring history for retrospective analysis
- Allow direct process termination with guided UI

Users are left to interpret raw numbers manually, which is time-consuming and error-prone, especially for non-technical users.

### 2.3 The Problem ProcessGuard Solves

ProcessGuard bridges the gap by providing:

1. **Automatic classification:** Every process is labelled NORMAL, SUSPICIOUS, BLOCKED, or WHITELISTED on each scan cycle without user intervention.
2. **Graded alerting:** Alerts are prioritised by severity (LOW → CRITICAL) so users address the most serious issues first.
3. **Custom rule authoring:** Users or administrators can write rules targeting specific process names, executable paths, CPU usage, memory usage, or parent PID — without writing code.
4. **History and audit trail:** All alerts and process snapshots are persisted locally in JSON so users can review past incidents.
5. **One-click remediation:** Processes can be terminated from the same screen where the alert is displayed.

---

## 3. Target Users and Stakeholders

### 3.1 Primary Users

#### Developers
Software developers who need to monitor what processes their applications spawn during development and testing. They care about CPU and memory consumption, parent-child process relationships, and detecting unintended background activity.

**Key needs:** Low overhead, custom rules tied to specific executables, accurate CPU/memory metrics.

#### System Administrators
IT professionals responsible for maintaining machine health. They may monitor multiple machines and want blacklist-based protection for endpoints.

**Key needs:** Blacklist and whitelist management, alert history export, ability to kill rogue processes.

### 3.2 Secondary Users

#### Students
Students studying operating systems, cybersecurity, or system programming who want a practical tool for understanding how processes behave.

**Key needs:** Simple UI, clear classification labels, educational value of visible rules.

#### Security Enthusiasts
Power users who suspect unwanted programs are running and want a low-cost monitoring tool.

**Key needs:** Suspicious process detection, process path inspection, alert notifications.

#### General Computer Users
Non-technical users who want basic protection against sluggish or suspicious applications.

**Key needs:** Simple, intuitive interface; clear alerts; easy process termination.

### 3.3 Stakeholders

| Stakeholder | Interest |
|---|---|
| Project developers (Group 5) | Successful implementation and academic evaluation |
| Academic supervisors | Correct application of software engineering principles |
| End users | Reliable, lightweight monitoring with actionable alerts |
| System administrators | Stability, configurability, and cross-platform compatibility |

---

## 4. User Stories

### 4.1 Process Monitoring

| ID | User Story | Priority |
|---|---|---|
| US-01 | As a user, I want to see all running processes in a table so that I can monitor my system at a glance. | Must Have |
| US-02 | As a user, I want processes colour-coded by status (normal, suspicious, blocked) so that I can instantly identify problems. | Must Have |
| US-03 | As a user, I want to see CPU and memory usage for each process so that I can identify resource hogs. | Must Have |
| US-04 | As a user, I want to see the executable path of each process so that I can verify its origin. | Must Have |
| US-05 | As a user, I want to see the parent PID so that I can trace process hierarchies. | Should Have |
| US-06 | As a user, I want to sort the table by any column so that I can focus on the most relevant data. | Should Have |
| US-07 | As a user, I want the table to refresh automatically so that I always see current data. | Must Have |

### 4.2 Alerts

| ID | User Story | Priority |
|---|---|---|
| US-08 | As a user, I want an alert when CPU usage exceeds my configured threshold so that I can investigate runaway processes. | Must Have |
| US-09 | As a user, I want an alert when memory usage exceeds my configured threshold so that I can address memory leaks. | Must Have |
| US-10 | As a user, I want an alert when a blacklisted process is detected so that I can immediately respond. | Must Have |
| US-11 | As a user, I want alerts displayed with severity colours so that I can prioritise my response. | Should Have |
| US-12 | As a user, I want duplicate alerts suppressed so that I am not spammed by repeated notifications. | Should Have |
| US-13 | As a user, I want to view a history of all past alerts so that I can track when incidents occurred. | Should Have |

### 4.3 Custom Rules

| ID | User Story | Priority |
|---|---|---|
| US-14 | As a user, I want to create custom detection rules via a UI dialog so that I do not need to edit JSON files manually. | Must Have |
| US-15 | As a user, I want to combine multiple conditions with AND / OR logic in a rule so that I can express nuanced detection criteria. | Must Have |
| US-16 | As a user, I want to assign a severity level and action to each rule so that I control how violations are handled. | Must Have |
| US-17 | As a user, I want to enable or disable rules individually so that I can toggle monitoring without deleting rules. | Should Have |
| US-18 | As a user, I want to drag a process row onto the rule dialog to auto-populate rule fields so that rule creation is faster. | Nice to Have |
| US-19 | As a user, I want an automatic kill action on a rule so that I can block certain processes from running entirely. | Should Have |

### 4.4 Configuration

| ID | User Story | Priority |
|---|---|---|
| US-20 | As a user, I want to configure the CPU and memory alert thresholds so that they match my machine's normal behaviour. | Must Have |
| US-21 | As a user, I want to add processes to a whitelist so that trusted processes do not trigger false alerts. | Must Have |
| US-22 | As a user, I want to add processes to a blacklist so that specific processes always trigger a CRITICAL alert. | Must Have |
| US-23 | As a user, I want the scan interval to be configurable so that I can balance responsiveness with CPU overhead. | Should Have |
| US-24 | As a user, I want configuration changes to persist between sessions so that I do not have to reconfigure each time. | Must Have |

### 4.5 Process Interaction

| ID | User Story | Priority |
|---|---|---|
| US-25 | As a user, I want to kill a process from the sidebar kill button so that I can respond to alerts without leaving the app. | Must Have |
| US-26 | As a user, I want to kill a process from the right-click context menu so that I have a consistent and discoverable interaction. | Must Have |
| US-27 | As a user, I want a confirmation dialog before killing a process so that I do not accidentally terminate critical processes. | Must Have |
| US-28 | As a user, I want to copy a process's PID to clipboard so that I can use it in terminal commands. | Nice to Have |
| US-29 | As a user, I want to flag a process with a custom reason so that I can mark processes for later investigation. | Nice to Have |

### 4.6 Reporting

| ID | User Story | Priority |
|---|---|---|
| US-30 | As a user, I want to export a PDF report of the session's alert activity and resource-heavy processes so that I have a shareable audit record. | Should Have |
| US-31 | As a user, I want PDF reports saved automatically with a timestamped filename so that I never overwrite previous reports. | Should Have |

### 4.7 History

| ID | User Story | Priority |
|---|---|---|
| US-32 | As a user, I want alert history stored in a local file so that it survives application restarts. | Should Have |
| US-33 | As a user, I want the alert history capped at 1000 entries so that storage does not grow unboundedly. | Should Have |
| US-34 | As a user, I want the last process snapshot saved to disk so that I can reference it after closing the app. | Nice to Have |

---

## 5. Functional Requirements

### 5.1 Process Scanning (FR-SCAN)

| ID | Requirement |
|---|---|
| FR-SCAN-01 | The system shall enumerate all running processes using the Java 9+ `ProcessHandle.allProcesses()` API on every scan cycle. |
| FR-SCAN-02 | The system shall collect CPU usage and memory usage metrics for each process using platform-specific commands (`tasklist /fo csv /nh` on Windows; `ps -axo pid=,rss=,%cpu=` on macOS/Linux). |
| FR-SCAN-03 | The system shall resolve process names from the executable path via `ProcessHandle.info().command()`; on failure it shall fall back to a `ps -p <pid>` lookup (macOS/Linux) or tasklist name (Windows). |
| FR-SCAN-04 | The system shall detect newly started processes by comparing the current scan result with the previous snapshot (PID set difference). |
| FR-SCAN-05 | The system shall detect exited processes by identifying PIDs present in the previous snapshot but absent from the current scan. |
| FR-SCAN-06 | The system shall run scans on a configurable interval using a `ScheduledExecutorService` on a background daemon thread named `ProcessGuard-Monitor`. |
| FR-SCAN-07 | The system shall estimate CPU usage from `ProcessHandle.totalCpuDuration()` delta on platforms where `ps` or `tasklist` does not provide CPU data. |

### 5.2 Process Classification (FR-CLASS)

| ID | Requirement |
|---|---|
| FR-CLASS-01 | Each process shall be assigned one of four statuses: `NORMAL`, `SUSPICIOUS`, `BLOCKED`, or `WHITELISTED`. |
| FR-CLASS-02 | A process whose lowercase name or executable path matches any entry in the whitelist shall be classified as `WHITELISTED`. |
| FR-CLASS-03 | A process whose lowercase name or executable path matches any entry in the blacklist shall be classified as `BLOCKED`. |
| FR-CLASS-04 | A process whose CPU usage exceeds the configured CPU threshold or whose memory usage exceeds the configured memory threshold shall be classified as `SUSPICIOUS`. |
| FR-CLASS-05 | All other processes shall be classified as `NORMAL`. |
| FR-CLASS-06 | Whitelist check shall take precedence over blacklist check. |

### 5.3 Alert Engine (FR-ALERT)

| ID | Requirement |
|---|---|
| FR-ALERT-01 | The alert engine shall evaluate every process on each snapshot update against four built-in rules in priority order: BLOCKED → HIGH_CPU → HIGH_MEMORY → SUSPICIOUS. |
| FR-ALERT-02 | Each evaluation shall fire at most one alert per process per scan cycle (first matching rule only). |
| FR-ALERT-03 | Alert deduplication shall be enforced using a composite key of `PID + AlertType`; a given (PID, type) combination shall not fire a second alert until the process returns to NORMAL status. |
| FR-ALERT-04 | When a process returns to NORMAL, all active alert keys for that PID shall be cleared, allowing re-triggering if the condition reoccurs. |
| FR-ALERT-05 | The system shall support the following alert types: `HIGH_CPU_USAGE`, `HIGH_MEMORY_USAGE`, `BLACKLISTED_PROCESS`, `UNKNOWN_PROCESS`, `SUSPICIOUS_PARENT`, `RAPID_SPAWN`, `CUSTOM_RULE_VIOLATION`. |
| FR-ALERT-06 | The system shall support four severity levels: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`. |
| FR-ALERT-07 | The alert engine shall persist each generated alert via `HistoryStorage.saveAlert()`. |
| FR-ALERT-08 | The alert engine shall notify all registered `AlertListener` observers upon generating a new alert. |

### 5.4 Custom Rule Engine (FR-RULE)

| ID | Requirement |
|---|---|
| FR-RULE-01 | The system shall support user-defined rules loaded from `AppConfig`. |
| FR-RULE-02 | Each rule shall consist of one or more `Condition` objects, a logic operator (`AND` / `OR`), a severity, an action, a message template, and a cooldown period. |
| FR-RULE-03 | `AND` logic shall require all conditions in the rule to match for the rule to fire. |
| FR-RULE-04 | `OR` logic shall require at least one condition to match for the rule to fire. |
| FR-RULE-05 | Conditions shall support the following fields: `name`, `executablePath`, `cpuUsage`, `memoryUsageMB`, `pid`, `parentPid`. |
| FR-RULE-06 | String fields (`name`, `executablePath`) shall support operators `EQUALS` (case-insensitive) and `CONTAINS` (case-insensitive). |
| FR-RULE-07 | Numeric fields (`cpuUsage`, `memoryUsageMB`, `pid`, `parentPid`) shall support operators `GREATER_THAN`, `LESS_THAN`, and `EQUALS`. |
| FR-RULE-08 | Disabled rules shall be skipped during evaluation without being deleted. |
| FR-RULE-09 | The system shall support three rule actions: `LOG_ONLY` (console log only), `ALERT_ONLY` (generate alert and persist), `KILL_PROCESS` (generate alert then terminate process). |
| FR-RULE-10 | Rule matching shall be pure (no side effects inside `RuleEvaluator`); actions shall be executed by `RuleActionExecutor` only. |

### 5.5 Configuration Management (FR-CONFIG)

| ID | Requirement |
|---|---|
| FR-CONFIG-01 | Configuration shall be stored in `~/.processguard/config.json` using Gson-serialised JSON. |
| FR-CONFIG-02 | The application shall load configuration at startup; if the file does not exist, it shall create it with default values. |
| FR-CONFIG-03 | Any setter call on `AppConfig` shall persist the configuration immediately by rewriting the JSON file. |
| FR-CONFIG-04 | The configurable parameters shall include: `scanIntervalSeconds` (min 1), `cpuThreshold` (min 0), `memoryThreshold` (min 0), `blacklist` (set of strings), `whitelist` (set of strings), `customRules` (list), `webPort`, `startMinimized`, `enableSystemTray`. |
| FR-CONFIG-05 | `AppConfig` shall be a thread-safe singleton using double-checked locking. |
| FR-CONFIG-06 | Configuration shall survive application restarts without data loss. |

### 5.6 History Storage (FR-HIST)

| ID | Requirement |
|---|---|
| FR-HIST-01 | The system shall persist the latest process snapshot to `~/.processguard/history_snapshots.json`, overwriting the previous snapshot on each cycle. |
| FR-HIST-02 | The system shall persist all alert events to `~/.processguard/history_alerts.json`. |
| FR-HIST-03 | Alert history shall be capped at 1000 entries; when the cap is reached the oldest entry shall be evicted (FIFO). |
| FR-HIST-04 | Storage operations shall be synchronised to prevent concurrent write corruption. |
| FR-HIST-05 | The system shall provide `getRecentSnapshots()` and `getRecentAlerts()` methods returning defensive copies. |

### 5.7 User Interface (FR-UI)

| ID | Requirement |
|---|---|
| FR-UI-01 | The main dashboard shall display a toolbar, a process table, an alert sidebar, and a status bar. |
| FR-UI-02 | The process table shall display the following columns: PID, Process Name, Executable Path, CPU %, Memory MB, Status, Parent PID, Start Time, Captured At. |
| FR-UI-03 | Table rows shall be visually highlighted: red for BLOCKED, yellow for SUSPICIOUS. |
| FR-UI-04 | The table shall support column sorting; default sort shall be by CPU usage descending. |
| FR-UI-05 | The alert sidebar shall display the 10 most recent alerts with colour-coded severity: red for HIGH/CRITICAL, orange for MEDIUM, blue for LOW. |
| FR-UI-06 | The alert sidebar shall include a process details panel showing PID, name, path, CPU, memory, parent PID, and status of the selected process. |
| FR-UI-07 | A Kill Process button in the sidebar shall terminate the selected process after user confirmation. |
| FR-UI-08 | The toolbar shall provide Start Monitoring, Stop Monitoring, Refresh Now, Open Configuration, Export Report, and Close App buttons. |
| FR-UI-09 | The status bar shall display: total process count, suspicious count, blocked count, total CPU usage, total memory usage, and last scan time. |
| FR-UI-10 | The right-click context menu on a process row shall offer: Kill Process, Copy PID, View Details, Flag/Unflag Process. |
| FR-UI-11 | All UI updates from background threads shall be dispatched via `Platform.runLater()`. |

### 5.8 Rule Manager Dialog (FR-RMD)

| ID | Requirement |
|---|---|
| FR-RMD-01 | The Rule Manager dialog shall be accessible from the toolbar and displayed as a non-modal window. |
| FR-RMD-02 | The dialog shall allow creation of new rules by entering a name, field, operator, value, and action. |
| FR-RMD-03 | The dialog shall list all existing rules with their enabled/disabled status. |
| FR-RMD-04 | The dialog shall allow deletion of a selected rule. |
| FR-RMD-05 | The dialog shall allow toggling (enable/disable) of a selected rule. |
| FR-RMD-06 | The dialog shall provide an "Add Sample Rule" button that adds a predefined rule detecting high-CPU Chrome. |
| FR-RMD-07 | The dialog shall support drag-and-drop from the process table to auto-populate the rule name, field, and value fields. |

### 5.9 Process Termination (FR-KILL)

| ID | Requirement |
|---|---|
| FR-KILL-01 | The system shall terminate processes using `taskkill /PID <pid> /F` on Windows and `kill -9 <pid>` on macOS/Linux. |
| FR-KILL-02 | Process termination shall execute on a background thread to avoid blocking the JavaFX Application Thread. |
| FR-KILL-03 | A confirmation dialog shall be shown before any process is terminated via the UI. |
| FR-KILL-04 | The kill method shall return a boolean indicating success or failure. |
| FR-KILL-05 | The system shall refuse to kill processes with a PID of 0 or below. |

### 5.10 PDF Report Export (FR-REPORT)

| ID | Requirement |
|---|---|
| FR-REPORT-01 | The system shall generate a PDF report using raw PDF 1.4 byte construction with no external PDF library dependencies. |
| FR-REPORT-02 | The report shall contain: (1) Top Repeat Alerts section with alert type, count, and max severity; (2) Heaviest Processes section showing top 5 by CPU and top 5 by memory. |
| FR-REPORT-03 | Reports shall be saved to `~/Desktop/ProcessGuardReports/` with a filename of format `report_YYYYMMDD_HHmmss.pdf`. |
| FR-REPORT-04 | Report export shall execute on a background thread. |
| FR-REPORT-05 | A confirmation dialog shall display the saved file path upon successful export. |

---

## 6. Non-Functional Requirements

### 6.1 Performance

| ID | Requirement |
|---|---|
| NFR-PERF-01 | The system shall complete a full process scan cycle within 3 seconds on machines with up to 500 running processes. |
| NFR-PERF-02 | The ProcessGuard application itself shall consume less than 3% CPU on average during steady-state monitoring. |
| NFR-PERF-03 | The UI shall remain responsive at all times; background scans shall never block the JavaFX Application Thread. |
| NFR-PERF-04 | The system shall handle at least 500 concurrently running processes without degradation in UI frame rate. |

### 6.2 Reliability

| ID | Requirement |
|---|---|
| NFR-REL-01 | The monitoring thread shall catch and log all exceptions internally and continue scanning without crashing the application. |
| NFR-REL-02 | If configuration file loading fails, the system shall fall back to default values and log a warning rather than crashing. |
| NFR-REL-03 | If a `HistoryStorage` write fails, the system shall log the error and continue execution. |
| NFR-REL-04 | Observer notification failures shall be isolated; an exception in one listener shall not prevent others from receiving the event. |

### 6.3 Usability

| ID | Requirement |
|---|---|
| NFR-USE-01 | A new user shall be able to start monitoring and view all running processes within 30 seconds of launching the application. |
| NFR-USE-02 | Alert severity shall be communicated by both colour and label so that the UI is accessible to colour-blind users. |
| NFR-USE-03 | All destructive actions (kill process, delete rule) shall require user confirmation. |
| NFR-USE-04 | Form fields in the Rule Manager shall include prompt text describing the expected input. |

### 6.4 Security

| ID | Requirement |
|---|---|
| NFR-SEC-01 | The application shall not transmit any process data over the network. |
| NFR-SEC-02 | All configuration and history files shall be stored in the user's home directory under `~/.processguard/`, accessible only to the current OS user. |
| NFR-SEC-03 | The web server port (if used) shall be configurable and default to localhost only (port 8080). |

### 6.5 Maintainability

| ID | Requirement |
|---|---|
| NFR-MAINT-01 | The system shall follow a layered architecture (UI → Core → Service → OS) with no upward dependencies. |
| NFR-MAINT-02 | Core business logic shall be decoupled from the UI via the Observer pattern (ProcessListener, AlertListener). |
| NFR-MAINT-03 | Pure logic components (RuleEvaluator) shall be free of side effects for testability. |
| NFR-MAINT-04 | The system shall achieve at least 80% unit test coverage across model and core classes. |

### 6.6 Portability

| ID | Requirement |
|---|---|
| NFR-PORT-01 | The application shall run on Windows 10+, macOS 12+, and Ubuntu 20.04+ without code modification. |
| NFR-PORT-02 | Platform-specific code (process metrics collection, process termination) shall be isolated behind conditional branches keyed on `os.name`. |
| NFR-PORT-03 | The application shall require only Java 21 and JavaFX 21; no native binaries shall be required. |

### 6.7 Scalability

| ID | Requirement |
|---|---|
| NFR-SCALE-01 | Alert history shall be bounded at 1000 entries to prevent unbounded disk or memory usage. |
| NFR-SCALE-02 | Adding new alert types, rule conditions, or rule actions shall require changes only to enums and the relevant engine class, not to the core observer infrastructure. |

---

## 7. Assumptions and Dependencies

| Assumption / Dependency | Detail |
|---|---|
| Java 21 available | The host machine has Java 21 (JDK or JRE) installed and on the system PATH. |
| JavaFX 21 available | JavaFX 21 is bundled via the shadow JAR or available in the JDK distribution used. |
| OS process permission | The running user has permission to enumerate system processes via `ProcessHandle.allProcesses()`. |
| OS kill permission | The running user has permission to send SIGKILL / force-terminate at least user-owned processes. |
| `ps` / `tasklist` available | Unix platforms have `ps` available; Windows has `tasklist` available in PATH. |
| Desktop path exists | `~/Desktop/` exists for PDF report output (or will be created). |
| Gson library available | Gson is included as a Gradle dependency and bundled in the fat JAR. |

---

## 8. Constraints

| Constraint | Detail |
|---|---|
| Language | Java 21 only |
| UI framework | JavaFX 21 only |
| Build tool | Gradle 8.12 with Shadow plugin |
| Data storage | JSON files only (no external database, no SQLite) |
| PDF generation | Raw PDF 1.4 byte construction; no iText, PDFBox, or other PDF library |
| Test framework | JUnit 5 only |
| Network | No mandatory network connectivity; the app must function entirely offline |

---

## 9. Out of Scope

The following items are explicitly out of scope for version 1.6:

- Remote multi-machine monitoring
- Network traffic monitoring
- File system event monitoring
- Antivirus or signature-based detection
- Machine-learning-based anomaly detection
- User authentication or login
- Cloud synchronisation of configuration or history

---

## 10. Future Enhancements

| Enhancement | Description |
|---|---|
| ML anomaly detection | Train a model on normal process behaviour and flag statistical outliers |
| Remote monitoring dashboard | Optional web server serving a real-time monitoring dashboard accessible from a browser |
| Automatic process blocking | Prevent blacklisted processes from launching using OS-level hooks |
| Notification integrations | Push alerts to Slack, email, or OS native notification system |
| Performance analytics | Track CPU/memory trends over time with sparkline charts in the UI |
| Virus definition integration | Cross-reference process hashes with VirusTotal or local signature databases |
| System tray minimisation | Minimise to the system tray icon rather than closing the application |

---

## 11. Success Metrics

The product will be considered successful if all of the following criteria are met:

| Metric | Target |
|---|---|
| Process detection accuracy | All running processes on the host OS are visible in the table within one scan cycle |
| Alert accuracy | Alerts fire within one scan cycle of a threshold being exceeded |
| False positive rate | Fewer than 5% of alerts require user dismissal as false positives under default thresholds |
| Rule engine correctness | All 181 unit tests pass with 0 failures |
| UI responsiveness | No visible UI freeze during scan cycles on a machine with 200+ processes |
| PDF export | Report generates and saves within 3 seconds |
| Configuration persistence | All configuration changes survive application restart |

---

## 12. Glossary

| Term | Definition |
|---|---|
| **Alert** | A notification generated when a process violates a built-in or custom rule |
| **Blacklist** | A set of process names whose presence always triggers a CRITICAL alert |
| **BLOCKED** | Process classification status for blacklisted processes |
| **CPU Threshold** | The CPU usage percentage above which a process is flagged as SUSPICIOUS |
| **Custom Rule** | A user-defined rule consisting of one or more conditions, logic operator, severity, action, and message |
| **Deduplication** | Suppression of repeated alerts for the same (PID, AlertType) combination until the condition clears |
| **Memory Threshold** | The memory usage in MB above which a process is flagged as SUSPICIOUS |
| **NORMAL** | Process classification status for well-behaved, non-suspicious, non-whitelisted processes |
| **Observer Pattern** | Design pattern where subjects notify registered listeners of state changes |
| **PID** | Process Identifier — unique integer assigned by the OS to each running process |
| **Process Snapshot** | The full list of ProcessInfo objects captured in a single scan cycle |
| **Severity** | A graded importance level for alerts: LOW, MEDIUM, HIGH, CRITICAL |
| **SUSPICIOUS** | Process classification status for processes exceeding CPU or memory thresholds |
| **Whitelist** | A set of trusted process names that are never flagged or alerted |
| **WHITELISTED** | Process classification status for whitelisted processes |
