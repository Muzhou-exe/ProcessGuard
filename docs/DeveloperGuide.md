# ProcessGuard Developer Guide

---

## 1. Introduction

This guide provides an overview of the architecture, design decisions, and implementation details of ProcessGuard. It is intended for developers who wish to understand, maintain, or extend the system.

### 1.1 Purpose

ProcessGuard is a desktop system monitoring application that tracks running processes, classifies them using predefined and custom rules, and alerts users when suspicious activity is detected.

### 1.2 Technology Stack

| Component | Technology |
|---|---|
| Language | Java 21 |
| GUI Framework | JavaFX 21 |
| Build Tool | Gradle 8.12 (with Shadow plugin) |
| Data Storage | JSON (via Gson) |
| Testing | JUnit 5 |

---

## 2. Architecture Overview

ProcessGuard follows a layered architecture with clear separation of concerns:

```
┌──────────────────────────────────────────────────────────────────┐
│                          UI Layer                                │
│  MainDashboard │ ProcessTableManager │ AlertSidebarManager      │
│  StatusBarManager │ ToolbarManager │ RuleManagerDialog           │
├──────────────────────────────────────────────────────────────────┤
│                        Core Layer                                │
│  ProcessMonitor │ AlertEngine │ CustomRuleEngine                 │
│  RuleEvaluator │ RuleActionExecutor │ ReportExporter             │
├──────────────────────────────────────────────────────────────────┤
│                       Service Layer                              │
│  ProcessScanner │ ProcessKiller │ HistoryStorage │ AppConfig     │
├──────────────────────────────────────────────────────────────────┤
│                     System / OS Layer                            │
│          ProcessHandle API │ tasklist │ ps │ kill                │
└──────────────────────────────────────────────────────────────────┘
```

### 2.1 Component Interactions

The following describes the flow of data through the system during a single scan cycle:

1. `ProcessMonitor` triggers `ProcessScanner.scanProcesses()` on a scheduled interval.
2. `ProcessScanner` enumerates system processes via `ProcessHandle.allProcesses()` and collects CPU/memory metrics using platform-specific commands (`tasklist` on Windows, `ps` on macOS/Linux).
3. `ProcessScanner` classifies each process (NORMAL, SUSPICIOUS, BLOCKED, WHITELISTED) based on `AppConfig` blacklist/whitelist and thresholds.
4. `ProcessMonitor` compares the new snapshot with the previous one to detect new and exited processes.
5. `ProcessMonitor` notifies all registered `ProcessListener` observers (AlertEngine, CustomRuleEngine, MainDashboard).
6. `AlertEngine` evaluates built-in rules and fires `AlertEvent` objects to registered `AlertListener` observers.
7. `CustomRuleEngine` delegates to `RuleEvaluator` for condition matching and `RuleActionExecutor` for executing actions (log, alert, or kill).
8. `HistoryStorage` persists the snapshot and any alerts to JSON files.

---

## 3. Design Patterns

### 3.1 Observer Pattern

ProcessGuard uses the Observer pattern extensively for loose coupling between components.

**ProcessListener interface:**
- `onNewProcesses(List<ProcessInfo>)` — called when new processes are detected
- `onExitedProcesses(List<ProcessInfo>)` — called when processes exit
- `onSnapshotUpdate(List<ProcessInfo>)` — called with the full snapshot after each scan

**Implementations:** `AlertEngine`, `CustomRuleEngine`, `MainDashboard`

**AlertListener interface:**
- `onAlert(AlertEvent)` — called when a new alert is generated

**Implementations:** `MainDashboard`

```
ProcessMonitor ──notifies──► ProcessListener
                                 ├── AlertEngine ──notifies──► AlertListener
                                 │                                  └── MainDashboard
                                 ├── CustomRuleEngine
                                 │       ├── RuleEvaluator (matching)
                                 │       └── RuleActionExecutor ──notifies──► AlertListener
                                 │                                               └── MainDashboard
                                 └── MainDashboard
```

