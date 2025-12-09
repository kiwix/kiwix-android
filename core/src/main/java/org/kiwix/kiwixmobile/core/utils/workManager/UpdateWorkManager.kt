package org.kiwix.kiwixmobile.core.utils.workManager

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
import okhttp3.logging.HttpLoggingInterceptor.Level.NONE
import org.kiwix.kiwixmobile.core.BuildConfig
import org.kiwix.kiwixmobile.core.data.remote.KiwixService
import org.kiwix.kiwixmobile.core.data.remote.UserAgentInterceptor
import org.kiwix.kiwixmobile.core.di.modules.CALL_TIMEOUT
import org.kiwix.kiwixmobile.core.di.modules.CONNECTION_TIMEOUT
import org.kiwix.kiwixmobile.core.di.modules.KIWIX_UPDATE_URL
import org.kiwix.kiwixmobile.core.di.modules.READ_TIMEOUT
import org.kiwix.kiwixmobile.core.di.modules.USER_AGENT
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.SECONDS

@Suppress()
class UpdateWorkManager @AssistedInject constructor(
  @Assisted appContext: Context,
  @Assisted workerParams: WorkerParameters,
  private var kiwixService: KiwixService
) : CoroutineWorker(appContext, workerParams) {
  override suspend fun doWork(): Result {
    kiwixService =
      KiwixService.ServiceCreator.newHackListService(
        okHttpClient = getOkHttpClient(),
        KIWIX_UPDATE_URL
      )
    return Result.success()
  }

  @Suppress()
  companion object {
    fun getOkHttpClient() = OkHttpClient().newBuilder()
      .followRedirects(true)
      .followSslRedirects(true)
      .connectTimeout(CONNECTION_TIMEOUT, SECONDS)
      .readTimeout(READ_TIMEOUT, SECONDS)
      .callTimeout(CALL_TIMEOUT, SECONDS)
      .addNetworkInterceptor(
        HttpLoggingInterceptor().apply {
          level = if (BuildConfig.DEBUG) BASIC else NONE
        }
      )
      .addNetworkInterceptor(UserAgentInterceptor(USER_AGENT))
      .build()

    private const val INTERVAL = 15L
    private const val INITIAL_DELAY = 0L
    fun startPeriodicWork(appContext: Context?) {
      /*val constraints = Constraints.Builder()
          .setRequiredNetworkType(NetworkType.CONNECTED)
          .build()*/

      val workRequest: PeriodicWorkRequest =
        PeriodicWorkRequestBuilder<UpdateWorkManager>(INTERVAL, TimeUnit.MINUTES)
          .setInitialDelay(INITIAL_DELAY, TimeUnit.MINUTES)
          .build()
      appContext?.let {
        WorkManager.getInstance(it)
          .enqueueUniquePeriodicWork(
            "update",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
          )
      }
    }
  }
}
