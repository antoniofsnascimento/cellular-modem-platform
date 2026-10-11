# Cellular AT Modem & Network Emulation Platform

Repository for Practical Work 1 (TP1) of the Mobile Communications Networks and Services course (Redes e Serviços de Comunicações Móveis - RSCM), Master in Informatics Engineering (MEI) / Degree in Informatics Engineering (LEI), University of Trás-os-Montes e Alto Douro (UTAD).

---

## 1. Overview and Architecture

The objective of this platform is to emulate the complete lifecycle of a physical cellular modem (GSM/3G/4G) interacting with client applications over the Hayes AT command set (3GPP TS 27.005 and TS 27.007 standards), without requiring physical radio hardware.

### System Topology

```
┌─────────────────────────┐          Virtual Serial Port          ┌─────────────────────────┐
│   Client Application    │ ◄───►  /tmp/ttyB (PTY) or Netcat ───► │       AT Emulator       │
│ (jSerialComm / Console) │                 │                     │      (CelerSMS Fork)    │
└─────────────────────────┘                 │                     └────────────┬────────────┘
                                            ▼                                  │
                                     socat PTY-TCP Bridge                      ▼
                                 (socat ... tcp:127.0.0.1:2001)       Server Telemetry Console
                                                                      ([RX], [FSM], [LATENCY], [TX])
```

---

## 2. Weekly Implementation Milestones

### Week 1 — Environment Setup & Virtual Serial Ports
* Virtual serial communication channel setup using `socat` pseudo-terminals (`pty`).
* Verified cross-terminal loopback bridging without physical hardware.
* Bridge command:
  ```bash
  socat -d -d pty,raw,echo=0,link=/tmp/ttyA pty,raw,echo=0,link=/tmp/ttyB
  ```

### Week 2 — AT Command Parser & Network States
* Cloned and integrated the open-source **CelerSMS** baseline into the Maven project structure (`com.celer.emul.AT`).
* Standard Hayes commands execution (`AT`, `ATE0`/`ATE1`, `ATI`).
* Radio & SIM status inquiry:
  * `AT+CPIN?`: SIM card status.
  * `AT+CSQ`: Signal quality report.
  * `AT+CREG?`: Network registration status.
  * `AT+COPS?`: Operator identification (`UTAD Telecom`).

### Week 3 — Finite State Machine (FSM) & Realistic Radio Latencies
* **Deterministic FSM Engine**: Models physical hardware power-up and cellular network negotiation:
  * `BOOT`: Modem powering up (duration: 2000 ms). Commands return `ERROR`.
  * `PIN_REQUIRED`: Hardware operational, SIM locked. Awaiting `AT+CPIN=0000`.
  * `REGISTRATION`: Radio scanning frequencies (duration: 5000 ms). `AT+CREG?` reports `0,2` (Searching).
  * `IDLE`: Connected to Home Network (`0,1`). Full SMS/USSD stack enabled.
* **Radio Latency Simulation**:
  * Basic AT commands: 20–50 ms.
  * Signal strength (`+CSQ`): 10 ms.
  * SMS transmission (`+CMGS`): 200–800 ms.
  * USSD session (`+CUSD`): 500–2000 ms.

```
+------------+       elapsed > 2s       +------------------+
|    BOOT    | ───────────────────────► |   PIN_REQUIRED   |
+------------+                          +------------------+
                                                  │
                                                  │ AT+CPIN=0000
                                                  ▼
+------------+       elapsed > 5s       +------------------+
|    IDLE    | ◄─────────────────────── |   REGISTRATION   |
+------------+                          +------------------+
```

### Week 4 — Text-Mode SMS Configuration & Transmission
* Service Center Address (SMSC) management:
  * `AT+CSCA?`: Returns current SMSC address (e.g., `+CSCA: "+351910000000",145`).
  * `AT+CSCA="<number>"`: Updates SMSC address dynamically.
* Text Mode SMS format (`AT+CMGF=1`).
* Full SMS send flow with interactive prompt (`>`) and `<Ctrl+Z>` termination (`AT+CMGS="<number>"`).
* Automatic persistence of sent messages into the in-memory store marked as `STO SENT`.