### 3.2 Singleton Pattern

`AppConfig` is implemented as a thread-safe singleton using double-checked locking. This ensures a single source of truth for all configuration values across the application.

```java
public static AppConfig getInstance() {
    if (instance == null) {
        synchronized (AppConfig.class) {
            if (instance == null) {
                instance = new AppConfig();
            }
        }
    }
    return instance;
}
```

### 3.3 Strategy Pattern

`RuleActionExecutor` implements the Strategy pattern for rule actions. Each `RuleAction` enum value (`LOG_ONLY`, `ALERT_ONLY`, `KILL_PROCESS`) triggers a different execution path:

- **LOG_ONLY**: Logs the violation to console without creating an alert.
- **ALERT_ONLY**: Creates an `AlertEvent`, persists it via `HistoryStorage`, and notifies `AlertListener` observers.
- **KILL_PROCESS**: Performs the ALERT_ONLY behaviour, then terminates the process via `ProcessKiller`.

### 3.4 Delegation Pattern

`MainDashboard` delegates UI responsibilities to four specialised manager classes:

- `ProcessTableManager` — process table, columns, row factory, context menu, row highlighting
- `AlertSidebarManager` — alert list, process details panel, kill button
- `StatusBarManager` — status bar labels and live metrics
- `ToolbarManager` — toolbar buttons and their event handlers

This keeps `MainDashboard` focused on lifecycle management and observer routing.

---

## 4. Class Descriptions

### 4.1 Models

| Class | Description |
|---|---|
| `ProcessInfo` | Snapshot of a system process (PID, name, executable path, CPU, memory, status, parent PID, start time). Equality based on PID. |
| `AlertEvent` | Represents a single alert with type, severity, message, timestamp, and optional triggering rule reference. |
| `CustomRule` | User-defined rule with conditions, logic operator, severity, message template, cooldown, and action. |
| `Condition` | A single condition within a custom rule (field, operator, value). |
| `RuleAction` | Enum: `LOG_ONLY`, `ALERT_ONLY`, `KILL_PROCESS`. Includes a `fromString()` factory method with safe default. |
| `ConfigState` | DTO for JSON serialisation of configuration (scan interval, thresholds, blacklist, whitelist, custom rules). |
| `Status` | Enum: NORMAL, SUSPICIOUS, BLOCKED, WHITELISTED |
| `Severity` | Enum: LOW, MEDIUM, HIGH, CRITICAL |
| `AlertType` | Enum: HIGH_CPU_USAGE, HIGH_MEMORY_USAGE, BLACKLISTED_PROCESS, UNKNOWN_PROCESS, SUSPICIOUS_PARENT, RAPID_SPAWN, CUSTOM_RULE_VIOLATION |

### 4.2 Core

| Class | Description |
|---|---|
| `ProcessScanner` | Enumerates running processes using Java `ProcessHandle` API with platform-specific fallbacks for metrics. Stateless and thread-safe. |
| `ProcessMonitor` | Orchestrates periodic scanning, detects process changes (new/exited), and notifies observers. Uses `ScheduledExecutorService`. |
| `AlertEngine` | Evaluates built-in alert rules (blacklist, CPU, memory, suspicious) and manages alert deduplication via an `activeAlerts` set. Implements `ProcessListener`. |
| `CustomRuleEngine` | Orchestrates user-defined rule evaluation. Delegates matching to `RuleEvaluator` and action execution to `RuleActionExecutor`. Implements `ProcessListener`. |
| `RuleEvaluator` | Pure matching logic for custom rules. Evaluates conditions against process fields (name, executablePath, cpuUsage, memoryUsageMB, pid, parentPid) with AND/OR logic. No side effects. |
| `RuleActionExecutor` | Executes actions for matched rules: logging, alerting, or killing processes. Manages its own `AlertListener` list. |
| `ReportExporter` | Generates PDF summary reports using raw PDF 1.4 syntax (zero external dependencies). Includes top alerts by frequency and heaviest processes by CPU/memory. |
| `ProcessKiller` | Cross-platform process termination utility. Uses `taskkill /F` on Windows and `kill -9` on macOS/Linux. |
| `HistoryStorage` | Persists process snapshots and alert history to JSON files in `~/.processguard/`. Alert history capped at 1000 entries. |
| `AppConfig` | Singleton configuration manager. Loads/saves JSON config via `ConfigState` DTO from `~/.processguard/config.json`. |

