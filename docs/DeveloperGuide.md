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
| Build Tool | Gradle 8.12 |
| Data Storage | JSON (via Gson) |
| Testing | JUnit 5 |

---

## 2. Architecture Overview

ProcessGuard follows a layered architecture with clear separation of concerns:

```
┌─────────────────────────────────────────────────┐
│                   UI Layer                      │
│              (MainDashboard)                    │
├─────────────────────────────────────────────────┤
│                 Core Layer                      │
│  ProcessMonitor │ AlertEngine │ CustomRuleEngine│
├─────────────────────────────────────────────────┤
│               Service Layer                     │
│     ProcessScanner │ HistoryStorage │ AppConfig │
├─────────────────────────────────────────────────┤
│              System / OS Layer                  │
│       ProcessHandle API │ tasklist │ ps         │
└─────────────────────────────────────────────────┘
```

### 2.1 Component Interactions

The following describes the flow of data through the system during a single scan cycle:

1. `ProcessMonitor` triggers `ProcessScanner.scanProcesses()` on a scheduled interval.
2. `ProcessScanner` enumerates system processes via `ProcessHandle.allProcesses()` and collects CPU/memory metrics using platform-specific commands (`tasklist` on Windows, `ps` on macOS/Linux).
3. `ProcessScanner` classifies each process (NORMAL, SUSPICIOUS, BLOCKED, WHITELISTED) based on `AppConfig` blacklist/whitelist and thresholds.
4. `ProcessMonitor` compares the new snapshot with the previous one to detect new and exited processes.
5. `ProcessMonitor` notifies all registered `ProcessListener` observers (AlertEngine, CustomRuleEngine, MainDashboard).
6. `AlertEngine` evaluates built-in rules and fires `AlertEvent` objects to registered `AlertListener` observers.
7. `HistoryStorage` persists the snapshot and any alerts to JSON files.

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

---

## 4. Class Descriptions

### 4.1 Models

| Class | Description |
|---|---|
| `ProcessInfo` | Immutable snapshot of a system process (PID, name, CPU, memory, status). Equality based on PID. |
| `AlertEvent` | Represents a single alert with type, severity, message, and timestamp. Equality based on ID. |
| `CustomRule` | User-defined rule with conditions, logic operator, severity, and action. |
| `Condition` | A single condition within a custom rule (field, operator, value). |
| `Status` | Enum: NORMAL, SUSPICIOUS, BLOCKED, WHITELISTED |
| `Severity` | Enum: LOW, MEDIUM, HIGH, CRITICAL |
| `AlertType` | Enum: HIGH_CPU_USAGE, HIGH_MEMORY_USAGE, BLACKLISTED_PROCESS, UNKNOWN_PROCESS, SUSPICIOUS_PARENT, RAPID_SPAWN, CUSTOM_RULE_VIOLATION |

### 4.2 Core

| Class | Description |
|---|---|
| `ProcessScanner` | Enumerates running processes using Java `ProcessHandle` API with platform-specific fallbacks for metrics. Stateless and thread-safe. |
| `ProcessMonitor` | Orchestrates periodic scanning, detects process changes, and notifies observers. Uses `ScheduledExecutorService`. |
| `AlertEngine` | Evaluates built-in alert rules (blacklist, CPU, memory, suspicious) and manages alert deduplication. Implements `ProcessListener`. |
| `CustomRuleEngine` | Evaluates user-defined rules from configuration against processes. Implements `ProcessListener`. |
| `HistoryStorage` | Persists process snapshots and alert history to JSON files in `~/.processguard/`. |
| `AppConfig` | Singleton configuration manager. Loads/saves JSON config from `~/.processguard/config.json`. |

### 4.3 UI

| Class | Description |
|---|---|
| `MainDashboard` | JavaFX application with process table, alert sidebar, and status bar. Implements both `ProcessListener` and `AlertListener`. |

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
        ▲                              └────────┬─────────┘
        │ implements                            │ notifies
        │                                       ▼
┌───────┴──────────┐                    ┌──────────────────┐
│ CustomRuleEngine │                    │  AlertListener   │
│                  │                    │  «interface»     │
│  +onNewProcesses │                    │  +onAlert()      │
│  +onSnapshotUpd. │                    └──────────────────┘
└──────────────────┘                            ▲
        ▲                                       │ implements
        │ implements                            │
┌───────┴──────────┐    uses            ┌───────┴──────────┐
│  MainDashboard   │◄──────────────────►│  ProcessMonitor  │
│  «Application»   │                    │                  │
│  -processTable   │                    │  -scanner        │
│  -alertList      │                    │  -lastSnapshot   │
│  -statusBar      │                    │  -listeners      │
└──────────────────┘                    │  +start()        │
                                        │  +stop()         │
                                        │  +scanNow()      │
                                        └────────┬─────────┘
                                                 │ uses
                                                 ▼
                                        ┌──────────────────┐
                                        │ ProcessScanner   │
                                        │                  │
                                        │  +scanProcesses()│
                                        │  +getOwnPid()    │
                                        └──────────────────┘