### Week 5 — SMS Storage Management, Asynchronous URC & USSD
* **SMS Storage Engine (`SmsRecord`)**:
  * In-memory storage with automatic index sequencing ($1..N$).
  * `AT+CMGL="ALL"`: Lists all stored messages.
  * `AT+CMGR=<index>`: Reads specific message and triggers state transition from `REC UNREAD` to `REC READ`.
  * `AT+CMGD=<index>`: Deletes message from storage, freeing capacity according to 3GPP standards.
* **Asynchronous Notifications (Unsolicited Result Codes - URC)**:
  * Spontaneous delivery notification upon new incoming message: `+CMTI: "SM", <index>`.
  * Dedicated lab testing injector: `AT+INJECT="<sender>","<text>"`.
* **Supplementary Services / USSD**:
  * `AT+CUSD=1,"*#100#",15`: Simulates network balance inquiry with delayed response:
    ```text
    +CUSD: 0,"Saldo: 5.40 EUR (Validade: 31/12/2026)",15
    OK
    ```

### Server-Side Telemetry & Observability
* Diagnostic logger running on the server console (`System.out`) providing structured logs with millisecond timestamps:
  * `[RX]`: Incoming raw command and current FSM state.
  * `[LATENCY]`: Simulated hardware delay applied.
  * `[TX]`: Response sent back to client.
  * `[URC]`: Unsolicited event generated by network.
  * `[STORE]`: Storage lifecycle events.

---

## 3. Technology Stack

* **Language**: Java 21 (LTS).
* **Build System**: Apache Maven 3.9+.
* **Serial Communications**: `jSerialComm` 2.11.0.
* **Transport Bridge**: `socat` / Netcat (`nc`).

---

## 4. Build and Execution Guide

### Prerequisites
* JDK 21 installed (`java -version`).
* Maven installed (`mvn -version`).

### 1. Compile the Project
```bash
mvn clean compile
```

### 2. Run the AT Emulator Server
The emulator listens on TCP port `2001` by default:
```bash
java -cp target/classes:$(mvn dependency:build-classpath | grep -A1 'Dependencies classpath:' | tail -1) com.celer.emul.AT 2001
```

### 3. Connect a Client

#### Option A: Direct Socket Connection (Interactive Testing)
```bash
nc 127.0.0.1 2001
```

#### Option B: Virtual Serial Port Bridge (`socat`)
In one terminal, spawn the virtual serial bridge:
```bash
socat pty,raw,echo=0,link=/tmp/ttyB tcp:127.0.0.1:2001
```
The client application or serial terminal connects directly to `/tmp/ttyB`.

---

## 5. Verification & Test Plan Session

```text
# 1. Modem Boot & Security Verification
AT+CPIN?
ERROR                       <-- Modem in BOOT state (< 2s)

AT+CPIN?
+CPIN: SIM PIN              <-- Modem ready, SIM locked
OK

AT+CPIN=0000
OK                          <-- SIM unlocked, radio turned on

# 2. Network Registration
AT+CREG?
+CREG: 0,2                  <-- Searching for network (REGISTRATION state)
OK

(wait 5 seconds)

AT+CREG?
+CREG: 0,1                  <-- Registered to Home Network (IDLE state)
OK

# 3. SMS Configuration & Message Center
AT+CSCA?
+CSCA: "+351910000000",145
OK

# 4. Asynchronous SMS Injection & Notification
AT+INJECT="912345678","Test message from peer"
+CMTI: "SM",1               <-- Spontaneous URC event

# 5. Read Message
AT+CMGR=1
+CMGR: "REC READ","912345678",,"26/10/11,19:00:00+00"
Test message from peer
OK

# 6. Delete Message
AT+CMGD=1
OK

# 7. USSD Balance Inquiry
AT+CUSD=1,"*#100#",15
+CUSD: 0,"Saldo: 5.40 EUR (Validade: 31/12/2026)",15
OK
```