### 4.3 UI

| Class | Description |
|---|---|
| `MainDashboard` | JavaFX `Application` entry point. Assembles the UI layout, registers observers, and routes callbacks to manager classes. Implements both `ProcessListener` and `AlertListener`. |
| `ProcessTableManager` | Manages the process `TableView`: column setup, `SortedList` binding, row factory with context menu (kill, copy PID, view details), drag-and-drop initiation, and row highlighting based on status/thresholds. |
| `AlertSidebarManager` | Manages the alert `ListView` with severity-based colour rendering, a process details panel showing selected process info, and a kill button with confirmation dialog. |
| `StatusBarManager` | Manages the bottom status bar displaying process count, total CPU/memory usage, suspicious count, and blocked count. |
| `ToolbarManager` | Creates toolbar buttons (Start, Stop, Refresh, Open Configuration, Export Report) and wires their event handlers. |
| `RuleManagerDialog` | Modal-less dialog for creating, deleting, and toggling custom rules. Supports drag-and-drop from the process table to auto-fill rule fields. |

### 4.4 Listeners

| Interface | Description |
|---|---|
| `ProcessListener` | Observer interface for process monitoring events (new, exited, snapshot). |
| `AlertListener` | Observer interface for alert events. |

---

## 5. Class Diagram

```
┌──────────────────┐     implements     ┌──────────────────┐
│  ProcessListener │◄───────────────────│   AlertEngine    │
│  «interface»     │                    │                  │
│  +onNewProcesses │                    │  -alertHistory   │
│  +onExitedProc.  │                    │  -activeAlerts   │
│  +onSnapshotUpd. │                    │  +addAlertList.()│
└──────────────────┘                    │  +getAlertHist.()│
        ▲                               └────────┬─────────┘
        │ implements                             │ notifies
        │                                        ▼
┌───────┴──────────┐                    ┌──────────────────┐
│ CustomRuleEngine │                    │  AlertListener   │
│                  │                    │  «interface»     │
│  -ruleEvaluator  │──delegates──►      │  +onAlert()      │
│  -actionExecutor │──delegates──►      └──────────────────┘
└──────────────────┘                            ▲
        ▲                                       │ implements
        │ implements                            │
┌───────┴──────────┐    delegates       ┌───────┴──────────┐
│  MainDashboard   │──────────────────►│  ProcessMonitor  │
│  «Application»   │                    │                  │
│                  │                    │  -scanner        │
│  -tableManager   │                    │  -lastSnapshot   │
│  -alertSidebar   │                    │  -listeners      │
│  -statusBar      │                    │  +start()        │
│  -toolbarMgr     │                    │  +stop()         │
└──────────────────┘                    │  +scanNow()      │
        │                               └────────┬─────────┘
        │ delegates to                           │ uses
        ▼                                        ▼
┌──────────────────┐                    ┌──────────────────┐
│ProcessTableMgr   │                    │ ProcessScanner   │
│AlertSidebarMgr   │                    │                  │
│StatusBarMgr      │                    │  +scanProcesses()│
│ToolbarMgr        │                    │  +getOwnPid()    │
│RuleManagerDialog │                    └──────────────────┘
└──────────────────┘

┌──────────────────┐    ┌──────────────┐    ┌──────────────┐
│ RuleEvaluator    │    │RuleActionExec│    │ ProcessKiller│
│                  │    │              │    │              │
│  +matches()      │    │  +execute()  │    │  +kill(pid)  │
│  -evaluateCond() │    │  -notify...()│    └──────────────┘
└──────────────────┘    └──────────────┘

┌──────────────────┐    ┌──────────────┐    ┌──────────────┐
│   ProcessInfo    │    │  AlertEvent  │    │  CustomRule   │
│                  │    │              │    │              │
│  -pid            │    │  -id         │    │  -id         │
│  -name           │    │  -process    │    │  -name       │
│  -executablePath │    │  -type       │    │  -conditions │
│  -cpuUsage       │    │  -severity   │    │  -logicOp    │
│  -memoryUsageMB  │    │  -message    │    │  -severity   │
│  -status         │    │  -timestamp  │    │  -action     │
│  -parentPid      │    │  -trigRule   │    │  -cooldown   │
│  +copy()         │    └──────────────┘    └──────┬───────┘
└──────────────────┘                               │ has many
                                                   ▼
┌──────────────────┐    ┌──────────────┐    ┌──────────────┐
│    AppConfig     │    │ ConfigState  │    │  Condition   │
│  «singleton»     │    │  «DTO»       │    │              │
│  -scanInterval   │    │  -scanInt.   │    │  -field      │
│  -cpuThreshold   │    │  -cpuThresh. │    │  -operator   │
│  -blacklist      │    │  -memThresh. │    │  -value      │
│  -whitelist      │    │  -blacklist  │    └──────────────┘
│  -customRules    │    │  -whitelist  │
│  +getInstance()  │    │  -customRules│    ┌──────────────┐
└──────────────────┘    └──────────────┘    │  RuleAction  │
                                            │  «enum»      │
┌──────────────────┐    ┌──────────────┐    │  LOG_ONLY    │
│ HistoryStorage   │    │ReportExporter│    │  ALERT_ONLY  │
│                  │    │              │    │  KILL_PROCESS │
│  +saveSnapshot() │    │  +export()   │    └──────────────┘
│  +saveAlert()    │    └──────────────┘
│  +getRecent...() │
└──────────────────┘
```

