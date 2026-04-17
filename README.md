# ProcessGuard

A desktop system monitoring application that tracks running processes, classifies them using predefined and custom rules, and alerts users when suspicious activity is detected.

Built with Java 21 and JavaFX 21.

## Features

- **Real-time process monitoring** with configurable scan intervals
- **Automatic classification** of processes (Normal, Suspicious, Blocked, Whitelisted)
- **Built-in alert engine** for high CPU, high memory, blacklisted, and suspicious processes
- **Custom rule engine** with user-defined conditions, AND/OR logic, and configurable actions (Log, Alert, Kill)
- **Rule Manager UI** for creating, editing, and managing custom rules with drag-and-drop support
- **Process termination** via right-click context menu or sidebar kill button
- **PDF report export** with alert summaries and top resource-consuming processes
- **JSON-based persistence** for configuration, snapshots, and alert history

## Quick Start

**Prerequisites:** Java 21 or later

```bash
# Build the fat JAR
./gradlew shadowJar

# Run the application
java -jar build/libs/ProcessGuard-1.6.jar
```

## Documentation

- [User Guide](docs/UserGuide.md) — Setup, features, configuration, and usage instructions
- [Developer Guide](docs/DeveloperGuide.md) — Architecture, design patterns, class diagrams, and extension points
- [Product Requirements Document](docs/PRD.md) — Requirements and user stories

## Testing

```bash
# Run all tests (181 tests)
./gradlew test
```

## Project Structure

```
src/main/java/com/processguard/
├── ProcessGuardMain.java         # Application entry point
├── core/                         # Business logic
│   ├── ProcessMonitor.java       # Scheduled scan orchestrator
│   ├── ProcessScanner.java       # OS process enumeration
│   ├── AlertEngine.java          # Built-in alert rules
│   ├── CustomRuleEngine.java     # Custom rule orchestrator
│   ├── RuleEvaluator.java        # Rule condition matching
│   ├── RuleActionExecutor.java   # Rule action execution
│   ├── ReportExporter.java       # PDF report generation
│   ├── ProcessKiller.java        # Cross-platform process termination
│   ├── HistoryStorage.java       # JSON persistence
│   └── AppConfig.java            # Singleton configuration manager
├── models/                       # Data models and enums
├── listeners/                    # Observer interfaces
└── ui/                           # JavaFX user interface
    ├── MainDashboard.java        # Application shell and observer routing
    ├── ProcessTableManager.java  # Process table with sorting and context menu
    ├── AlertSidebarManager.java  # Alert list and process details panel
    ├── StatusBarManager.java     # Bottom status bar with live metrics
    ├── ToolbarManager.java       # Toolbar with monitoring controls
    └── RuleManagerDialog.java    # Custom rule creation dialog
```

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 21 |
| GUI | JavaFX 21 |
| Build | Gradle 8.12 (with Shadow plugin) |
| Storage | JSON (Gson) |
| Testing | JUnit 5 |

## Team

CS2103DE AY25/26 Semester 2 Team Project
