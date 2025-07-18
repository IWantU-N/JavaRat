# Sorillus RAT

Sorillus RAT is a remote administration tool designed for managing client machines remotely. This project includes a server application to control clients and a client application that executes commands, captures screenshots, streams webcam, and manages files.

## Features
- Remote command execution (CMD and PowerShell)
- Screenshot capture and streaming
- Webcam streaming
- File management (upload, download, list)
- Process management (list, kill)
- Mouse control
- Persistent execution with random file naming
- Autostart via Windows Registry and Task Scheduler

## Prerequisites
- **Java Development Kit (JDK)**: Version 11 or higher
- **Maven**: For building the project
- **Webcam Capture Library**: `webcam-capture` (included via Maven dependency)
- **Windows OS**: For full functionality (registry and task scheduler features)