---

## 6. Sequence Diagram: Scan Cycle

```
User     MainDashboard    ProcessMonitor    ProcessScanner    AlertEngine    CustomRuleEngine    HistoryStorage
 │            │                │                 │                │                │                │
 │            │     scheduled tick               │                │                │                │
 │            │                │─────────────────>                │                │                │
 │            │                │  scanProcesses()│                │                │                │
 │            │                │                 │                │                │                │
 │            │                │  List<Process>  │                │                │                │
 │            │                │<─────────────────                │                │                │
 │            │                │                                  │                │                │
 │            │                │──── detectNewProcesses() ────>   │                │                │
 │            │                │──── detectExitedProcesses() ──>  │                │                │
 │            │                │                                  │                │                │
 │            │  onSnapshot    │                                  │                │                │
 │            │<───────────────│                                  │                │                │
 │            │ (via runLater) │── onSnapshotUpdate() ──────────> │                │                │
 │            │                │                                  │                │                │
 │            │                │── onSnapshotUpdate() ─────────────────────────────>│                │
 │            │                │                                  │                │                │
 │            │                │                   checkBuiltInRules()             │                │
 │            │                │                                  │                │                │
 │            │                │                                  │ RuleEvaluator  │                │
 │            │                │                                  │   .matches()   │                │
 │            │                │                                  │                │                │
 │            │                │                                  │ RuleActionExec │                │
 │            │                │                                  │   .execute()───────saveAlert()──>
 │            │                │                                  │                │                │
 │            │                │───────── saveSnapshot() ──────────────────────────────────────────>
 │            │                │                                                                   │
 │  UI updated│                │                                                                   │
 │<───────────│                │                                                                   │
```

---

## 7. Alert Engine Logic

The `AlertEngine` evaluates each process against built-in rules in the following priority order:

