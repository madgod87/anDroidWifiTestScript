# Android Wi-Fi Grid Test System 🛰️

A professional-grade Android tool designed for investigating, testing, and benchmarking Wi-Fi networks in a "Grid" environment. This app allows users to arm specific nodes (SSIDs), perform automated diagnostic mission cycles, and analyze network stability with dual-throughput (Download/Upload) metrics.

## 🚀 Key Features

*   **Grid Vault**: A master database for storing Wi-Fi credentials. Because Android restricts access to system-saved passwords, the Vault allows the app to remember passwords for automated testing.
*   **Mission Diagnostics**: 
    *   **DL/UL Benchmarking**: Measures both download and upload throughput using HTTP diagnostics.
    *   **Stability Metrics**: Performs high-frequency ping tests (20 samples per node) to calculate Average Latency, Jitter, and Packet Loss.
*   **Automated Testing**: "Arm" multiple networks from the dashboard and run a "Batch Test" to cycle through them automatically.
*   **Material 3 Cyberpunk UI**: A futuristic, high-contrast interface designed for "Mission Operations."
*   **Mission Reports**: Persistent storage of all test results, sorted by time (newest first), with detailed analysis popups.
*   **CSV Export**: Export all mission logs to a CSV file for external analysis or reporting.

## 🛠️ Technology Stack

*   **Language**: Kotlin
*   **UI Framework**: Jetpack Compose (Material 3)
*   **Database**: Room Persistence Library
*   **Networking**: OkHttp & Cronet (via Android ConnectivityManager)
*   **Services**: Foreground Service for uninterrupted testing cycles.

## 📱 Permissions Required

To function as a grid testing tool, the app requires:
*   `ACCESS_FINE_LOCATION` & `ACCESS_COARSE_LOCATION`: Required by Android for Wi-Fi scanning.
*   `NEARBY_WIFI_DEVICES`: Required for API 33+ to scan without location.
*   `CHANGE_WIFI_STATE`: Needed to bridge the network interface to different SSIDs.

## 🧪 How to Use

1.  **Sync the Vault**: Go to the **GRID VAULT** screen and tap **SYNC**. This discovers all nearby nodes and imports known SSIDs.
2.  **Add Credentials**: Tap on a node in the Vault to save its passphrase.
3.  **Arm the Grid**: On the **DASHBOARD**, tap the nodes you want to test. Secure nodes with saved passwords in the Vault will arm instantly.
4.  **Execute Mission**: Tap **START BATCH TEST**. The app will automatically connect to each node, run the full diagnostic suite, and move to the next.
5.  **Analyze**: Review the results in the **MISSION REPORTS** tab.

---

*Developed for Advanced Network Investigation and Site Surveys.*