┌──────────────────┐    ┌──────────────┐    ┌──────────────┐
│   ProcessInfo    │    │  AlertEvent  │    │  CustomRule   │
│                  │    │              │    │              │
│  -pid            │    │  -id         │    │  -id         │
│  -name           │    │  -process    │    │  -name       │
│  -cpuUsage       │    │  -type       │    │  -conditions │
│  -memoryUsageMB  │    │  -severity   │    │  -logicOp    │
│  -status         │    │  -message    │    │  -severity   │
│  +copy()         │    │  -timestamp  │    │  -action     │
└──────────────────┘    └──────────────┘    └──────┬───────┘
                                                   │ has many
                                                   ▼
┌──────────────────┐                       ┌──────────────┐
│    AppConfig     │                       │  Condition   │
│  «singleton»     │                       │              │
│  -scanInterval   │                       │  -field      │
│  -cpuThreshold   │                       │  -operator   │
│  -blacklist      │                       │  -value      │
│  -whitelist      │                       └──────────────┘
│  -customRules    │
│  +getInstance()  │
└──────────────────┘

┌──────────────────┐
│ HistoryStorage   │
│                  │
│  +saveSnapshot() │
│  +saveAlert()    │
│  +getRecent...() │
└──────────────────┘
```

---

## 6. Sequence Diagram: Scan Cycle

```
User        MainDashboard    ProcessMonitor    ProcessScanner    AlertEngine    HistoryStorage
 │               │                │                 │                │               │
 │               │     scheduled tick               │                │               │
 │               │                │─────────────────>                │               │
 │               │                │  scanProcesses()│                │               │
 │               │                │                 │                │               │
 │               │                │  List<Process>  │                │               │
 │               │                │<─────────────────                │               │
 │               │                │                                  │               │
 │               │                │──── detectNewProcesses() ────>   │               │
 │               │                │──── detectExitedProcesses() ──>  │               │
 │               │                │                                  │               │
 │               │  onSnapshot    │                                  │               │
 │               │<───────────────│                                  │               │
 │               │                │── onSnapshotUpdate() ──────────> │               │
 │               │                │                                  │               │
 │               │                │                   checkBuiltInRules()            │
 │               │                │                                  │──saveAlert()──>
 │               │                │                                  │               │
 │               │                │───────── saveSnapshot() ─────────────────────────>
 │               │                │                                                  │
 │  UI updated   │                │                                                  │
 │<──────────────│                │                                                  │
```

---

## 7. Alert Engine Logic

The `AlertEngine` evaluates each process against built-in rules in the following priority order:

1. **BLOCKED status** → `BLACKLISTED_PROCESS` alert (CRITICAL)
2. **CPU > threshold** → `HIGH_CPU_USAGE` alert (HIGH)
3. **Memory > threshold** → `HIGH_MEMORY_USAGE` alert (MEDIUM)
4. **SUSPICIOUS status** → `UNKNOWN_PROCESS` alert (LOW)

Only the first matching rule fires an alert per process per cycle. Alerts are deduplicated using a composite key of `PID + AlertType`. When a process returns to NORMAL, its alert keys are cleared, allowing the alert to fire again if the condition reoccurs.

---

## 8. Custom Rule Engine Logic

The `CustomRuleEngine` evaluates user-defined rules from `AppConfig`:

1. For each process, iterate through all enabled rules.
2. For each rule, evaluate all conditions against the process.
3. Apply the logic operator (AND requires all conditions to match, OR requires any).
4. If a rule matches, generate a `CUSTOM_RULE_VIOLATION` alert and stop evaluating further rules for that process.

---

## 9. Data Storage Design

All data is stored as JSON files in `~/.processguard/`:

- **config.json** — Application settings, loaded at startup by `AppConfig`.
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

---

## 11. Testing

Tests are located in `src/test/java/com/processguard/` and use JUnit 5.

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

## 12. Build and Run

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

## 13. Project Structure

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
    │   │   ├── CustomRuleEngine.java       # User-defined rules
    │   │   ├── HistoryStorage.java         # JSON persistence
    │   │   ├── ProcessMonitor.java         # Scan orchestrator
    │   │   └── ProcessScanner.java         # OS process enumeration
    │   ├── listeners/
    │   │   ├── AlertListener.java          # Alert observer interface
    │   │   └── ProcessListener.java        # Process observer interface
    │   ├── models/
    │   │   ├── AlertEvent.java             # Alert data model
    │   │   ├── AlertType.java              # Alert type enum
    │   │   ├── Condition.java              # Rule condition model
    │   │   ├── CustomRule.java             # Custom rule model
    │   │   ├── ProcessInfo.java            # Process data model
    │   │   ├── Severity.java               # Severity enum
    │   │   └── Status.java                 # Process status enum
    │   └── ui/
    │       └── MainDashboard.java          # JavaFX dashboard
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
