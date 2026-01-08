package com.example.wifitest.data;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class WifiDatabase_Impl extends WifiDatabase {
  private volatile WifiDao _wifiDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(3) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `selected_networks` (`ssid` TEXT NOT NULL, `password` TEXT, `isEnabled` INTEGER NOT NULL, PRIMARY KEY(`ssid`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `test_results` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `ssid` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `downloadMbps` REAL NOT NULL, `uploadMbps` REAL NOT NULL, `latencyMs` INTEGER NOT NULL, `jitterMs` INTEGER NOT NULL, `packetLossPercent` INTEGER NOT NULL, `rssi` INTEGER NOT NULL, `frequency` INTEGER NOT NULL, `linkSpeed` INTEGER NOT NULL, `bssid` TEXT NOT NULL, `gatewayIp` TEXT NOT NULL, `reliabilityScore` INTEGER NOT NULL, `qualityLabel` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `wifi_vault` (`ssid` TEXT NOT NULL, `password` TEXT, PRIMARY KEY(`ssid`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '4ae59bc76916dd2eab4cbc1d340f53cc')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `selected_networks`");
        db.execSQL("DROP TABLE IF EXISTS `test_results`");
        db.execSQL("DROP TABLE IF EXISTS `wifi_vault`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsSelectedNetworks = new HashMap<String, TableInfo.Column>(3);
        _columnsSelectedNetworks.put("ssid", new TableInfo.Column("ssid", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSelectedNetworks.put("password", new TableInfo.Column("password", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSelectedNetworks.put("isEnabled", new TableInfo.Column("isEnabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSelectedNetworks = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSelectedNetworks = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSelectedNetworks = new TableInfo("selected_networks", _columnsSelectedNetworks, _foreignKeysSelectedNetworks, _indicesSelectedNetworks);
        final TableInfo _existingSelectedNetworks = TableInfo.read(db, "selected_networks");
        if (!_infoSelectedNetworks.equals(_existingSelectedNetworks)) {
          return new RoomOpenHelper.ValidationResult(false, "selected_networks(com.example.wifitest.data.SelectedWifi).\n"
                  + " Expected:\n" + _infoSelectedNetworks + "\n"
                  + " Found:\n" + _existingSelectedNetworks);
        }
        final HashMap<String, TableInfo.Column> _columnsTestResults = new HashMap<String, TableInfo.Column>(15);
        _columnsTestResults.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTestResults.put("ssid", new TableInfo.Column("ssid", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTestResults.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTestResults.put("downloadMbps", new TableInfo.Column("downloadMbps", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTestResults.put("uploadMbps", new TableInfo.Column("uploadMbps", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTestResults.put("latencyMs", new TableInfo.Column("latencyMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTestResults.put("jitterMs", new TableInfo.Column("jitterMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTestResults.put("packetLossPercent", new TableInfo.Column("packetLossPercent", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTestResults.put("rssi", new TableInfo.Column("rssi", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTestResults.put("frequency", new TableInfo.Column("frequency", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTestResults.put("linkSpeed", new TableInfo.Column("linkSpeed", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTestResults.put("bssid", new TableInfo.Column("bssid", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTestResults.put("gatewayIp", new TableInfo.Column("gatewayIp", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTestResults.put("reliabilityScore", new TableInfo.Column("reliabilityScore", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTestResults.put("qualityLabel", new TableInfo.Column("qualityLabel", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysTestResults = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesTestResults = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoTestResults = new TableInfo("test_results", _columnsTestResults, _foreignKeysTestResults, _indicesTestResults);
        final TableInfo _existingTestResults = TableInfo.read(db, "test_results");
        if (!_infoTestResults.equals(_existingTestResults)) {
          return new RoomOpenHelper.ValidationResult(false, "test_results(com.example.wifitest.data.TestResult).\n"
                  + " Expected:\n" + _infoTestResults + "\n"
                  + " Found:\n" + _existingTestResults);
        }
        final HashMap<String, TableInfo.Column> _columnsWifiVault = new HashMap<String, TableInfo.Column>(2);
        _columnsWifiVault.put("ssid", new TableInfo.Column("ssid", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWifiVault.put("password", new TableInfo.Column("password", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysWifiVault = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesWifiVault = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoWifiVault = new TableInfo("wifi_vault", _columnsWifiVault, _foreignKeysWifiVault, _indicesWifiVault);
        final TableInfo _existingWifiVault = TableInfo.read(db, "wifi_vault");
        if (!_infoWifiVault.equals(_existingWifiVault)) {
          return new RoomOpenHelper.ValidationResult(false, "wifi_vault(com.example.wifitest.data.VaultEntity).\n"
                  + " Expected:\n" + _infoWifiVault + "\n"
                  + " Found:\n" + _existingWifiVault);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "4ae59bc76916dd2eab4cbc1d340f53cc", "ae1dfe4c660cc44b758f0391b83e3cac");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "selected_networks","test_results","wifi_vault");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `selected_networks`");
      _db.execSQL("DELETE FROM `test_results`");
      _db.execSQL("DELETE FROM `wifi_vault`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(WifiDao.class, WifiDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public WifiDao dao() {
    if (_wifiDao != null) {
      return _wifiDao;
    } else {
      synchronized(this) {
        if(_wifiDao == null) {
          _wifiDao = new WifiDao_Impl(this);
        }
        return _wifiDao;
      }
    }
  }
}
