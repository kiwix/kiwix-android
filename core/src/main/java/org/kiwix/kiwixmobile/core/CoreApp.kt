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
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDex
import com.jakewharton.threetenabp.AndroidThreeTen
import org.kiwix.kiwixmobile.core.data.local.KiwixDatabase
import org.kiwix.kiwixmobile.core.di.components.CoreComponent
import org.kiwix.kiwixmobile.core.di.components.DaggerCoreComponent
import org.kiwix.kiwixmobile.core.downloader.DownloadMonitor
import org.kiwix.kiwixmobile.core.utils.files.FileLogger
import javax.inject.Inject

abstract class CoreApp : Application() {
  companion object {
    @JvmStatic
    lateinit var instance: CoreApp

    @JvmStatic
    lateinit var coreComponent: CoreComponent

    init {
      AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }
  }

  @Inject
  lateinit var downloadMonitor: DownloadMonitor

  @Inject
  lateinit var nightModeConfig: NightModeConfig

  @Inject
  lateinit var kiwixDatabase: KiwixDatabase

  /**
   * The init of this class does the work of initializing,
   * simply injecting it is all that there is to be done
   */
  @Inject
  internal lateinit var jniInitialiser: JNIInitialiser

  @Inject
  lateinit var fileLogger: FileLogger

  override fun attachBaseContext(base: Context) {
    super.attachBaseContext(base)
    if (BuildConfig.DEBUG) {
      MultiDex.install(this)
    }
  }

  override fun onCreate() {
    super.onCreate()
    instance = this
    fileLogger = FileLogger()
    coreComponent = DaggerCoreComponent.builder()
      .context(this)
      .build()
    AndroidThreeTen.init(this)
    fileLogger.writeLogFile(this)
    coreComponent.inject(this)
    kiwixDatabase.forceMigration()
    downloadMonitor.init()
    nightModeConfig.init()
    configureStrictMode()
  }

  private fun configureStrictMode() {
    if (BuildConfig.DEBUG) {
      StrictMode.setThreadPolicy(
        StrictMode.ThreadPolicy.Builder().apply {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            detectResourceMismatches()
          }
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
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            detectCleartextNetwork()
          }
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            detectContentUriWithoutPermission()
          }
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            detectFileUriExposure()
          }
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            detectNonSdkApiUsage()
          }
          detectActivityLeaks()
          detectLeakedClosableObjects()
          detectLeakedSqlLiteObjects()
          penaltyLog()
          detectLeakedRegistrationObjects()
        }.build()
      )
    }
  }
}
