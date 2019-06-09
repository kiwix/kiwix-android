/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.kiwix.kiwixmobile

import android.app.Activity
import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDexApplication
import com.squareup.leakcanary.LeakCanary
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import java.io.File
import java.io.IOException
import javax.inject.Inject
import org.kiwix.kiwixmobile.di.components.ApplicationComponent
import org.kiwix.kiwixmobile.di.components.DaggerApplicationComponent
import com.jakewharton.threetenabp.AndroidThreeTen
import org.kiwix.kiwixmobile.di.modules.ApplicationModule

class KiwixApplication : MultiDexApplication(), HasActivityInjector {

  @Inject
  internal var activityInjector: DispatchingAndroidInjector<Activity>? = null
  private var logFile: File? = null

  /* Checks if external storage is available for read and write */
  private val isExternalStorageWritable: Boolean
    get() {
      val state = Environment.getExternalStorageState()
      return Environment.MEDIA_MOUNTED == state
    }

  override fun attachBaseContext(base: Context) {
    super.attachBaseContext(base)
    instance = this
    applicationComponent = DaggerApplicationComponent.builder()
        .applicationModule(ApplicationModule(this))
        .build()
  }

  override fun onCreate() {
    super.onCreate()
    AndroidThreeTen.init(this)
    if (isExternalStorageWritable) {
      val appDirectory = File(Environment.getExternalStorageDirectory().toString() + "/Kiwix")
      logFile = File(appDirectory, "logcat.txt")
      Log.d("KIWIX", "Writing all logs into [" + logFile!!.path + "]")

      // create app folder
      if (!appDirectory.exists()) {
        appDirectory.mkdir()
      }

      // create log folder
      if (!appDirectory.exists()) {
        appDirectory.mkdir()
      }

      if (logFile!!.exists() && logFile!!.isFile) {
        logFile!!.delete()
      }

      // clear the previous logcat and then write the new one to the file
      try {
        logFile!!.createNewFile()
        var process = Runtime.getRuntime()
            .exec("logcat -c")
        process = Runtime.getRuntime()
            .exec("logcat -f " + logFile!!.path + " -s kiwix")
      } catch (e: IOException) {
        Log.e("KIWIX", "Error while writing logcat.txt", e)
      }

    }

    Log.d("KIWIX", "Started KiwixApplication")

    applicationComponent!!.inject(this)
    if (LeakCanary.isInAnalyzerProcess(this)) {
      // This process is dedicated to LeakCanary for heap analysis.
      // You should not init your app in this process.
      return
    }
    LeakCanary.install(this)
  }

  override fun activityInjector(): AndroidInjector<Activity>? {
    return activityInjector
  }

  companion object {

    var instance: KiwixApplication? = null
      private set
    var applicationComponent: ApplicationComponent? = null

    init {
      AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }
  }
}