1. **BLOCKED status** → `BLACKLISTED_PROCESS` alert (CRITICAL)
2. **CPU > threshold** → `HIGH_CPU_USAGE` alert (HIGH)
3. **Memory > threshold** → `HIGH_MEMORY_USAGE` alert (MEDIUM)
4. **SUSPICIOUS status** → `UNKNOWN_PROCESS` alert (LOW)

Only the first matching rule fires an alert per process per cycle. Alerts are deduplicated using a composite key of `PID + AlertType` stored in an `activeAlerts` set. When a process returns to NORMAL, its alert keys are cleared, allowing the alert to fire again if the condition reoccurs.

---

## 8. Custom Rule Engine Logic

The `CustomRuleEngine` evaluates user-defined rules from `AppConfig` using a two-phase approach:

### Phase 1: Matching (RuleEvaluator)

1. For each process, iterate through all enabled rules.
2. For each rule, evaluate all conditions against process fields using type-aware comparisons:
   - **String fields** (`name`, `executablePath`): case-insensitive `EQUALS` and `CONTAINS`
   - **Numeric fields** (`cpuUsage`, `memoryUsageMB`, `pid`, `parentPid`): `GREATER_THAN`, `LESS_THAN`, `EQUALS`
3. Apply the logic operator (`AND` requires all conditions to match, `OR` requires any).

### Phase 2: Action Execution (RuleActionExecutor)

If a rule matches, `RuleActionExecutor` executes the rule's configured action:

- **LOG_ONLY**: Print to console, no alert generated.
- **ALERT_ONLY**: Create `AlertEvent` with `CUSTOM_RULE_VIOLATION` type, persist via `HistoryStorage`, notify `AlertListener` observers.
- **KILL_PROCESS**: Perform ALERT_ONLY behaviour, then call `ProcessKiller.kill(pid)` to terminate the process.

---

## 9. Data Storage Design

All data is stored as JSON files in `~/.processguard/`:

- **config.json** — Application settings, loaded at startup by `AppConfig` via `ConfigState` DTO.
- **history_snapshots.json** — The latest process snapshot (overwritten each cycle).
- **history_alerts.json** — Rolling alert history, capped at 1000 entries (FIFO eviction).

JSON serialisation is handled by Gson with pretty printing enabled.

---

## 10. Platform Support

### Process Scanning

| Platform | Process Enumeration | CPU Metrics | Memory Metrics |
|---|---|---|---|
| All | `ProcessHandle.allProcesses()` (Java 9+) | — | — |
| Windows | — | Not available via tasklist | `tasklist /fo csv /nh` |
| macOS/Linux | `ps -p <pid> -o comm=` (name fallback) | `ps -axo pid=,rss=,%cpu=` | `ps -axo pid=,rss=,%cpu=` |

### Process Name Resolution

1. Try `ProcessHandle.info().command()` to get the executable path, then extract the filename.
2. If the name is "unknown", fall back to `ps` lookup (macOS/Linux only).
3. On Windows, names from `tasklist` are used when `ProcessHandle` returns no command.

### Process Termination

| Platform | Command |
|---|---|
| Windows | `taskkill /PID <pid> /F` |
| macOS/Linux | `kill -9 <pid>` |

---

## 11. Threading Model

ProcessGuard uses multiple threads with the following safety considerations:

- **ProcessGuard-Monitor thread**: `ScheduledExecutorService` thread that runs periodic scans. Observer callbacks (`onSnapshotUpdate`, `onNewProcesses`, `onExitedProcesses`) are invoked on this thread.
- **JavaFX Application Thread**: All UI updates must run on this thread. `MainDashboard` wraps observer callbacks in `Platform.runLater()` to ensure thread safety. `AlertSidebarManager.addAlert()` also uses `Platform.runLater()` internally.
- **Background threads**: PDF export and process termination run on separate daemon threads to avoid blocking the UI.

---

## 12. Testing

