package com.example.wifitest.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "selected_networks")
data class SelectedWifi(
    @PrimaryKey val ssid: String,
    val password: String? = null,
    val isEnabled: Boolean = true,
    val isGuardEnabled: Boolean = false
)

@Entity(tableName = "wifi_vault")
data class VaultEntity(
    @PrimaryKey val ssid: String,
    val password: String? = null
)

@Entity(tableName = "test_results")
data class TestResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ssid: String,
    val timestamp: Long,
    val downloadMbps: Double,
    val uploadMbps: Double,
    val latencyMs: Long,
    val jitterMs: Long,
    val packetLossPercent: Int,
    val dnsResolutionMs: Long,
    val dhcpTimeMs: Long,
    val rssi: Int,
    val frequency: Int,
    val channel: Int,
    val bssid: String,
    val gatewayIp: String,
    val reliabilityScore: Int,
    val qualityLabel: String,
    val troubleshootingInfo: String? = null,
    val pingSamples: String? = null
)

@Dao
interface WifiDao {
    @Query("SELECT * FROM selected_networks")
    fun getSelectedNetworks(): Flow<List<SelectedWifi>>

    @Query("SELECT * FROM selected_networks")
    suspend fun getSelectedNetworksSnapshot(): List<SelectedWifi>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSelected(wifi: SelectedWifi)

    @Query("DELETE FROM selected_networks WHERE ssid = :ssid")
    suspend fun removeSelected(ssid: String)

    @Insert
    suspend fun insertResult(result: TestResult)

    @Query("SELECT * FROM test_results ORDER BY timestamp DESC")
    fun getRankedResults(): Flow<List<TestResult>>

    @Query("SELECT * FROM test_results WHERE ssid = :ssid ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getResultsForSsid(ssid: String, limit: Int): List<TestResult>

    @Query("SELECT * FROM wifi_vault")
    fun getVaultNetworks(): Flow<List<VaultEntity>>

    @Query("SELECT * FROM wifi_vault")
    suspend fun getVaultSnapshot(): List<VaultEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVault(vault: VaultEntity)

    @Query("DELETE FROM wifi_vault WHERE ssid = :ssid")
    suspend fun deleteVault(ssid: String)

    @Query("SELECT password FROM wifi_vault WHERE ssid = :ssid")
    suspend fun getPasswordFromVault(ssid: String): String?
}

@Database(entities = [SelectedWifi::class, TestResult::class, VaultEntity::class], version = 5, exportSchema = false)
abstract class WifiDatabase : RoomDatabase() {
    abstract fun dao(): WifiDao

    companion object {
        @Volatile
        private var INSTANCE: WifiDatabase? = null

        fun getDatabase(context: Context): WifiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WifiDatabase::class.java,
                    "wifi-db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
