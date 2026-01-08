# Android Wi-Fi Grid Test System 🛰️

A professional-grade Android tool designed for investigating, testing, and benchmarking Wi-Fi networks in a "Grid" environment. This app allows users to arm specific nodes (SSIDs), perform automated diagnostic mission cycles, and analyze network stability with high-fidelity visualization and branding.

![Branding](https://github.com/madgod87/anDroidWifiTestScript/raw/main/app/src/main/res/drawable/wifi_grid_logo_design.png)

## 🚀 Key Features

### 📡 Master Dashboard & Vault
*   **Grid Vault**: Encrypted storage for Wi-Fi credentials. Sync known networks from the system to enable 1-tap automated testing.
*   **Mission Control**: "Arm" multiple nodes from nearby scans. The system automatically cycles through connections to perform batch diagnostics.

### 🛰️ Advanced Operations (New)
*   **Quantum Traceroute**: Vertical timeline mapping of every server hop between the device and the internet. Identify exactly where the lag starts.
*   **Signal Hunter**: High-frequency RSSI power gauge. Update rate of 500ms allows physical location of dead zones and interference sources.
*   **Security Audit**: Automated probing of gateway ports and verification of WPA2/WPA3 encryption standards.
*   **Smart Guard**: Geofenced background monitoring. Automatically triggers deep diagnostics if packet loss or latency exceeds custom thresholds.

### 📄 Intelligence & Reporting
*   **Branded PDF Summaries**: Professional "White-Mode" reports featuring:
    *   **Latency Matrix**: A 20-sample individual ping grid for every tested node.
    *   **Quantum Sparklines**: Mini-trend graphs visualizing ping stability.
    *   **Reliability Scores**: Color-coded performance percentage per mission.
*   **Searchable Logic Guide**: Built-in technical encyclopedia for networking terms (RSSI, Jitter, Packet Loss, etc.) with a topic search option.

## 🛠️ Technology Stack

*   **Language**: Kotlin
*   **UI Framework**: Jetpack Compose (Modern Material 3 with Cyberpunk aesthetic)
*   **Logic Engine**: Multi-threaded Socket Probing & Shell Execution (Traceroute)
*   **Database**: Room Persistence for mission logs and vault credentials.
*   **Graphics**: Custom Android Canvas for PDF and Gauge rendering.

## 📱 Permissions Required

To function as a grid testing tool, the app requires:
*   `ACCESS_FINE_LOCATION` & `ACCESS_COARSE_LOCATION`: Required by Android for Wi-Fi scanning.
*   `NEARBY_WIFI_DEVICES`: Required for API 33+ to scan without location.
*   `POST_NOTIFICATIONS`: For background Guard Mode status updates.

## 🧪 Operational Workflow

1.  **Sync the Vault**: Use the **GRID VAULT** to import system SSIDs and save target passphrases.
2.  **Arm Nodes**: On the **DASHBOARD**, arm the networks you wish to investigate.
3.  **Execute Mission**: Use the central Core button to start a batch diagnostic.
4.  **Advanced Analysis**: Use the **TRACEROUTE** or **SIGNAL HUNTER** for real-time site surveys.
5.  **Export Intelligence**: Generate **PDF Reports** from the mission logs for professional documentation.

---

*Developed for Advanced Network Investigation, Site Surveys, and Cyber-Security Audits.*
