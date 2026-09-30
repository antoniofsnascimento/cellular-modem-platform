# Cellular AT Modem & Network Emulation Platform

Repository for Practical Work 1 (TP1) of the Mobile Communications Networks and Services course (UTAD).

## Week 1: Environment Setup and Virtual Serial Ports

In accordance with the Week 1 requirements, the virtual serial ports were successfully created and tested in a macOS environment.

### 1. Virtual Ports Creation Command
The `socat` utility was used to generate a pair of linked pseudo-terminals (pty) to emulate hardware serial communication without physical devices:
```bash
socat -d -d pty,raw,echo=0,link=/tmp/ttyA pty,raw,echo=0,link=/tmp/ttyB
