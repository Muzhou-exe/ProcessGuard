# SOFTWARE DESIGN DOCUMENT (SDD)

---

1. SYSTEM OVERVIEW
   ============================================================

ProcessGuard is a real-time process monitoring system that continuously scans system processes, evaluates them against both built-in and user-defined rules, and triggers alerts or automated actions.

Core capabilities:
- Real-time process tracking
- Rule-based anomaly detection
- Automated responses (alert / kill)
- Persistent history storage
- Interactive UI dashboard

------------------------------------------------------------
HIGH-LEVEL FLOW
------------------------------------------------------------

ProcessScanner --> ProcessMonitor --> Rule Engines --> Alert System --> UI

============================================================
2. ARCHITECTURE DESIGN
   ============================================================

LAYERED ARCHITECTURE:
```
+----------------------------------------------------------+
|                        UI LAYER                          |
|----------------------------------------------------------|
| MainDashboard                                            |
| ProcessTableManager  AlertSidebarManager                 |
| StatusBarManager     ToolbarManager                      |
| RuleManagerDialog                                        |
+----------------------------------------------------------+
|                        CORE LAYER                        |
|----------------------------------------------------------|
| ProcessMonitor     AlertEngine                           |
| CustomRuleEngine   RuleEvaluator                         |
| RuleActionExecutor ReportExporter                        |
+----------------------------------------------------------+
|                     SERVICE LAYER                        |
|----------------------------------------------------------|
| ProcessScanner     ProcessKiller                         |
| HistoryStorage     AppConfig                             |
+----------------------------------------------------------+
|                     MODEL LAYER                          |
|----------------------------------------------------------|
| ProcessInfo  AlertEvent  CustomRule  Condition           |
| Status       Severity    RuleAction  AlertType           |
+----------------------------------------------------------+
```

============================================================
3. FULL CLASS DIAGRAM
   ============================================================
```
                     +----------------------+
                     |  ProcessMonitor      |
                     |----------------------|
                     | -scanner             |
                     | -listeners           |
                     | -lastSnapshot        |
                     |----------------------|
                     | +start()             |
                     | +stop()              |
                     | +scanNow()           |
                     +----------+-----------+
                                |
                                | uses
                                v
                     +----------------------+
                     |  ProcessScanner      |
                     |----------------------|
                     | +scanProcesses()     |
                     | +getOwnPid()         |
                     +----------------------+

                                |
                                | notifies
                                v

   +-------------------------------------------------------------+
   |                  ProcessListener (interface)                |
   |-------------------------------------------------------------|
   | +onNewProcesses()                                           |
   | +onExitedProcesses()                                        |
   | +onSnapshotUpdate()                                         |
   +-------------------+-------------------+---------------------+
   |                   |
   |                   |
   v                   v

         +------------------------+    +------------------------+
         |     AlertEngine        |    |   CustomRuleEngine     |
         |------------------------|    |------------------------|
         | -activeAlerts          |    | -ruleEvaluator         |
         | -alertHistory          |    | -actionExecutor        |
         |------------------------|    |------------------------|
         | +onSnapshotUpdate()    |    | +onSnapshotUpdate()    |
         | +addAlertListener()    |    +-----------+------------+
         +-----------+------------+                |
                     |                             | delegates
                     |                             v
                     |                 +-------------------------+
                     |                 |    RuleEvaluator        |
                     |                 |-------------------------|
                     |                 | +matches()              |
                     |                 +-------------------------+
                     |
                     |                             |
                     |                             v
                     |                 +-------------------------+
                     |                 | RuleActionExecutor      |
                     |                 |-------------------------|
                     |                 | +execute()              |
                     |                 +-----------+-------------+
                     |                             |
                     |                             v
                     |                 +-------------------------+
                     |                 |   ProcessKiller         |
                     |                 |-------------------------|
                     |                 | +kill(pid)              |
                     |                 +-------------------------+
                     |
                     v
        +---------------------------+
        |   AlertListener           |
        |---------------------------|
        | +onAlert()                |
        +------------+--------------+
                     |
                     v
        +---------------------------+
        |    MainDashboard          |
        |---------------------------|
        | -tableManager             |
        | -alertSidebar             |
        | -statusBar                |
        | -toolbarManager           |
        +------------+--------------+
                     |
                     v
   +--------------------------------------------------------+
   | UI MANAGERS                                            |
   |--------------------------------------------------------|
   | ProcessTableManager                                    |
   | AlertSidebarManager                                    |
   | StatusBarManager                                       |
   | ToolbarManager                                         |
   | RuleManagerDialog                                      |
   +--------------------------------------------------------+
```
------------------------------------------------------------
MODEL RELATIONSHIPS
------------------------------------------------------------
```
CustomRule
|
+----> Condition (1..*)
|
+----> RuleAction (enum)

AlertEvent
|
+----> ProcessInfo
|
+----> Severity
|
+----> AlertType

ProcessInfo
|
+----> Status

AppConfig (Singleton)
|
+----> ConfigState (DTO)
|
+----> List<CustomRule>

HistoryStorage
|
+----> AlertEvent (persisted)
|
+----> ProcessInfo (snapshot)
```
============================================================
4. SEQUENCE DIAGRAM (SCAN CYCLE)
   ============================================================
```
User
|
v
MainDashboard
|
v
ProcessMonitor -----> ProcessScanner -----> OS
|                    |                 |
|<-------------------|                 |
|
|---- detect new/exited processes ----|
|
|---- notify listeners --------------> AlertEngine
|                                     |
|                                     v
|                                 evaluate rules
|                                     |
|                                     v
|                                 AlertEvent
|                                     |
|                                     v
|-------------------------------> MainDashboard (UI)

      |
      |-------------------------------> CustomRuleEngine
                                            |
                                            v
                                     RuleEvaluator
                                            |
                                            v
                                     RuleActionExecutor
                                            |
                                            v
                                     ProcessKiller (optional)
```
============================================================
5. USE CASE DIAGRAM (TEXTUAL)
   ============================================================
```
         +------------------+
         |      USER        |
         +--------+---------+
                  |
   +--------------+----------------------+
   |              |                      |
   v              v                      v
   View Processes   Manage Rules        Export Reports
   |              |                      |
   v              v                      v
   Monitor System   Create/Delete      Generate PDF
   Rules
```
SYSTEM ACTIONS:
- Auto detect suspicious processes
- Auto generate alerts
- Auto kill malicious processes

============================================================
6. KEY DESIGN DECISIONS
   ============================================================

1. Layered architecture for separation of concerns
2. Observer pattern for decoupled communication
3. Singleton pattern for global configuration
4. Strategy pattern for rule execution
5. Delegation pattern for UI modularity
6. JSON storage for simplicity and portability
7. ScheduledExecutorService for controlled concurrency

============================================================
7. THREADING MODEL
   ============================================================

[Monitor Thread]
|
+--> Process scanning
+--> Rule evaluation
+--> Listener notifications

[JavaFX UI Thread]
|
+--> All UI updates via Platform.runLater()

[Background Threads]
|
+--> Report generation
+--> Process killing

============================================================
8. DATA FLOW SUMMARY
   ============================================================
```
Processes (OS)
↓
ProcessScanner
↓
ProcessMonitor
↓
+--------------------------+
| AlertEngine              |
| CustomRuleEngine         |
+--------------------------+
↓
RuleActionExecutor
↓
+--------------------------+
| AlertEvent               |
| ProcessKiller            |
+--------------------------+
↓
HistoryStorage + UI
```