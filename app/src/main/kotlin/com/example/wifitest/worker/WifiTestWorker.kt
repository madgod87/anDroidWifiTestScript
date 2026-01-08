package com.example.wifitest.worker

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.wifitest.service.WifiTestService

class WifiTestWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val intent = Intent(applicationContext, WifiTestService::class.java)
        ContextCompat.startForegroundService(applicationContext, intent)
        return Result.success()
    }
}
