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
import android.os.Build
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import com.jakewharton.threetenabp.AndroidThreeTen
import dagger.hilt.android.EarlyEntryPoints
import org.kiwix.kiwixmobile.core.di.CoreAppEntryPoint
import org.kiwix.kiwixmobile.core.utils.files.FileLogger

@Suppress("UnnecessaryAbstractClass")
abstract class CoreApp : Application() {
  companion object {
    @JvmStatic
    lateinit var instance: CoreApp
  }

  lateinit var themeConfig: ThemeConfig
    private set

  /**
   * The init of this class does the work of initializing,
   * simply injecting it is all that there is to be done
   */
  internal lateinit var jniInitialiser: JNIInitialiser
    private set

  lateinit var fileLogger: FileLogger
    private set

  override fun onCreate() {
    super.onCreate()
    instance = this
    val entryPoint = EarlyEntryPoints.get(this, CoreAppEntryPoint::class.java)
    themeConfig = entryPoint.themeConfig()
    jniInitialiser = entryPoint.jniInitialiser()
    fileLogger = entryPoint.fileLogger()
    AndroidThreeTen.init(this)
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
}
