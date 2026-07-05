package org.kiwix.kiwixmobile.core.utils.workManager

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import javax.inject.Inject

/*To avoid dropping of worker jobs after the constraints are not met and app is updated,
 This WorkerFactory will handle renamed workers.When workerClassName content matches the OldWorker,
  we return an instance of NewWorker instead. It's important to return "null" for non-handled cases,
  as this lets the default WorkerFactory take over.*/
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
