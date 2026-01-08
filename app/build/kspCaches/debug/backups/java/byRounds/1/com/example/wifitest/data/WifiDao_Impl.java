package com.example.wifitest.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class WifiDao_Impl implements WifiDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<SelectedWifi> __insertionAdapterOfSelectedWifi;

  private final EntityInsertionAdapter<TestResult> __insertionAdapterOfTestResult;

  private final EntityInsertionAdapter<VaultEntity> __insertionAdapterOfVaultEntity;

  private final SharedSQLiteStatement __preparedStmtOfRemoveSelected;

  private final SharedSQLiteStatement __preparedStmtOfDeleteVault;

  public WifiDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSelectedWifi = new EntityInsertionAdapter<SelectedWifi>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `selected_networks` (`ssid`,`password`,`isEnabled`) VALUES (?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SelectedWifi entity) {
        statement.bindString(1, entity.getSsid());
        if (entity.getPassword() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getPassword());
        }
        final int _tmp = entity.isEnabled() ? 1 : 0;
        statement.bindLong(3, _tmp);
      }
    };
    this.__insertionAdapterOfTestResult = new EntityInsertionAdapter<TestResult>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `test_results` (`id`,`ssid`,`timestamp`,`downloadMbps`,`uploadMbps`,`latencyMs`,`jitterMs`,`packetLossPercent`,`rssi`,`frequency`,`linkSpeed`,`bssid`,`gatewayIp`,`reliabilityScore`,`qualityLabel`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TestResult entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getSsid());
        statement.bindLong(3, entity.getTimestamp());
        statement.bindDouble(4, entity.getDownloadMbps());
        statement.bindDouble(5, entity.getUploadMbps());
        statement.bindLong(6, entity.getLatencyMs());
        statement.bindLong(7, entity.getJitterMs());
        statement.bindLong(8, entity.getPacketLossPercent());
        statement.bindLong(9, entity.getRssi());
        statement.bindLong(10, entity.getFrequency());
        statement.bindLong(11, entity.getLinkSpeed());
        statement.bindString(12, entity.getBssid());
        statement.bindString(13, entity.getGatewayIp());
        statement.bindLong(14, entity.getReliabilityScore());
        statement.bindString(15, entity.getQualityLabel());
      }
    };
    this.__insertionAdapterOfVaultEntity = new EntityInsertionAdapter<VaultEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `wifi_vault` (`ssid`,`password`) VALUES (?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final VaultEntity entity) {
        statement.bindString(1, entity.getSsid());
        if (entity.getPassword() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getPassword());
        }
      }
    };
    this.__preparedStmtOfRemoveSelected = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM selected_networks WHERE ssid = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteVault = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM wifi_vault WHERE ssid = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertSelected(final SelectedWifi wifi,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfSelectedWifi.insert(wifi);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertResult(final TestResult result,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfTestResult.insert(result);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertVault(final VaultEntity vault, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfVaultEntity.insert(vault);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object removeSelected(final String ssid, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfRemoveSelected.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, ssid);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfRemoveSelected.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteVault(final String ssid, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteVault.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, ssid);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteVault.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<SelectedWifi>> getSelectedNetworks() {
    final String _sql = "SELECT * FROM selected_networks";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"selected_networks"}, new Callable<List<SelectedWifi>>() {
      @Override
      @NonNull
      public List<SelectedWifi> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfSsid = CursorUtil.getColumnIndexOrThrow(_cursor, "ssid");
          final int _cursorIndexOfPassword = CursorUtil.getColumnIndexOrThrow(_cursor, "password");
          final int _cursorIndexOfIsEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "isEnabled");
          final List<SelectedWifi> _result = new ArrayList<SelectedWifi>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SelectedWifi _item;
            final String _tmpSsid;
            _tmpSsid = _cursor.getString(_cursorIndexOfSsid);
            final String _tmpPassword;
            if (_cursor.isNull(_cursorIndexOfPassword)) {
              _tmpPassword = null;
            } else {
              _tmpPassword = _cursor.getString(_cursorIndexOfPassword);
            }
            final boolean _tmpIsEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsEnabled);
            _tmpIsEnabled = _tmp != 0;
            _item = new SelectedWifi(_tmpSsid,_tmpPassword,_tmpIsEnabled);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getSelectedNetworksSnapshot(
      final Continuation<? super List<SelectedWifi>> $completion) {
    final String _sql = "SELECT * FROM selected_networks";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<SelectedWifi>>() {
      @Override
      @NonNull
      public List<SelectedWifi> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfSsid = CursorUtil.getColumnIndexOrThrow(_cursor, "ssid");
          final int _cursorIndexOfPassword = CursorUtil.getColumnIndexOrThrow(_cursor, "password");
          final int _cursorIndexOfIsEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "isEnabled");
          final List<SelectedWifi> _result = new ArrayList<SelectedWifi>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SelectedWifi _item;
            final String _tmpSsid;
            _tmpSsid = _cursor.getString(_cursorIndexOfSsid);
            final String _tmpPassword;
            if (_cursor.isNull(_cursorIndexOfPassword)) {
              _tmpPassword = null;
            } else {
              _tmpPassword = _cursor.getString(_cursorIndexOfPassword);
            }
            final boolean _tmpIsEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsEnabled);
            _tmpIsEnabled = _tmp != 0;
            _item = new SelectedWifi(_tmpSsid,_tmpPassword,_tmpIsEnabled);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<TestResult>> getRankedResults() {
    final String _sql = "SELECT * FROM test_results ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"test_results"}, new Callable<List<TestResult>>() {
      @Override
      @NonNull
      public List<TestResult> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSsid = CursorUtil.getColumnIndexOrThrow(_cursor, "ssid");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfDownloadMbps = CursorUtil.getColumnIndexOrThrow(_cursor, "downloadMbps");
          final int _cursorIndexOfUploadMbps = CursorUtil.getColumnIndexOrThrow(_cursor, "uploadMbps");
          final int _cursorIndexOfLatencyMs = CursorUtil.getColumnIndexOrThrow(_cursor, "latencyMs");
          final int _cursorIndexOfJitterMs = CursorUtil.getColumnIndexOrThrow(_cursor, "jitterMs");
          final int _cursorIndexOfPacketLossPercent = CursorUtil.getColumnIndexOrThrow(_cursor, "packetLossPercent");
          final int _cursorIndexOfRssi = CursorUtil.getColumnIndexOrThrow(_cursor, "rssi");
          final int _cursorIndexOfFrequency = CursorUtil.getColumnIndexOrThrow(_cursor, "frequency");
          final int _cursorIndexOfLinkSpeed = CursorUtil.getColumnIndexOrThrow(_cursor, "linkSpeed");
          final int _cursorIndexOfBssid = CursorUtil.getColumnIndexOrThrow(_cursor, "bssid");
          final int _cursorIndexOfGatewayIp = CursorUtil.getColumnIndexOrThrow(_cursor, "gatewayIp");
          final int _cursorIndexOfReliabilityScore = CursorUtil.getColumnIndexOrThrow(_cursor, "reliabilityScore");
          final int _cursorIndexOfQualityLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "qualityLabel");
          final List<TestResult> _result = new ArrayList<TestResult>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TestResult _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpSsid;
            _tmpSsid = _cursor.getString(_cursorIndexOfSsid);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final double _tmpDownloadMbps;
            _tmpDownloadMbps = _cursor.getDouble(_cursorIndexOfDownloadMbps);
            final double _tmpUploadMbps;
            _tmpUploadMbps = _cursor.getDouble(_cursorIndexOfUploadMbps);
            final long _tmpLatencyMs;
            _tmpLatencyMs = _cursor.getLong(_cursorIndexOfLatencyMs);
            final long _tmpJitterMs;
            _tmpJitterMs = _cursor.getLong(_cursorIndexOfJitterMs);
            final int _tmpPacketLossPercent;
            _tmpPacketLossPercent = _cursor.getInt(_cursorIndexOfPacketLossPercent);
            final int _tmpRssi;
            _tmpRssi = _cursor.getInt(_cursorIndexOfRssi);
            final int _tmpFrequency;
            _tmpFrequency = _cursor.getInt(_cursorIndexOfFrequency);
            final int _tmpLinkSpeed;
            _tmpLinkSpeed = _cursor.getInt(_cursorIndexOfLinkSpeed);
            final String _tmpBssid;
            _tmpBssid = _cursor.getString(_cursorIndexOfBssid);
            final String _tmpGatewayIp;
            _tmpGatewayIp = _cursor.getString(_cursorIndexOfGatewayIp);
            final int _tmpReliabilityScore;
            _tmpReliabilityScore = _cursor.getInt(_cursorIndexOfReliabilityScore);
            final String _tmpQualityLabel;
            _tmpQualityLabel = _cursor.getString(_cursorIndexOfQualityLabel);
            _item = new TestResult(_tmpId,_tmpSsid,_tmpTimestamp,_tmpDownloadMbps,_tmpUploadMbps,_tmpLatencyMs,_tmpJitterMs,_tmpPacketLossPercent,_tmpRssi,_tmpFrequency,_tmpLinkSpeed,_tmpBssid,_tmpGatewayIp,_tmpReliabilityScore,_tmpQualityLabel);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getResultsForSsid(final String ssid, final int limit,
      final Continuation<? super List<TestResult>> $completion) {
    final String _sql = "SELECT * FROM test_results WHERE ssid = ? ORDER BY timestamp DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, ssid);
    _argIndex = 2;
    _statement.bindLong(_argIndex, limit);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<TestResult>>() {
      @Override
      @NonNull
      public List<TestResult> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSsid = CursorUtil.getColumnIndexOrThrow(_cursor, "ssid");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfDownloadMbps = CursorUtil.getColumnIndexOrThrow(_cursor, "downloadMbps");
          final int _cursorIndexOfUploadMbps = CursorUtil.getColumnIndexOrThrow(_cursor, "uploadMbps");
          final int _cursorIndexOfLatencyMs = CursorUtil.getColumnIndexOrThrow(_cursor, "latencyMs");
          final int _cursorIndexOfJitterMs = CursorUtil.getColumnIndexOrThrow(_cursor, "jitterMs");
          final int _cursorIndexOfPacketLossPercent = CursorUtil.getColumnIndexOrThrow(_cursor, "packetLossPercent");
          final int _cursorIndexOfRssi = CursorUtil.getColumnIndexOrThrow(_cursor, "rssi");
          final int _cursorIndexOfFrequency = CursorUtil.getColumnIndexOrThrow(_cursor, "frequency");
          final int _cursorIndexOfLinkSpeed = CursorUtil.getColumnIndexOrThrow(_cursor, "linkSpeed");
          final int _cursorIndexOfBssid = CursorUtil.getColumnIndexOrThrow(_cursor, "bssid");
          final int _cursorIndexOfGatewayIp = CursorUtil.getColumnIndexOrThrow(_cursor, "gatewayIp");
          final int _cursorIndexOfReliabilityScore = CursorUtil.getColumnIndexOrThrow(_cursor, "reliabilityScore");
          final int _cursorIndexOfQualityLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "qualityLabel");
          final List<TestResult> _result = new ArrayList<TestResult>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TestResult _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpSsid;
            _tmpSsid = _cursor.getString(_cursorIndexOfSsid);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final double _tmpDownloadMbps;
            _tmpDownloadMbps = _cursor.getDouble(_cursorIndexOfDownloadMbps);
            final double _tmpUploadMbps;
            _tmpUploadMbps = _cursor.getDouble(_cursorIndexOfUploadMbps);
            final long _tmpLatencyMs;
            _tmpLatencyMs = _cursor.getLong(_cursorIndexOfLatencyMs);
            final long _tmpJitterMs;
            _tmpJitterMs = _cursor.getLong(_cursorIndexOfJitterMs);
            final int _tmpPacketLossPercent;
            _tmpPacketLossPercent = _cursor.getInt(_cursorIndexOfPacketLossPercent);
            final int _tmpRssi;
            _tmpRssi = _cursor.getInt(_cursorIndexOfRssi);
            final int _tmpFrequency;
            _tmpFrequency = _cursor.getInt(_cursorIndexOfFrequency);
            final int _tmpLinkSpeed;
            _tmpLinkSpeed = _cursor.getInt(_cursorIndexOfLinkSpeed);
            final String _tmpBssid;
            _tmpBssid = _cursor.getString(_cursorIndexOfBssid);
            final String _tmpGatewayIp;
            _tmpGatewayIp = _cursor.getString(_cursorIndexOfGatewayIp);
            final int _tmpReliabilityScore;
            _tmpReliabilityScore = _cursor.getInt(_cursorIndexOfReliabilityScore);
            final String _tmpQualityLabel;
            _tmpQualityLabel = _cursor.getString(_cursorIndexOfQualityLabel);
            _item = new TestResult(_tmpId,_tmpSsid,_tmpTimestamp,_tmpDownloadMbps,_tmpUploadMbps,_tmpLatencyMs,_tmpJitterMs,_tmpPacketLossPercent,_tmpRssi,_tmpFrequency,_tmpLinkSpeed,_tmpBssid,_tmpGatewayIp,_tmpReliabilityScore,_tmpQualityLabel);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<VaultEntity>> getVaultNetworks() {
    final String _sql = "SELECT * FROM wifi_vault";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"wifi_vault"}, new Callable<List<VaultEntity>>() {
      @Override
      @NonNull
      public List<VaultEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfSsid = CursorUtil.getColumnIndexOrThrow(_cursor, "ssid");
          final int _cursorIndexOfPassword = CursorUtil.getColumnIndexOrThrow(_cursor, "password");
          final List<VaultEntity> _result = new ArrayList<VaultEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final VaultEntity _item;
            final String _tmpSsid;
            _tmpSsid = _cursor.getString(_cursorIndexOfSsid);
            final String _tmpPassword;
            if (_cursor.isNull(_cursorIndexOfPassword)) {
              _tmpPassword = null;
            } else {
              _tmpPassword = _cursor.getString(_cursorIndexOfPassword);
            }
            _item = new VaultEntity(_tmpSsid,_tmpPassword);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getVaultSnapshot(final Continuation<? super List<VaultEntity>> $completion) {
    final String _sql = "SELECT * FROM wifi_vault";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<VaultEntity>>() {
      @Override
      @NonNull
      public List<VaultEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfSsid = CursorUtil.getColumnIndexOrThrow(_cursor, "ssid");
          final int _cursorIndexOfPassword = CursorUtil.getColumnIndexOrThrow(_cursor, "password");
          final List<VaultEntity> _result = new ArrayList<VaultEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final VaultEntity _item;
            final String _tmpSsid;
            _tmpSsid = _cursor.getString(_cursorIndexOfSsid);
            final String _tmpPassword;
            if (_cursor.isNull(_cursorIndexOfPassword)) {
              _tmpPassword = null;
            } else {
              _tmpPassword = _cursor.getString(_cursorIndexOfPassword);
            }
            _item = new VaultEntity(_tmpSsid,_tmpPassword);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getPasswordFromVault(final String ssid,
      final Continuation<? super String> $completion) {
    final String _sql = "SELECT password FROM wifi_vault WHERE ssid = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, ssid);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<String>() {
      @Override
      @Nullable
      public String call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final String _result;
          if (_cursor.moveToFirst()) {
            if (_cursor.isNull(0)) {
              _result = null;
            } else {
              _result = _cursor.getString(0);
            }
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
