# Product Requirements Document (PRD)

---

# 1. Product Overview

ProcessGuard is a desktop system monitoring application that tracks running processes and identifies suspicious behaviour in real time. The system helps users detect abnormal CPU usage memory spikes and unknown applications running on their device.

ProcessGuard continuously scans system processes classifies them using predefined rules and alerts users when suspicious activity is detected. The application also allows users to define their own custom rules and manage whitelist and blacklist configurations.

ProcessGuard is designed to be lightweight responsive and easy to use while still providing meaningful monitoring and alerting capabilities.

---

# 2. Problem Statement

Modern computers often run many background processes that users are unaware of. Some of these processes may consume excessive resources behave suspiciously or pose security risks.

Existing tools such as Task Manager provide raw data but do not offer intelligent classification alerting or custom rule based monitoring. Users must manually interpret information which can be difficult and time consuming.

Students developers and general users need a simple monitoring system that:

- Automatically detects abnormal processes
- Provides alerts when suspicious activity occurs
- Allows configuration of rules
- Stores monitoring history
- Runs quietly in the background

ProcessGuard aims to solve these problems by providing intelligent process monitoring and alerting.

---

# 3. Target Users and Stakeholders

## Primary Users

### Developers
Developers who want to monitor applications during development and testing.

### System Administrators
Administrators who want lightweight monitoring tools.

---

## Secondary Users

### Students
Students learning operating systems or security who want to understand system behaviour.

### Security Enthusiasts
Users interested in detecting unusual system behaviour.

### General Computer Users
Users who want basic protection against suspicious programs and performance issues.

---

## Stakeholders

- Project developers
- Academic supervisors
- End users
- System administrators

---

# 4. User Stories

### Basic Monitoring

- As a user I want to see all running processes so that I can monitor my system
- As a user I want suspicious processes highlighted so that I can identify problems quickly
- As a user I want CPU and memory usage displayed so that I understand system performance

---

### Alerts

- As a user I want alerts when CPU usage is high so that I can stop problematic applications
- As a user I want alerts for unknown processes so that I can investigate them
- As a user I want alerts for blacklisted applications so that I can prevent unsafe programs

---

### Custom Rules

- As a user I want to define custom rules so that I can monitor specific behaviour
- As a user I want to enable or disable rules so that I control alert behaviour
- As a user I want rule severity levels so that I prioritise alerts

---

### Configuration

- As a user I want to configure scan interval so that I control performance
- As a user I want whitelist support so that trusted applications are ignored
- As a user I want blacklist support so that blocked applications trigger alerts

---

### History

- As a user I want to view past alerts so that I track system behaviour
- As a user I want process history so that I understand patterns over time

---

### System Integration

- As a user I want the application to run in system tray so that it runs quietly
- As a user I want CLI support so that I can automate monitoring
- As a user I want optional web interface so that I monitor remotely

---

# 5. Functional Requirements

## 5.1 Process Monitoring

The system shall:

- Scan system processes periodically
- Detect newly started processes
- Detect exited processes
- Track CPU usage and memory usage
- Classify processes into statuses

Statuses include:

- Normal
- Suspicious
- Blocked
- Whitelisted

---

## 5.2 Alert Engine

The system shall:

- Generate alerts for high CPU usage
- Generate alerts for high memory usage
- Generate alerts for blacklisted processes
- Generate alerts for suspicious processes
- Store alert history

Alert types include:

- High CPU usage
- High memory usage
- Blacklisted process
- Suspicious process
- Custom rule violation

---

## 5.3 Custom Rules

The system shall:

- Allow users to create rules
- Support rule conditions
- Support AND and OR logic
- Support severity levels
- Support cooldown periods
- Support rule actions

Rules may evaluate:

- Process name
- CPU usage
- Memory usage
- Parent process
- Custom conditions

---

## 5.4 Configuration Management

The system shall:

- Store configuration in JSON
- Allow CPU threshold configuration
- Allow memory threshold configuration
- Allow scan interval configuration
- Allow whitelist and blacklist configuration
- Persist configuration between sessions

---

## 5.5 History Storage

The system shall:

- Store process snapshots
- Store alert history
- Persist data in JSON files
- Maintain recent history
- Allow retrieval of recent alerts

---

## 5.6 User Interface

The system shall include:

- Main dashboard
- Alert display
- Process list
- Rule configuration UI
- Settings panel

---

## 5.7 CLI Interface

The system shall:

- Support command line arguments
- Allow headless execution
- Allow automation

---

## 5.8 System Tray

The system shall:

- Run in background
- Show notifications
- Allow quick access

---

## 5.9 Web Interface

The system shall:

- Provide optional web server
- Display monitoring dashboard
- Allow remote viewing

---

# 6. Non Functional Requirements

## 6.1 Performance

- The system shall scan processes every few seconds
- The system shall run with low CPU usage
- The system shall handle large numbers of processes

---

## 6.2 Reliability

- The system shall continue running in background
- The system shall handle exceptions gracefully
- The system shall recover from failures

---

## 6.3 Usability

- The interface shall be simple and intuitive
- Alerts shall be clear and understandable
- Configuration shall be easy to modify

---

## 6.4 Security

- The system shall not expose sensitive data
- Configuration files shall be stored locally
- Web interface shall run on configurable port

---

## 6.5 Maintainability

- The system shall follow modular architecture
- The system shall support extension
- The system shall support future features

---

## 6.6 Portability

- The system shall support Windows
- The system shall support Mac
- The system shall support Linux

---

# 7. Assumptions

- Users have permission to read system processes
- Java runtime is available
- Users have basic understanding of system monitoring

---

# 8. Constraints

- Must run on Java 21
- Must use JSON storage
- Must be lightweight

---

# 9. Future Enhancements

- Machine learning anomaly detection
- Remote monitoring dashboard
- Automatic process blocking
- Notification integrations
- Performance analytics
- Virus detection

---

# 10. Success Metrics

The product will be considered successful if:

- Users can detect suspicious processes
- Alerts are generated accurately
- System performance remains lightweight
- Users can configure rules successfully
