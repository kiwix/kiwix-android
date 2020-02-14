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
package org.kiwix.kiwixmobile.core.splash

import android.content.Intent
import android.os.Bundle
import android.os.Process
import org.kiwix.kiwixmobile.core.BuildConfig
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.error.ErrorActivity
import kotlin.system.exitProcess

abstract class CoreSplashActivity : BaseActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (!BuildConfig.DEBUG) {
      val appContext = applicationContext
      Thread.setDefaultUncaughtExceptionHandler { paramThread: Thread?,
        paramThrowable: Throwable? ->
        val intent = Intent(appContext, ErrorActivity::class.java)
        val extras = Bundle()
        extras.putSerializable("exception", paramThrowable)
        intent.putExtras(extras)
        appContext.startActivity(intent)
        finish()
        Process.killProcess(Process.myPid())
        exitProcess(10)
      }
    }
    startActivity(intentForNextScreen)
    finish()
  }

  protected abstract val intentForNextScreen: Intent
}
