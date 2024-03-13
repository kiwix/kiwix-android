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

package org.kiwix.kiwixmobile

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.di.components.DaggerTestComponent
import org.kiwix.kiwixmobile.core.di.components.TestComponent
import org.kiwix.kiwixmobile.main.KiwixMainActivity

@RunWith(AndroidJUnit4::class)
abstract class BaseActivityTest {
  open lateinit var activityScenario: ActivityScenario<KiwixMainActivity>

  private val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    arrayOf(
      Manifest.permission.POST_NOTIFICATIONS,
      Manifest.permission.READ_EXTERNAL_STORAGE,
      Manifest.permission.WRITE_EXTERNAL_STORAGE,
      Manifest.permission.NEARBY_WIFI_DEVICES,
      Manifest.permission.ACCESS_NETWORK_STATE
    )
  } else {
    arrayOf(
      Manifest.permission.READ_EXTERNAL_STORAGE,
      Manifest.permission.WRITE_EXTERNAL_STORAGE,
      Manifest.permission.ACCESS_FINE_LOCATION,
      Manifest.permission.ACCESS_NETWORK_STATE
    )
  }

  @get:Rule
  var permissionRules: GrantPermissionRule =
    GrantPermissionRule.grant(*permissions)

  val context: Context by lazy {
    getInstrumentation().targetContext.applicationContext
  }

  protected fun testComponent(): TestComponent = DaggerTestComponent.builder()
    .context(context)
    .build()

  abstract fun waitForIdle()
}
