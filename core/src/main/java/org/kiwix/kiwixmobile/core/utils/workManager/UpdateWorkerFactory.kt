package org.kiwix.kiwixmobile.core.utils.workManager

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import javax.inject.Inject

class UpdateWorkerFactory @Inject constructor(
  private val updateWorkerFactory: UpdateWorkManager.Factory,
) : WorkerFactory() {
  override fun createWorker(
    appContext: Context,
    workerClassName: String,
    workerParameters: WorkerParameters,
  ): ListenableWorker? {
    return when (workerClassName) {
      UpdateWorkManager::class.java.name ->
        updateWorkerFactory.create(appContext, workerParameters)

      else -> null
    }
  }
}
