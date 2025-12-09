/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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
 *
 */
package org.kiwix.kiwixmobile.core

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Environment.MEDIA_MOUNTED
import android.os.Environment.getExternalStorageState
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import androidx.multidex.MultiDex
import com.jakewharton.threetenabp.AndroidThreeTen
import org.kiwix.kiwixmobile.core.di.components.CoreComponent
import org.kiwix.kiwixmobile.core.di.components.DaggerCoreComponent
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.utils.files.FileLogger
import org.kiwix.kiwixmobile.core.utils.workManager.UpdateWorkManager
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
abstract class CoreApp : Application() {
  companion object {
    @JvmStatic
    lateinit var instance: CoreApp

    @JvmStatic
    lateinit var coreComponent: CoreComponent
  }

  @Inject
  lateinit var themeConfig: ThemeConfig

  /**
   * The init of this class does the work of initializing,
   * simply injecting it is all that there is to be done
   */
  @Inject
  internal lateinit var jniInitialiser: JNIInitialiser

  @Inject
  lateinit var fileLogger: FileLogger

  /**
   * The init of this class does the work of initializing,
   * simply injecting it is all that there is to be done
   */
  @Inject
  lateinit var serviceWorkerInitialiser: ServiceWorkerInitialiser

  private lateinit var coreMainActivity: CoreMainActivity

  override fun attachBaseContext(base: Context) {
    super.attachBaseContext(base)
    if (BuildConfig.DEBUG) {
      MultiDex.install(this)
    }
  }

  override fun onCreate() {
    super.onCreate()
    UpdateWorkManager.startPeriodicWork(this)
    instance = this
    coreComponent = DaggerCoreComponent.builder()
      .context(this)
      .build()
    AndroidThreeTen.init(this)
    coreComponent.inject(this)
    serviceWorkerInitialiser.init(this)
    themeConfig.init()
    fileLogger.writeLogFile(this)
    configureStrictMode()
  }

  private fun configureStrictMode() {
    if (BuildConfig.DEBUG) {
      StrictMode.setThreadPolicy(
        StrictMode.ThreadPolicy.Builder().apply {
          detectResourceMismatches()
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            detectUnbufferedIo()
          }
          detectCustomSlowCalls()
          detectDiskReads()
          detectDiskWrites()
          detectNetwork()
          penaltyFlashScreen()
          penaltyLog()
        }.build()
      )
      StrictMode.setVmPolicy(
        VmPolicy.Builder().apply {
          detectCleartextNetwork()
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            detectContentUriWithoutPermission()
          }
          detectFileUriExposure()
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            detectNonSdkApiUsage()
          }
          detectActivityLeaks()
          detectLeakedClosableObjects()
          detectLeakedSqlLiteObjects()
          penaltyLog()
          detectLeakedRegistrationObjects()
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            detectUnsafeIntentLaunch()
          }
        }.build()
      )
    }
  }

  fun setMainActivity(coreMainActivity: CoreMainActivity) {
    this.coreMainActivity = coreMainActivity
  }

  fun getMainActivity() = coreMainActivity

  // Checks if external storage is available for read and write
  val isExternalStorageWritable: Boolean
    get() = MEDIA_MOUNTED == getExternalStorageState()
}
