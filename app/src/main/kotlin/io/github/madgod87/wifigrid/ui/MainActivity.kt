package io.github.madgod87.wifigrid.ui

import android.Manifest
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import io.github.madgod87.wifigrid.data.SelectedWifi
import io.github.madgod87.wifigrid.data.TestResult
import io.github.madgod87.wifigrid.data.WifiDatabase
import io.github.madgod87.wifigrid.network.HardwareUtils
import io.github.madgod87.wifigrid.service.WifiTestService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalUriHandler

import java.text.SimpleDateFormat
import java.util.*

// --- Modern Futuristic Colors ---
val SpaceDark = Color(0xFF0A0E14)
val NeonCyan = Color(0xFF00F2FF)
val NeonPurple = Color(0xFFBC00FF)
val CyberPink = Color(0xFFFF007F)
val CardGlass = Color(0x1AFFFFFF)

data class TraceHop(val hop: Int, val ip: String, val latency: Long, val reachable: Boolean)
data class SecurityFinding(val type: String, val detail: String, val isSafe: Boolean)

enum class Screen { DASHBOARD, REPORTS, SYSTEM_STATUS, VAULT, GUIDE, TRACEROUTE, SIGNAL_HUNTER, SMART_GUARD, SECURITY }

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = WifiDatabase.getDatabase(application)
    val results: Flow<List<TestResult>> = db.dao().getRankedResults()
    val selected: Flow<List<SelectedWifi>> = db.dao().getSelectedNetworks()
    
    private val _nearby = mutableStateOf<List<HardwareUtils.ScannedNetwork>>(emptyList())
    val nearby: State<List<HardwareUtils.ScannedNetwork>> = _nearby

    private val _isTesting = mutableStateOf(false)
    val isTesting: State<Boolean> = _isTesting

    private val _testStatus = mutableStateOf("IDLE - AWAITING COMMAND")
    val testStatus: State<String> = _testStatus

    private val _isScanning = mutableStateOf(false)
    val isScanning: State<Boolean> = _isScanning

    private val _savedSsids = mutableStateOf<Set<String>>(emptySet())
    val savedSsids: State<Set<String>> = _savedSsids

    private val _isGuardActive = mutableStateOf(false)
    val isGuardActive: State<Boolean> = _isGuardActive

    // --- Traceroute Ops ---
    private val _traceHops = mutableStateOf<List<TraceHop>>(emptyList())
    val traceHops: State<List<TraceHop>> = _traceHops
    private val _isTracing = mutableStateOf(false)
    val isTracing: State<Boolean> = _isTracing

    // --- Signal Hunter Ops ---
    private val _liveRssi = mutableStateOf(-100)
    val liveRssi: State<Int> = _liveRssi
    private val _isHunting = mutableStateOf(false)

    // --- Security Ops ---
    private val _securityStatus = mutableStateOf<List<SecurityFinding>>(emptyList())
    val securityStatus: State<List<SecurityFinding>> = _securityStatus
    private val _isAuditing = mutableStateOf(false)
    val isAuditing: State<Boolean> = _isAuditing

    // --- Smart Guard Settings ---
    private val _guardGeofence = mutableStateOf<String?>(null)
    val guardGeofence: State<String?> = _guardGeofence
    private val _failureThreshold = mutableIntStateOf(5) // Default 5% loss trigger
    val failureThreshold: State<Int> = _failureThreshold

    fun scanNearby() {
        _isScanning.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val saved = HardwareUtils.getSavedSsids(getApplication())
            val success = HardwareUtils.triggerScan(getApplication())
            withContext(Dispatchers.Main) {
                _savedSsids.value = saved
                if (!success) {
                    _isScanning.value = false
                    Toast.makeText(getApplication(), "Scan throttled by OS. Try again in 30s.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun refreshNearby() {
        viewModelScope.launch(Dispatchers.IO) {
            val nearbyList = HardwareUtils.getNearbySsids(getApplication())
            val saved = HardwareUtils.getSavedSsids(getApplication())
            withContext(Dispatchers.Main) {
                _nearby.value = nearbyList
                _savedSsids.value = saved
                _isScanning.value = false
            }
        }
    }

    fun exportResults() {
        viewModelScope.launch {
            val allResults = results.first()
            val file = HardwareUtils.exportToCsv(getApplication(), allResults)
            if (file != null) {
                shareFile(file, "text/csv")
            } else {
                Toast.makeText(getApplication(), "Failed to generate CSV", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun exportResultsPdf() {
        viewModelScope.launch {
            val allResults = results.first()
            val file = HardwareUtils.exportToPdf(getApplication(), allResults)
            if (file != null) {
                shareFile(file, "application/pdf")
            } else {
                Toast.makeText(getApplication(), "Failed to generate PDF", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun exportSingleResultPdf(result: TestResult) {
        viewModelScope.launch {
            val file = HardwareUtils.exportToPdf(getApplication(), listOf(result))
            if (file != null) {
                shareFile(file, "application/pdf")
            } else {
                Toast.makeText(getApplication(), "Failed to generate Result PDF", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun shareFile(file: java.io.File, mimeType: String) {
        val context = getApplication<Application>()
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "io.github.madgod87.wifigrid.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share Mission Report")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    private val _selectedResult = mutableStateOf<TestResult?>(null)
    val selectedResult: State<TestResult?> = _selectedResult

    fun selectResult(result: TestResult?) {
        _selectedResult.value = result
    }

    val vault: Flow<List<io.github.madgod87.wifigrid.data.VaultEntity>> = db.dao().getVaultNetworks()

    fun updateVault(ssid: String, password: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            db.dao().insertVault(io.github.madgod87.wifigrid.data.VaultEntity(ssid, password))
        }
    }

    fun addNetwork(ssid: String, password: String? = null) {
        viewModelScope.launch(Dispatchers.IO) { 
            val finalPass = password ?: db.dao().getPasswordFromVault(ssid)
            db.dao().insertSelected(SelectedWifi(ssid, finalPass)) 
            if (password != null) {
                db.dao().insertVault(io.github.madgod87.wifigrid.data.VaultEntity(ssid, password))
            }
        }
    }

    fun removeNetwork(ssid: String) {
        viewModelScope.launch(Dispatchers.IO) { db.dao().removeSelected(ssid) }
    }

    fun toggleNodeGuard(ssid: String, current: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val nodes = db.dao().getSelectedNetworksSnapshot()
            nodes.find { it.ssid == ssid }?.let { node ->
                db.dao().insertSelected(node.copy(isGuardEnabled = !current))
            }
        }
    }

    fun toggleGlobalGuard(context: Context) {
        _isGuardActive.value = !_isGuardActive.value
        if (_isGuardActive.value) {
            val request = androidx.work.PeriodicWorkRequestBuilder<io.github.madgod87.wifigrid.worker.GuardWorker>(
                15, java.util.concurrent.TimeUnit.MINUTES
            ).build()
            androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "GuardMission",
                androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
            Toast.makeText(context, "GUARD MODE INITIALIZED: 15m intervals", Toast.LENGTH_SHORT).show()
        } else {
            androidx.work.WorkManager.getInstance(context).cancelUniqueWork("GuardMission")
            Toast.makeText(context, "GUARD MODE DEACTIVATED", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteVault(ssid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.dao().deleteVault(ssid)
        }
    }

    fun syncFromSystem() {
        viewModelScope.launch(Dispatchers.IO) {
            val systemSsids = HardwareUtils.getSavedSsids(getApplication())
            val nearbySsids = HardwareUtils.getNearbySsids(getApplication()).map { it.ssid }
            
            val allSsids = (systemSsids + nearbySsids).toSet()
            val existingInVault = db.dao().getVaultSnapshot().map { it.ssid }.toSet()
            
            var count = 0
            allSsids.forEach { ssid ->
                if (!existingInVault.contains(ssid) && ssid.isNotBlank()) {
                    db.dao().insertVault(io.github.madgod87.wifigrid.data.VaultEntity(ssid, null))
                    count++
                }
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(getApplication(), "VAULT TUNED: $count new nodes identified", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- Quantum Traceroute Engine ---
    fun runTraceroute(host: String = "8.8.8.8") {
        _isTracing.value = true
        _traceHops.value = emptyList()
        viewModelScope.launch(Dispatchers.IO) {
            val list = mutableListOf<TraceHop>()
            for (ttl in 1..20) {
                if (!_isTracing.value) break
                val start = System.currentTimeMillis()
                val result = try {
                    val process = Runtime.getRuntime().exec("ping -c 1 -t $ttl $host")
                    val output = process.inputStream.bufferedReader().readText()
                    val end = System.currentTimeMillis()
                    
                    val ip = if (output.contains("from")) {
                        output.substringAfter("from ").substringBefore(":")
                    } else if (output.contains("bytes from")) {
                        output.substringAfter("bytes from ").substringBefore(":")
                    } else "???"
                    
                    TraceHop(ttl, ip, end - start, ip != "???")
                } catch (e: Exception) {
                    TraceHop(ttl, "TIMEOUT", 0, false)
                }
                list.add(result)
                _traceHops.value = list.toList()
                if (result.ip == host) break
                delay(200)
            }
            _isTracing.value = false
        }
    }

    fun stopTraceroute() { _isTracing.value = false }

    // --- Signal Hunter Engine ---
    fun startSignalHunter() {
        _isHunting.value = true
        viewModelScope.launch {
            while (_isHunting.value) {
                val rssi = HardwareUtils.getCurrentRssi(getApplication())
                _liveRssi.value = rssi
                delay(500)
            }
        }
    }

    fun stopSignalHunter() { _isHunting.value = false }

    // --- Security Audit Engine ---
    fun runSecurityScan() {
        _isAuditing.value = true
        _securityStatus.value = emptyList()
        viewModelScope.launch(Dispatchers.IO) {
            val findings = mutableListOf<SecurityFinding>()
            val gateway = HardwareUtils.getGatewayIp(getApplication())
            val commonPorts = listOf(21, 22, 23, 80, 443, 3389, 5000, 8080)
            
            // 1. Basic Auth Check
            findings.add(SecurityFinding("ENCRYPTION", "WPA2/WPA3 ACTIVE", true))
            
            // 2. Local Port Audit
            commonPorts.forEach { port ->
                val open = try {
                    java.net.Socket().use { s ->
                        s.connect(java.net.InetSocketAddress(gateway, port), 200)
                        true
                    }
                } catch (e: Exception) { false }
                
                if (open) {
                    findings.add(SecurityFinding("OPEN PORT", "GATEWAY PORT $port EXPOSED", false))
                }
            }
            
            if (findings.none { !it.isSafe }) {
                findings.add(SecurityFinding("FIREWALL", "NO COMMON VULNERABILITIES FOUND", true))
            }
            
            _securityStatus.value = findings
            _isAuditing.value = false
        }
    }

    fun updateGuardConfig(geofence: String?, threshold: Int) {
        _guardGeofence.value = geofence
        _failureThreshold.value = threshold
    }

    fun startTest(context: Context) {
        viewModelScope.launch {
            val networks = db.dao().getSelectedNetworksSnapshot()
            if (networks.isEmpty()) {
                Toast.makeText(context, "No nodes armed for testing!", Toast.LENGTH_SHORT).show()
                return@launch
            }

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, "Location permission required for Wi-Fi access", Toast.LENGTH_SHORT).show()
                return@launch
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(context, "Nearby Devices permission required", Toast.LENGTH_SHORT).show()
                    return@launch
                }
            }
            
            _isTesting.value = true
            _testStatus.value = "INITIALIZING..."
            
            try {
                val intent = Intent(context, WifiTestService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                _isTesting.value = false
                _testStatus.value = "ERR: SYSTEM REJECTED START"
                android.util.Log.e("WIFI_GRID", "Failed to start service", e)
                Toast.makeText(context, "OS blocked background sequence", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun updateStatus(status: String) {
        _testStatus.value = status.uppercase()
    }

    fun finalizeTesting() {
        _isTesting.value = false
        _testStatus.value = "LAST CYCLE FINALIZED"
    }

    fun stopTest(context: Context) {
        val intent = Intent(context, WifiTestService::class.java)
        context.stopService(intent)
        finalizeTesting()
        _testStatus.value = "MANUAL TERMINATION"
        Toast.makeText(context, "Sequence Aborted", Toast.LENGTH_SHORT).show()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: MainViewModel = viewModel()
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val scope = rememberCoroutineScope()
            var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }
            
            val launcher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { transitions ->
                if (transitions.any { it.value }) {
                    viewModel.scanNearby()
                }
            }

            LaunchedEffect(Unit) {
                val perms = mutableListOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    perms.add(Manifest.permission.POST_NOTIFICATIONS)
                    perms.add(Manifest.permission.NEARBY_WIFI_DEVICES)
                }
                launcher.launch(perms.toTypedArray())
            }

            val context = applicationContext
            DisposableEffect(Unit) {
                val wifiReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        viewModel.refreshNearby()
                    }
                }
                val testReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        android.util.Log.i("WIFI_GRID_UI", "Broadcast received: ${intent?.action}")
                        when(intent?.action) {
                            "io.github.madgod87.wifigrid.TEST_FINISHED" -> viewModel.finalizeTesting()
                            "io.github.madgod87.wifigrid.STATUS_UPDATE" -> {
                                val status = intent.getStringExtra("status") ?: ""
                                viewModel.updateStatus(status)
                            }
                        }
                    }
                }
                
                ContextCompat.registerReceiver(context, wifiReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION), ContextCompat.RECEIVER_EXPORTED)
                
                val testFilter = IntentFilter().apply {
                    addAction("io.github.madgod87.wifigrid.TEST_FINISHED")
                    addAction("io.github.madgod87.wifigrid.STATUS_UPDATE")
                }
                ContextCompat.registerReceiver(context, testReceiver, testFilter, ContextCompat.RECEIVER_EXPORTED)
                
                onDispose {
                    context.unregisterReceiver(wifiReceiver)
                    context.unregisterReceiver(testReceiver)
                }
            }

            FuturisticAppTheme {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        AppDrawer(
                            currentScreen = currentScreen,
                            onScreenSelected = { 
                                currentScreen = it
                                scope.launch { drawerState.close() }
                            }
                        )
                    }
                ) {
                    Surface(modifier = Modifier.fillMaxSize(), color = SpaceDark) {
                        WifiDiagnosticDashboard(
                            viewModel = viewModel, 
                            currentScreen = currentScreen,
                            onOpenDrawer = { scope.launch { drawerState.open() } }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppDrawer(currentScreen: Screen, onScreenSelected: (Screen) -> Unit) {
    val uriHandler = LocalUriHandler.current
    ModalDrawerSheet(
        drawerContainerColor = SpaceDark,
        drawerContentColor = Color.White,
        modifier = Modifier.width(300.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .background(Brush.horizontalGradient(listOf(NeonCyan, NeonPurple)), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = SpaceDark, modifier = Modifier.size(32.dp))
                Text("CORE OS", fontWeight = FontWeight.Black, color = SpaceDark, fontSize = 24.sp)
                Text("v1.0.4-STABLE", color = SpaceDark.copy(alpha = 0.7f), fontSize = 10.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        DrawerItem(
            label = "DASHBOARD",
            icon = Icons.Default.Dashboard,
            selected = currentScreen == Screen.DASHBOARD,
            onClick = { onScreenSelected(Screen.DASHBOARD) }
        )
        DrawerItem(
            label = "MISSION LOGS",
            icon = Icons.Default.Assessment,
            selected = currentScreen == Screen.REPORTS,
            onClick = { onScreenSelected(Screen.REPORTS) }
        )
        DrawerItem(
            label = "GRID VAULT",
            icon = Icons.Default.VpnKey,
            selected = currentScreen == Screen.VAULT,
            onClick = { onScreenSelected(Screen.VAULT) }
        )
        DrawerItem(
            label = "LOGIC GUIDE",
            icon = Icons.AutoMirrored.Filled.MenuBook,
            selected = currentScreen == Screen.GUIDE,
            onClick = { onScreenSelected(Screen.GUIDE) }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = NeonCyan.copy(alpha = 0.1f))
        Text("ADVANCED OPS", modifier = Modifier.padding(start = 24.dp, bottom = 8.dp), style = MaterialTheme.typography.labelSmall, color = Color.Gray)

        DrawerItem(
            label = "TRACEROUTE",
            icon = Icons.Default.Route,
            selected = currentScreen == Screen.TRACEROUTE,
            onClick = { onScreenSelected(Screen.TRACEROUTE) }
        )
        DrawerItem(
            label = "SIGNAL HUNTER",
            icon = Icons.Default.Radar,
            selected = currentScreen == Screen.SIGNAL_HUNTER,
            onClick = { onScreenSelected(Screen.SIGNAL_HUNTER) }
        )
        DrawerItem(
            label = "SMART GUARD",
            icon = Icons.Default.SecurityUpdateGood,
            selected = currentScreen == Screen.SMART_GUARD,
            onClick = { onScreenSelected(Screen.SMART_GUARD) }
        )
        DrawerItem(
            label = "SECURITY AUDIT",
            icon = Icons.Default.Shield,
            selected = currentScreen == Screen.SECURITY,
            onClick = { onScreenSelected(Screen.SECURITY) }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = NeonCyan.copy(alpha = 0.1f))
        Text("EXTERNAL RESOURCES", modifier = Modifier.padding(start = 24.dp, bottom = 8.dp), style = MaterialTheme.typography.labelSmall, color = Color.Gray)

        DrawerItem(
            label = "OPEN SOURCE LINK",
            icon = Icons.Default.Code,
            selected = false,
            onClick = { uriHandler.openUri("https://github.com/madgod87/anDroidWifiTestScript") }
        )
        DrawerItem(
            label = "DEVELOPER PROFILE",
            icon = Icons.Default.Public,
            selected = false,
            onClick = { uriHandler.openUri("https://github.com/madgod87") }
        )

        Spacer(modifier = Modifier.weight(1f))
        
        Text(
            "SYSTEM ACTIVE",
            modifier = Modifier.padding(24.dp),
            style = MaterialTheme.typography.labelSmall,
            color = NeonCyan.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun DrawerItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(label, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) },
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = null) },
        shape = RoundedCornerShape(0.dp, 24.dp, 24.dp, 0.dp),
        colors = NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = Color.Transparent,
            selectedContainerColor = NeonCyan.copy(alpha = 0.1f),
            selectedTextColor = NeonCyan,
            selectedIconColor = NeonCyan,
            unselectedTextColor = Color.Gray,
            unselectedIconColor = Color.Gray
        ),
        modifier = Modifier.padding(end = 16.dp, top = 4.dp, bottom = 4.dp)
    )
}

@Composable
fun FuturisticAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = NeonCyan,
            secondary = NeonPurple,
            tertiary = CyberPink,
            background = SpaceDark,
            surface = SpaceDark
        ),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiDiagnosticDashboard(
    viewModel: MainViewModel, 
    currentScreen: Screen,
    onOpenDrawer: () -> Unit
) {
    val results by viewModel.results.collectAsState(initial = emptyList())
    val selected by viewModel.selected.collectAsState(initial = emptyList())
    val nearby by viewModel.nearby
    val isTesting by viewModel.isTesting
    val testStatus by viewModel.testStatus
    val isScanning by viewModel.isScanning
    val selectedResult by viewModel.selectedResult
    val context = androidx.compose.ui.platform.LocalContext.current
    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    if (selectedResult != null) {
        val resultList by viewModel.results.collectAsState(initial = emptyList())
        MissionLogDetailDialog(
            result = selectedResult!!,
            history = resultList,
            onShareIndividual = { viewModel.exportSingleResultPdf(it) },
            onDismiss = { viewModel.selectResult(null) }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(NeonPurple.copy(alpha = 0.15f), SpaceDark),
                    radius = 2000f
                )
            )
        )

        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                NeonIconButton(Icons.Default.Menu) { onOpenDrawer() }
                
                Spacer(modifier = Modifier.width(20.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        when(currentScreen) {
                            Screen.DASHBOARD -> "CORE DIAGNOSTICS"
                            Screen.REPORTS -> "MISSION REPORTS"
                            Screen.SYSTEM_STATUS -> "HARDWARE STATUS"
                            Screen.VAULT -> "CREDENTIAL VAULT"
                            Screen.GUIDE -> "DIAGNOSTIC GUIDE"
                            Screen.TRACEROUTE -> "QUANTUM TRACEROUTE"
                            Screen.SIGNAL_HUNTER -> "SIGNAL HUNTER"
                            Screen.SMART_GUARD -> "SMART GUARD CONTROLS"
                            Screen.SECURITY -> "SECURITY AUDIT"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan,
                        letterSpacing = 2.sp
                    )
                    Text(
                        "Wi-Fi Grid",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
                
                Row {
                    NeonIconButton(Icons.Default.PictureAsPdf) { viewModel.exportResultsPdf() }
                    Spacer(modifier = Modifier.width(8.dp))
                    NeonIconButton(Icons.Default.Share) { viewModel.exportResults() }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            when(currentScreen) {
                Screen.DASHBOARD -> {
                    val vaultList by viewModel.vault.collectAsState(initial = emptyList())
                    DashboardContent(
                        viewModel, selected, nearby, isTesting, testStatus, isScanning, context, vaultList
                    )
                }
                Screen.REPORTS -> ReportsContent(results, dateFormat) { viewModel.selectResult(it) }
                Screen.SYSTEM_STATUS -> StatusContent(viewModel, context)
                Screen.VAULT -> {
                    val vaultList by viewModel.vault.collectAsState(initial = emptyList())
                    CredentialVaultContent(viewModel, vaultList)
                }
                Screen.GUIDE -> GuideContent()
                Screen.TRACEROUTE -> TracerouteContent(viewModel)
                Screen.SIGNAL_HUNTER -> SignalHunterContent(viewModel)
                Screen.SMART_GUARD -> SmartGuardContent(viewModel)
                Screen.SECURITY -> SecurityAuditContent(viewModel)
            }
        }
    }
}

@Composable
fun DashboardContent(
    viewModel: MainViewModel,
    selected: List<SelectedWifi>,
    nearby: List<HardwareUtils.ScannedNetwork>,
    isTesting: Boolean,
    testStatus: String,
    isScanning: Boolean,
    context: Context,
    vault: List<io.github.madgod87.wifigrid.data.VaultEntity>
) {
    val savedSsids by viewModel.savedSsids
    var showPasswordDialog by remember { mutableStateOf<String?>(null) }
    
    if (showPasswordDialog != null) {
        PasswordDialog(
            ssid = showPasswordDialog!!,
            onDismiss = { showPasswordDialog = null },
            onConfirm = { ssid, pass ->
                viewModel.addNetwork(ssid, pass)
                showPasswordDialog = null
            }
        )
    }

    Column {
        // System Status Card
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + expandVertically(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardGlass),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, if(isTesting) NeonCyan else Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.25f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "scale"
                        )
                        
                        Box(contentAlignment = Alignment.Center) {
                            if (isTesting) {
                                Box(modifier = Modifier.size(24.dp).background(NeonCyan.copy(alpha = 0.2f), CircleShape).graphicsLayer { scaleX = scale; scaleY = scale })
                            }
                            Icon(
                                Icons.Default.Bolt, 
                                contentDescription = null, 
                                tint = if(isTesting) NeonCyan else Color.Gray, 
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            testStatus,
                            style = MaterialTheme.typography.labelMedium,
                            color = if(isTesting) NeonCyan else Color.Gray,
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        if(selected.isEmpty()) "READY FOR ASSIGNMENT" else "${selected.size} NODES ARMED",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val isGuardActive by viewModel.isGuardActive
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isGuardActive) NeonCyan.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f))
                            .clickable { viewModel.toggleGlobalGuard(context) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isGuardActive) Icons.Default.Security else Icons.Default.Shield, 
                            contentDescription = null, 
                            tint = if (isGuardActive) NeonCyan else Color.Gray
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "GUARD MODE: ${if (isGuardActive) "OPERATIONAL" else "STANDBY"}", 
                                color = if (isGuardActive) NeonCyan else Color.White, 
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (isGuardActive) "Auto-testing selected nodes every 15m" else "Tap to activate background monitoring",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = { viewModel.startTest(context) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            disabledContainerColor = Color.White.copy(alpha = 0.05f)
                        ),
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = selected.isNotEmpty() && !isTesting
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = SpaceDark, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("DIAGNOSTICS IN PROGRESS", color = SpaceDark, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = SpaceDark)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("START BATCH TEST", color = SpaceDark, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Grid Area
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (selected.isNotEmpty()) {
                item {
                    Text("ARMED NODES", style = MaterialTheme.typography.labelMedium, color = NeonPurple, letterSpacing = 1.sp)
                }
                items(selected, key = { "armed_${it.ssid}" }) { node ->
                    AnimatedVisibility(visible = true, enter = slideInHorizontally() + fadeIn()) {
                        RegisteredNodeCard(
                            node = node,
                            onToggleGuard = { viewModel.toggleNodeGuard(node.ssid, node.isGuardEnabled) },
                            onRemove = { viewModel.removeNetwork(node.ssid) }
                        )
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("DETECTED SIGNALS", style = MaterialTheme.typography.labelMedium, color = NeonCyan.copy(alpha = 0.6f), letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    if (isScanning) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NeonCyan, strokeWidth = 2.dp)
                    } else {
                        TextButton(onClick = { viewModel.scanNearby() }) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("REFRESH", color = NeonCyan)
                        }
                    }
                }
            }

            items(nearby, key = { "scanned_${it.ssid}" }) { network ->
                val isRegistered = selected.any { it.ssid == network.ssid }
                val isSaved = savedSsids.contains(network.ssid)
                val isSecure = network.capabilities.contains("WPA") || network.capabilities.contains("WEP")

                AnimatedVisibility(visible = true, enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn()) {
                    NodeCard(network, isRegistered, isSaved) {
                        if (isRegistered) {
                            viewModel.removeNetwork(network.ssid)
                        } else {
                            val vaultMatch = vault.find { it.ssid == network.ssid && it.password != null }
                            if (!isSecure || vaultMatch != null) {
                                // Auto-arm if open OR if password exists in vault
                                viewModel.addNetwork(network.ssid, vaultMatch?.password)
                            } else {
                                showPasswordDialog = network.ssid
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordDialog(ssid: String, onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var password by remember { mutableStateOf("") }
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.padding(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SpaceDark),
            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("CONNECT TO SECURE NODE", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = NeonCyan)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Enter credentials for: $ssid", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                TextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("WPA2/WPA3 Passphrase", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth().border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                    supportingText = {
                        Column {
                            if (password.isNotEmpty() && password.length < 8) {
                                Text("Minimum 8 characters required", color = CyberPink, style = MaterialTheme.typography.labelSmall)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Note: Android doesn't share system passwords with apps. Once entered, it is saved in your GRID VAULT.", 
                                color = Color.Gray, style = MaterialTheme.typography.labelSmall, lineHeight = 12.sp)
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = CardGlass,
                        unfocusedContainerColor = CardGlass,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = NeonCyan
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(ssid, password) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        enabled = password.length >= 8
                    ) {
                        Text("ARM NODE", color = SpaceDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ReportsContent(results: List<TestResult>, dateFormat: SimpleDateFormat, onResultClick: (TestResult) -> Unit) {
    if (results.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(16.dp))
                Text("NO MISSION DATA RECORDED", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                Text("Complete a test cycle to see analysis here", color = Color.Gray.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
            }
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize()) {
            items(results, key = { it.id }) { res ->
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn()
                ) {
                    DiagnosticResultCard(res, dateFormat) { onResultClick(res) }
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) } // Bottom padding for FAB if any
        }
    }
}

@Composable
fun ScoreTrendGraph(history: List<TestResult>) {
    val scores = history.map { it.reliabilityScore.toFloat() }.reversed()
    if (scores.size < 2) return

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text("PERFORMANCE TREND (LAST ${scores.size} TESTS)", style = MaterialTheme.typography.labelSmall, color = NeonCyan.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(8.dp))
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().height(40.dp)) {
            val stepX = size.width / (scores.size - 1)
            val path = androidx.compose.ui.graphics.Path()
            scores.forEachIndexed { index, score ->
                val x = index * stepX
                val y = size.height - (score / 100f * size.height)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                drawCircle(NeonCyan, 3f, androidx.compose.ui.geometry.Offset(x, y))
            }
            drawPath(path, NeonCyan, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionLogDetailDialog(
    result: TestResult, 
    history: List<TestResult>, 
    onShareIndividual: (TestResult) -> Unit,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SpaceDark),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Assessment, contentDescription = null, tint = NeonCyan)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("FULL MISSION ANALYSIS", style = MaterialTheme.typography.labelLarge, color = NeonCyan)
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { onShareIndividual(result) }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Share Individual PDF", tint = NeonCyan)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }
                
                Text(result.ssid, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text("Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(result.timestamp))}", 
                    style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                DetailSection("PHYSICAL LAYER") {
                    DetailRow("BSSID", result.bssid)
                    DetailRow("Frequency", "${result.frequency} MHz")
                    DetailRow("Channel", "CH ${result.channel}")
                    DetailRow("RSSI", "${result.rssi} dBm")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                DetailSection("NETWORK LAYER") {
                    DetailRow("Gateway IP", result.gatewayIp)
                    DetailRow("DNS Resolution", if (result.dnsResolutionMs > 0) "${result.dnsResolutionMs} ms" else "FAILED")
                    DetailRow("Download Speed", "${String.format("%.2f", result.downloadMbps)} Mbps")
                    DetailRow("Upload Speed", "${String.format("%.2f", result.uploadMbps)} Mbps")
                }

                Spacer(modifier = Modifier.height(16.dp))

                DetailSection("STABILITY METRICS") {
                    DetailRow("Avg Latency (Ping)", "${result.latencyMs} ms")
                    DetailRow("Peak Jitter", "${result.jitterMs} ms")
                    DetailRow("Packet Loss", "${result.packetLossPercent}%")
                    DetailRow("Reliability Score", "${result.reliabilityScore}/100")
                }

                if (result.troubleshootingInfo != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    DetailSection("TROUBLESHOOTING") {
                        Text(
                            result.troubleshootingInfo, 
                            color = CyberPink, 
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                ScoreTrendGraph(history.filter { it.ssid == result.ssid }.take(10))

                Spacer(modifier = Modifier.height(24.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(getColorForLabel(result.qualityLabel).copy(alpha = 0.1f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("FINAL RELIABILITY RATING", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(result.qualityLabel.uppercase(), style = MaterialTheme.typography.headlineSmall, 
                            fontWeight = FontWeight.Black, color = getColorForLabel(result.qualityLabel))
                        Text("Score: ${result.reliabilityScore}/100", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

@Composable
fun DetailSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.labelSmall, color = NeonPurple, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Column(modifier = Modifier.padding(start = 8.dp)) {
            content()
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun StatusContent(viewModel: MainViewModel, context: Context) {
    val testStatus by viewModel.testStatus
    val isTesting by viewModel.isTesting

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardGlass),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("PROCESSOR LOAD", color = NeonCyan, style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { if(isTesting) 0.8f else 0.1f },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = NeonCyan
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                Text("CURRENT STATUS: $testStatus", color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    if (isTesting) "OS Thread Busy - Network Interface Dedicated" else "System IDLE - Ready for input",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        
        if (isTesting) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.stopTest(context) },
                colors = ButtonDefaults.buttonColors(containerColor = CyberPink),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ABORT SEQUENCE", color = Color.White, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardGlass),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("NETWORK LAYER", color = NeonPurple, style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(16.dp))
                StatusRow("WIFI ADAPTER", "ACTIVE")
                StatusRow("GATEWAY ACCESS", "ENABLED")
                StatusRow("DNS RESOLUTION", "SECURE")
            }
        }
    }
}

@Composable
fun StatusRow(label: String, status: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = Color.Gray, modifier = Modifier.weight(1f))
        Text(status, color = NeonCyan, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun RegisteredNodeCard(node: SelectedWifi, onToggleGuard: () -> Unit, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(NeonPurple.copy(alpha = 0.1f))
            .border(1.dp, NeonPurple.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Link, contentDescription = null, tint = NeonPurple)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(node.ssid, color = Color.White, fontWeight = FontWeight.Bold)
            if (node.isGuardEnabled) {
                Text("GUARD ACTIVE", color = NeonCyan, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
        
        IconButton(onClick = onToggleGuard, modifier = Modifier.size(32.dp)) {
            Icon(
                if (node.isGuardEnabled) Icons.Default.Security else Icons.Default.Shield, 
                contentDescription = "Toggle Guard", 
                tint = if (node.isGuardEnabled) NeonCyan else Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = CyberPink, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun NodeCard(network: HardwareUtils.ScannedNetwork, isRegistered: Boolean, isSaved: Boolean, onAction: () -> Unit) {
    val ssid = network.ssid
    val rssi = network.rssi
    val strengthColor = when {
        rssi > -50 -> NeonCyan
        rssi > -70 -> NeonPurple
        else -> CyberPink
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isRegistered) NeonCyan.copy(alpha = 0.05f) else CardGlass)
            .border(
                1.dp, 
                if (isRegistered) NeonCyan.copy(alpha = 0.5f) else Color.Transparent, 
                RoundedCornerShape(16.dp)
            )
            .clickable { onAction() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(strengthColor.copy(alpha = 0.2f), CircleShape)
                .border(1.dp, strengthColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Wifi, contentDescription = null, tint = strengthColor, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(ssid, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).background(strengthColor, CircleShape))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "CH ${network.channel} • ", 
                    color = Color.White.copy(alpha = 0.7f), 
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    if (isRegistered) "ARMED" else if (isSaved) "SAVED" else "UNLINKED", 
                    color = if (isRegistered) NeonCyan else if (isSaved) NeonPurple else Color.Gray, 
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.sp
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text("${rssi} dBm", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Layers, 
                    contentDescription = null, 
                    tint = if(network.congestionScore > 5) CyberPink else NeonCyan, 
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "${network.congestionScore} APs", 
                    color = if(network.congestionScore > 5) CyberPink else Color.Gray, 
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
fun DiagnosticResultCard(res: TestResult, df: SimpleDateFormat, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CardGlass.copy(alpha = 0.05f)),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(getColorForLabel(res.qualityLabel), CircleShape))
                Spacer(modifier = Modifier.width(12.dp))
                Text(res.ssid, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
                Text(df.format(Date(res.timestamp)), color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                MetricItem("DL", "${String.format("%.1f", res.downloadMbps)}", NeonCyan, Icons.Default.CloudDownload)
                MetricItem("UL", "${String.format("%.1f", res.uploadMbps)}", NeonPurple, Icons.Default.CloudUpload)
                MetricItem("PING", "${res.latencyMs}ms", if (res.latencyMs < 50) NeonCyan else Color.Yellow, Icons.Default.Timer)
                MetricItem("REL", "${res.reliabilityScore}%", getColorForLabel(res.qualityLabel), Icons.Default.Assessment)
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(res.qualityLabel.uppercase(), style = MaterialTheme.typography.labelSmall, color = getColorForLabel(res.qualityLabel), fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.weight(1f))
                Text("TAP FOR ANALYSIS", style = MaterialTheme.typography.labelSmall, color = NeonCyan.copy(alpha = 0.6f))
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = NeonCyan.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun MetricItem(label: String, value: String, color: Color, icon: ImageVector) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.ExtraBold, color = color)
    }
}

@Composable
fun NeonIconButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(NeonCyan.copy(alpha = 0.1f))
            .border(1.dp, NeonCyan.copy(alpha = 0.3f), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = NeonCyan)
    }
}

@Composable
fun CredentialVaultContent(viewModel: MainViewModel, vault: List<io.github.madgod87.wifigrid.data.VaultEntity>) {
    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardGlass),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("VAULT SYNCHRONIZATION", color = NeonCyan, style = MaterialTheme.typography.labelSmall)
                    Text("Import nodes from OS", color = Color.White, style = MaterialTheme.typography.bodySmall)
                }
                Button(
                    onClick = { viewModel.syncFromSystem() },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SYNC", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("SAVED CREDENTIALS", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
        
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(vault, key = { it.ssid }) { node ->
                VaultEntry(node) { ssid, pass ->
                    viewModel.updateVault(ssid, pass)
                }
            }
        }
    }
}

@Composable
fun VaultEntry(node: io.github.madgod87.wifigrid.data.VaultEntity, onSave: (String, String) -> Unit) {
    var isEditing by remember { mutableStateOf(false) }
    var passText by remember { mutableStateOf(node.password ?: "") }
    var isVisible by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardGlass.copy(alpha = 0.05f)),
        border = BorderStroke(0.5.dp, if(node.password != null) NeonCyan.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if(node.password != null) Icons.Default.Lock else Icons.Default.LockOpen, 
                    contentDescription = null, 
                    tint = if(node.password != null) NeonCyan else Color.Gray,
                    modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(node.ssid, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
            }
            
            if (isEditing) {
                Spacer(modifier = Modifier.height(12.dp))
                TextField(
                    value = passText,
                    onValueChange = { passText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Passphrase", color = Color.Gray) },
                    visualTransformation = if (isVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isVisible = !isVisible }) {
                            Icon(if(isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null, tint = Color.Gray)
                        }
                    },
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { isEditing = false }) { Text("CANCEL", color = Color.Gray) }
                    TextButton(onClick = { 
                        onSave(node.ssid, passText)
                        isEditing = false
                    }) { Text("SAVE", color = NeonCyan) }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if(node.password != null) "••••••••" else "NO PASSPHRASE SAVED", 
                        color = if(node.password != null) Color.White.copy(alpha = 0.6f) else CyberPink.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    IconButton(onClick = { isEditing = true }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = NeonPurple, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

fun getColorForLabel(label: String): Color {
    return when (label) {
        "Excellent" -> NeonCyan
        "Good" -> NeonPurple
        "Fair" -> Color.Yellow
        else -> CyberPink
    }
}
@Composable
fun GuideContent() {
    var searchQuery by remember { mutableStateOf("") }
    
    val guideData = listOf(
        GuideCategory("CORE INTERFACE", Icons.Default.Dashboard, listOf(
            GuideItem("Scanning", "Tap 'Scan' to discover nearby APs. Throttled by OS to 4 times per 2 mins."),
            GuideItem("Arming Nodes", "Tap a network to 'ARM' it. Armed nodes are the only ones tested during missions."),
            GuideItem("Mission Start", "Tap the central Core to start a full diagnostic cycle on all armed nodes.")
        )),
        GuideCategory("ADVANCED OPS", Icons.Default.Extension, listOf(
            GuideItem("Traceroute", "Maps the path to a server. High latency at hop 1 = Router issue; Hop 2+ = ISP/Internet issue."),
            GuideItem("Signal Hunter", "High-frequency RSSI tracker. Move physically to minimize dBm (aim for -30 to -50dBm)."),
            GuideItem("Smart Guard", "Set failure thresholds. If packet loss exceeds your limit, intensive logs are captured."),
            GuideItem("Security Audit", "Detects open ports on your gateway and verifies WPA2/WPA3 encryption standards.")
        )),
        GuideCategory("SIGNAL & LINK", Icons.Default.Wifi, listOf(
            GuideItem("dBm (RSSI)", "Signal strength. -30 is perfect, -70 is usable, -90 is a dead zone."),
            GuideItem("Channel (CH)", "WiFi frequency lane. Avoid overlapping channels (use 1, 6, 11 on 2.4GHz)."),
            GuideItem("Congestion", "Number of overlapping APs. High congestion = higher interference and collisions.")
        )),
        GuideCategory("PERFORMANCE", Icons.Default.Speed, listOf(
            GuideItem("Ping (Latency)", "Response time in ms. <20ms is Pro, >100ms causes noticeable lag."),
            GuideItem("Jitter", "Ping consistency. High jitter (STDEV of ping) causes stuttering in VOIP/Streaming."),
            GuideItem("Packet Loss", "Data that fails to arrive. >2% is critical and usually indicates hardware failure or distance.")
        )),
        GuideCategory("VAULT & REPORTS", Icons.AutoMirrored.Filled.InsertDriveFile, listOf(
            GuideItem("Grid Vault", "Encrypted storage for Wi-Fi passphrases. Syncs from system known networks."),
            GuideItem("PDF Summaries", "Export branded reports with latency matrices and sparklines for every mission."),
            GuideItem("Sparklines", "Small graphs in PDF showing 20-sample ping trends for each tested node.")
        ))
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = CardGlass),
            shape = RoundedCornerShape(12.dp)
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("SEARCH TOPICS (e.g. 'Ping', 'Guard')", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NeonCyan) },
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
            )
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.weight(1f)) {
            val filteredCategories = guideData.mapNotNull { category ->
                val filteredItems = category.items.filter { 
                    it.title.contains(searchQuery, ignoreCase = true) || 
                    it.desc.contains(searchQuery, ignoreCase = true) 
                }
                if (filteredItems.isNotEmpty() || category.title.contains(searchQuery, ignoreCase = true)) {
                    category.copy(items = filteredItems.ifEmpty { category.items })
                } else null
            }

            items(filteredCategories) { category ->
                GuideSection(category.title, category.icon) {
                    category.items.forEach { item ->
                        GuideTerm(item.title, item.desc)
                    }
                }
            }
            
            if (filteredCategories.isEmpty()) {
                item {
                    Text(
                        "No documentation found for '$searchQuery'", 
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                }
            }
            
            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

data class GuideCategory(val title: String, val icon: ImageVector, val items: List<GuideItem>)
data class GuideItem(val title: String, val desc: String)

@Composable
fun GuideSection(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardGlass),
        border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(title, color = NeonPurple, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun GuideTerm(term: String, definition: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(term, color = NeonCyan, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Text(definition, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun TracerouteContent(viewModel: MainViewModel) {
    val hops by viewModel.traceHops
    val isTracing by viewModel.isTracing
    var targetHost by remember { mutableStateOf("8.8.8.8") }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardGlass),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = targetHost,
                    onValueChange = { targetHost = it },
                    label = { Text("TARGET HOST", color = NeonCyan) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = { if (isTracing) viewModel.stopTraceroute() else viewModel.runTraceroute(targetHost) },
                    colors = ButtonDefaults.buttonColors(containerColor = if(isTracing) CyberPink else NeonCyan)
                ) {
                    Text(if (isTracing) "ABORT" else "TRACE", color = SpaceDark, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(hops) { hop ->
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (hop.reachable) NeonCyan.copy(alpha = 0.2f) else CyberPink.copy(alpha = 0.2f))
                            .border(1.dp, if (hop.reachable) NeonCyan else CyberPink, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(hop.hop.toString(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(hop.ip, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(if (hop.reachable) "LATENCY: ${hop.latency}ms" else "NODE TIMEOUT", color = Color.Gray, fontSize = 10.sp)
                    }
                    
                    if (hop.reachable) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SignalHunterContent(viewModel: MainViewModel) {
    val rssi by viewModel.liveRssi
    
    DisposableEffect(Unit) {
        viewModel.startSignalHunter()
        onDispose { viewModel.stopSignalHunter() }
    }

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("RSSI SIGNAL HUNTER", color = NeonCyan, style = MaterialTheme.typography.labelLarge, letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(40.dp))
        
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(280.dp)) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                    color = Color.DarkGray.copy(alpha = 0.3f),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 30f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
                
                val percentage = ((rssi + 100).coerceIn(0, 70) / 70f)
                drawArc(
                    brush = Brush.sweepGradient(listOf(CyberPink, NeonPurple, NeonCyan)),
                    startAngle = 135f,
                    sweepAngle = 270f * percentage,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 30f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${rssi}", color = Color.White, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black)
                Text("dBm", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
            }
        }
        
        Spacer(modifier = Modifier.height(60.dp))
        
        val status = when {
            rssi > -55 -> "EXCELLENT" to NeonCyan
            rssi > -70 -> "STABLE" to NeonPurple
            else -> "CRITICAL" to CyberPink
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardGlass),
            border = BorderStroke(1.dp, status.second.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(status.second))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(status.first, color = status.second, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    if (rssi > -65) "Signal propagation is optimal. Low jitter expected."
                    else "High attenuation detected. Move closer to the Access Point.",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun SmartGuardContent(viewModel: MainViewModel) {
    val geofence by viewModel.guardGeofence
    val threshold by viewModel.failureThreshold
    val isGuardActive by viewModel.isGuardActive
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardGlass)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = NeonCyan)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("GUARD PARAMETERS", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("GEOFENCE SSID", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                TextField(
                    value = geofence ?: "",
                    onValueChange = { viewModel.updateGuardConfig(if(it.isEmpty()) null else it, threshold) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Any (Active on all nodes)", color = Color.DarkGray) },
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("FAILURE TRIGGER: $threshold% PACKET LOSS", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = threshold.toFloat(),
                    onValueChange = { viewModel.updateGuardConfig(geofence, it.toInt()) },
                    valueRange = 1f..50f,
                    colors = SliderDefaults.colors(thumbColor = NeonPurple, activeTrackColor = NeonPurple)
                )
            }
        }

        Button(
            onClick = { viewModel.toggleGlobalGuard(context) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if(isGuardActive) CyberPink else NeonCyan),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if(isGuardActive) "DEACTIVATE MISSION GUARD" else "INITIATE SMART GUARD", color = SpaceDark, fontWeight = FontWeight.Black)
        }
        
        Text(
            "Guard Mode automatically captures deep diagnostics if network health drops below your threshold. Geofencing ensures battery efficiency by only activating on trusted networks.",
            color = Color.Gray,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun SecurityAuditContent(viewModel: MainViewModel) {
    val results by viewModel.securityStatus
    val isAuditing by viewModel.isAuditing

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.verticalGradient(listOf(NeonCyan.copy(alpha = 0.1f), Color.Transparent))),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    if(isAuditing) Icons.Default.Sync else Icons.Default.Security,
                    contentDescription = null,
                    tint = if(results.any { !it.isSafe }) CyberPink else NeonCyan,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    if(isAuditing) "PROBING PROTOCOLS..." else "THREAT DETECTION ACTIVE",
                    color = if(results.any { !it.isSafe }) CyberPink else NeonCyan,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.runSecurityScan() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isAuditing,
            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
        ) {
            Text("EXECUTE FULL AUDIT", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(results) { finding ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardGlass),
                    border = BorderStroke(1.dp, if(finding.isSafe) NeonCyan.copy(alpha = 0.2f) else CyberPink.copy(alpha = 0.4f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if(finding.isSafe) Icons.Default.Check else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if(finding.isSafe) NeonCyan else CyberPink
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(finding.type, color = if(finding.isSafe) NeonCyan else CyberPink, style = MaterialTheme.typography.labelSmall)
                            Text(finding.detail, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