Tests are located in `src/test/java/com/processguard/` and use JUnit 5. There are **181 tests** across 10 test classes.

| Test Class | Covers |
|---|---|
| `ProcessInfoTest` | Constructor, null handling, copy, equality |
| `AlertEventTest` | Constructor, null validation, acknowledgement |
| `CustomRuleTest` | Constructor, logic operator defaults, defensive copies |
| `ConditionTest` | Constructor, null handling, trimming |
| `EnumTest` | Verifies all enum values exist |
| `AlertEngineTest` | Built-in rule evaluation, deduplication, alert clearing |
| `CustomRuleEngineTest` | Custom rule matching, AND/OR logic, disabled rules |
| `HistoryStorageTest` | Snapshot save/replace, alert cap, empty defaults |
| `AppConfigTest` | Singleton, defaults, clamping, blacklist/whitelist |
| `ProcessMonitorTest` | Lifecycle (start/stop), scan, observer notification |

Run all tests:
```
./gradlew test
```

---

## 13. Build and Run

### Build the project

```
./gradlew build
```

### Run the application

```
./gradlew run
```

### Build a fat JAR

```
./gradlew shadowJar
```

The output JAR is located at `build/libs/ProcessGuard-1.6.jar`.

---

## 14. Project Structure

```
ProcessGuard/
├── build.gradle
├── gradlew / gradlew.bat
├── docs/
│   ├── PRD.md
│   ├── UserGuide.md
│   └── DeveloperGuide.md
└── src/
    ├── main/java/com/processguard/
    │   ├── ProcessGuardMain.java          # Entry point
    │   ├── core/
    │   │   ├── AlertEngine.java           # Built-in alert rules
    │   │   ├── AppConfig.java             # Singleton configuration
    │   │   ├── ConfigState.java           # Configuration DTO for JSON
    │   │   ├── CustomRuleEngine.java      # Custom rule orchestrator
    │   │   ├── HistoryStorage.java        # JSON persistence
    │   │   ├── ProcessKiller.java         # Cross-platform process termination
    │   │   ├── ProcessMonitor.java        # Scheduled scan orchestrator
    │   │   ├── ProcessScanner.java        # OS process enumeration
    │   │   ├── ReportExporter.java        # PDF report generation
    │   │   ├── RuleActionExecutor.java    # Rule action execution
    │   │   └── RuleEvaluator.java         # Rule condition matching
    │   ├── listeners/
    │   │   ├── AlertListener.java         # Alert observer interface
    │   │   └── ProcessListener.java       # Process observer interface
    │   ├── models/
    │   │   ├── AlertEvent.java            # Alert data model
    │   │   ├── AlertType.java             # Alert type enum
    │   │   ├── Condition.java             # Rule condition model
    │   │   ├── CustomRule.java            # Custom rule model
    │   │   ├── ProcessInfo.java           # Process data model
    │   │   ├── RuleAction.java            # Rule action enum
    │   │   ├── Severity.java              # Severity enum
    │   │   └── Status.java               # Process status enum
    │   └── ui/
    │       ├── MainDashboard.java         # Application shell and observer routing
    │       ├── ProcessTableManager.java   # Process table with sorting and context menu
    │       ├── AlertSidebarManager.java   # Alert list and process details panel
    │       ├── StatusBarManager.java      # Bottom status bar with live metrics
    │       ├── ToolbarManager.java        # Toolbar with monitoring controls
    │       └── RuleManagerDialog.java     # Custom rule creation dialog
    └── test/java/com/processguard/
        ├── core/
        │   ├── AlertEngineTest.java
        │   ├── AppConfigTest.java
        │   ├── CustomRuleEngineTest.java
        │   ├── HistoryStorageTest.java
        │   └── ProcessMonitorTest.java
        └── models/
            ├── AlertEventTest.java
            ├── ConditionTest.java
            ├── CustomRuleTest.java
            ├── EnumTest.java
            └── ProcessInfoTest.java
```
