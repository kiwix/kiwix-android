package org.kiwix.kiwixmobile.core.utils.workManager

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class UpdateWorkManager(appContext: Context, workerParams: WorkerParameters) :
  CoroutineWorker(appContext, workerParams) {
  override suspend fun doWork(): Result {
    Log.d("TAG", "doWork: executed")
    return Result.success()
  }

  companion object {
    fun startPeriodicWork(appContext: Context?) {
      /*val constraints = Constraints.Builder()
          .setRequiredNetworkType(NetworkType.CONNECTED)
          .build()*/

      val workRequest: PeriodicWorkRequest =
        PeriodicWorkRequestBuilder<UpdateWorkManager>(15, TimeUnit.MINUTES)
          .setInitialDelay(0, TimeUnit.MINUTES)
          // .setConstraints(constraints)
          .build()
      appContext?.let {
        WorkManager.getInstance(it)
          .enqueue(workRequest)
        Log.d("mWorkConfig", "Periodic Worker enqueued...")
      }
    }
  }
}
