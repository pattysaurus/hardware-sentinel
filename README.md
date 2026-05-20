# CRIS SysMonitor

A real-time hardware monitoring web application built with Java Spring Boot, developed during an internship at **CRIS (Centre for Railway Information Systems), Ministry of Railways** — Parcel Management System Group.

The dashboard is publicly hosted and accessible from any device on any network.

---

## Live Dashboard

👉 [cris-sysmonitor.netlify.app](https://cris-sysmonitor.netlify.app)

---

## Table of Contents

- [What It Does](#what-it-does)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Installation & Setup](#installation--setup)
- [Building the JAR](#building-the-jar)
- [Running the Application](#running-the-application)
- [Cross-Network Access](#cross-network-access)
- [API Endpoints](#api-endpoints)
- [Technologies](#technologies)
- [How It Works](#how-it-works)
- [Troubleshooting](#troubleshooting)
- [Future Improvements](#future-improvements)

---

## What It Does

SysMonitor detects and displays all hardware peripherals and physical ports connected to a Windows PC in real-time. The dashboard auto-refreshes every 10 seconds over a secure HTTPS connection and is accessible from any browser on any network.

**Detected hardware:**
- USB Devices (keyboards, mice, webcams, Bluetooth adapters, hubs)
- Disk Drives (internal and external storage)
- COM / Serial Ports
- Audio Devices
- Display Monitors

---

## Architecture

```
┌─────────────────────────────────────────────┐
│   Netlify Dashboard                          │
│   cris-sysmonitor.netlify.app               │
│   Accessible from any device, any network   │
│   JavaScript fetch() over HTTPS             │
└────────────────┬────────────────────────────┘
                 │ Enter server IP or Cloudflare URL
                 │
        ┌────────▼────────┐
        │ Same Network?   │
        └──┬──────────┬───┘
     YES   │          │  NO
           │          │
    Local IP          Cloudflare Tunnel
    :8443             (public URL)
           │          │
           └────┬─────┘
                │ HTTPS GET requests
┌───────────────▼─────────────────────────────┐
│   Controller Layer                           │
│   SystemController.java                      │
│   @GetMapping endpoints → JSON               │
└───────────────┬─────────────────────────────┘
                │ @Autowired
┌───────────────▼─────────────────────────────┐
│   Service Layer                              │
│   SystemService.java                         │
│   Business logic + OS integration            │
└───────────────┬─────────────────────────────┘
                │
       ┌────────┴────────┐
       ▼                 ▼
  OSHI Library     PowerShell / WMI
  (USB, Disks)     (COM, Audio, Display)
```

### Key Design Patterns

- **3-Layer Architecture**: Controller → Service → OS. Each layer has one responsibility.
- **Dependency Injection**: `@Autowired` — Spring injects `SystemService` into `SystemController` automatically.
- **Singleton Beans**: `@Service` makes `SystemService` a single instance reused across all requests.
- **Recursive Tree Traversal**: USB devices are a tree in Windows. `collectDevices()` walks it depth-first.
- **DTO Pattern**: `DeviceInfo` is a plain Java object that Jackson auto-serializes to JSON.
- **CORS Configuration**: `CorsConfig.java` allows the Netlify dashboard to call the HTTPS API cross-origin.

---

## Project Structure

```
hardware-sentinel/
├── README.md
├── pom.xml                                ← Maven config & dependencies
│
└── src/main/
    ├── java/com/cris/sysmonitor/
    │   ├── SysmonitorApplication.java     ← Entry point + Swing GUI launcher
    │   ├── SystemController.java          ← REST API endpoints
    │   ├── SystemService.java             ← Hardware detection logic
    │   ├── DeviceInfo.java                ← Data model (one device)
    │   └── CorsConfig.java                ← Allows cross-origin API calls
    │
    └── resources/
        ├── application.properties         ← SSL + port config
        ├── keystore.p12                   ← SSL certificate (self-signed, embedded in JAR, excluded from Git)
        └── templates/
            └── index.html                 ← Server-hosted dashboard (Thymeleaf fallback)
```

### Key Files Explained

| File | Purpose |
|---|---|
| `SysmonitorApplication.java` | Starts Spring Boot in background thread; launches Swing GUI window |
| `SystemController.java` | Maps HTTP GET requests to service methods; returns JSON |
| `SystemService.java` | Fetches hardware data via OSHI and PowerShell WMI |
| `DeviceInfo.java` | Data blueprint — name, type, status, details, category |
| `CorsConfig.java` | Enables Netlify dashboard to call the API cross-origin |
| `keystore.p12` | Self-signed SSL certificate bundled inside the JAR (excluded from Git) |

---

## Prerequisites

| Requirement | Version | Notes |
|---|---|---|
| Java JDK | 8 or above | Must be JDK, not JRE |
| OS | Windows 10 / 11 | WMI queries are Windows-only |
| Maven | 3.6+ | Only needed for building from source |

**To verify Java:**
```cmd
java -version
```

---

## Installation & Setup

### 1. Clone the repo

```cmd
git clone https://github.com/pattysaurus/hardware-sentinel.git
cd hardware-sentinel
```

### 2. Generate SSL Certificate (one time only)

`keystore.p12` is excluded from Git for security. Generate your own:

```cmd
keytool -genkeypair -alias sysmonitor -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore src/main/resources/keystore.p12 -validity 365 -storepass sysmonitor123
```

When prompted:
- First and last name: `localhost`
- Everything else: press Enter
- Final confirmation: type `yes`

### 3. Configure the keystore password

The password is read from an environment variable. Set it before running:

```cmd
set KEYSTORE_PASSWORD=your_password_here
```

Or leave it unset — it defaults to `sysmonitor123` for local development.

---

## Building the JAR

```cmd
mvn clean package -DskipTests
```

Output: `target/sysmonitor-1.0.0.jar`

The JAR is fully self-contained — includes your code, all libraries, Tomcat, and the keystore. No external files needed.

---

## Running the Application

Double-click `sysmonitor-1.0.0.jar` directly. A Swing GUI window opens:

```
┌─────────────────────────────────┐
│        CRIS SysMonitor          │
│  Centre for Railway Info Systems│
│                                 │
│   ✅ Server is running          │
│   https://192.168.x.x:8443      │
│                                 │
│   [ Open Dashboard ]            │
│   [ Stop Server    ]            │
└─────────────────────────────────┘
```

### Accessing the Dashboard

Open [cris-sysmonitor.netlify.app](https://cris-sysmonitor.netlify.app) from any device and enter your server's address in the connect screen.

**Same network (local IP):**
- First visit `https://192.168.x.x:8443` in the browser and accept the SSL warning (one time only)
- Then enter `192.168.x.x` in the connect screen

**Different network (Cloudflare Tunnel):**
- Run the tunnel command (see below)
- Enter the Cloudflare URL in the connect screen — no SSL warning

---

## Cross-Network Access

To make the server accessible from outside your local network, use Cloudflare Tunnel.

### Install cloudflared

Download from:
```
https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.msi
```

### Run the tunnel

Make sure the JAR is running first, then:

```cmd
cloudflared tunnel --url https://localhost:8443 --origin-server-name sysmonitor --no-tls-verify
```

Copy the generated URL (e.g. `https://xyz.trycloudflare.com`) and enter it in the Netlify dashboard connect screen.

> **Note:** The free quick tunnel URL changes every restart. For a fixed permanent URL, a Cloudflare account and a domain name are required.

---

## API Endpoints

All endpoints return JSON arrays of `DeviceInfo` objects.

| Method | Endpoint | Returns |
|---|---|---|
| GET | `/` | HTML dashboard (Thymeleaf) |
| GET | `/api/usb` | All USB devices |
| GET | `/api/disks` | All disk drives |
| GET | `/api/ports` | COM / Serial ports |
| GET | `/api/audio` | Audio devices |
| GET | `/api/display` | Display monitors |

**Sample response from `/api/usb`:**

```json
[
  {
    "name": "HID Keyboard Device",
    "type": "USB",
    "status": "Connected",
    "details": "Vendor: (Standard keyboards) | Serial: ",
    "category": "Keyboard"
  },
  {
    "name": "HID-compliant mouse",
    "type": "USB",
    "status": "Connected",
    "details": "Vendor: Microsoft | Serial: TLSR8278",
    "category": "Mouse"
  }
]
```

---

## Technologies

| Technology | Version | Purpose |
|---|---|---|
| Java | 1.8 target / 17 dev | Language and runtime |
| Spring Boot | 2.7.18 | Web framework (last version with Java 8 support) |
| Apache Tomcat | 9.0.83 (embedded) | Web server — bundled inside JAR |
| OSHI | 5.8.7 | USB and disk detection via Windows OS APIs |
| JNA | Auto via OSHI | Java Native Access — bridges Java to Windows DLLs |
| PowerShell / WMI | System built-in | COM ports, audio, display detection |
| Jackson | Auto via Spring | Converts Java objects to JSON automatically |
| Thymeleaf | 2.7.x | Server-side HTML template (fallback dashboard) |
| Java Swing | Built-in | Desktop GUI launcher window |
| Maven | 3.9.15 | Build tool and dependency management |
| SSL / TLS | Self-signed | HTTPS encryption on port 8443 |
| Cloudflare Tunnel | Latest | Cross-network access without router configuration |
| Netlify | — | Public dashboard hosting |

### Why These Choices?

- **Spring Boot 2.7.18**: Java 1.8 compatibility requirement. Spring Boot 3.x requires Java 17 minimum.
- **OSHI 5.8.7**: Last version with Java 8 support. 6.x dropped it.
- **PowerShell for COM/Audio/Display**: OSHI 5.x does not cover these on Windows. Zero extra dependencies.
- **Self-signed SSL**: Internal network tool. CA-signed certificates require a domain name.
- **Java Swing GUI**: Built into Java — no extra dependencies. Clean launcher for non-technical users.
- **Netlify**: Free static hosting. Dashboard is pure HTML/CSS/JS — no server needed.
- **Cloudflare Tunnel**: Free cross-network access without port forwarding or router access.

---

## How It Works

### USB Tree Traversal

Windows organizes USB devices as a tree:

```
Host Controller
└── Root Hub (USB 3.0)
    └── Generic Hub
        ├── HID Keyboard Device    ← 3 levels deep
        ├── HID-compliant mouse
        └── TP-Link Bluetooth Adapter
```

`collectDevices()` uses recursion to walk the full tree:

```java
private void collectDevices(UsbDevice device, List<DeviceInfo> devices) {
    if (!shouldFilter(device.getName())) {
        devices.add(new DeviceInfo(...));
    }
    for (UsbDevice child : device.getConnectedDevices()) {
        collectDevices(child, devices);
    }
}
```

### Connect Screen

The Netlify dashboard opens with a connect screen. Enter either:
- A local IP (e.g. `192.168.0.4`) — connects via port 8443
- A Cloudflare tunnel URL (e.g. `xyz.trycloudflare.com`) — connects cross-network

The address is saved to localStorage — pre-filled on next visit.

### Auto-Refresh

```javascript
setInterval(() => {
    countdown--;
    if (countdown <= 0) {
        countdown = 10;
        fetchAll();
    }
}, 1000);
```

---

## Troubleshooting

### "Address already in use" on startup

```cmd
netstat -ano | findstr :8443
taskkill /PID <PID> /F
```

### Dashboard shows "Could not reach the server"

1. Confirm the JAR is running and Swing window shows "Server is running"
2. For local IP: visit `https://[IP]:8443` directly in browser and accept the SSL warning first
3. For Cloudflare URL: confirm the tunnel is still running in Command Prompt

### PowerShell queries return empty (COM/Audio/Display)

Right-click the JAR → **Run as administrator**.

### JAR won't run on another PC

```cmd
java -version
```

Must be Java 8 or above. Download from `https://www.oracle.com/java/downloads`

---

## Future Improvements

- Device history logging with H2/SQLite database
- Network interface monitoring endpoint
- Unknown device flagging for security alerts
- Package as `.exe` using Launch4j
- Fixed Cloudflare tunnel with permanent URL using a domain name
- Proper SSL certificate via Let's Encrypt
- Multi-machine monitoring — single dashboard, multiple agents
- WebSocket push-based updates instead of polling

---

## Author

**Pratham Gaur**
B.Tech CSE (Data Science), ASET — Amity University Noida
CRIS Internship — Parcel Management System Group
May–July 2026

---

## Acknowledgements

- [OSHI Library](https://github.com/oshi/oshi) — OS & Hardware Information for Java
- [Spring Boot](https://spring.io/projects/spring-boot) — Application framework
- [Cloudflare Tunnel](https://developers.cloudflare.com/cloudflare-one/connections/connect-apps) — Cross-network tunneling
- Centre for Railway Information Systems (CRIS), Ministry of Railways, Government of India