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

import android.Manifest.permission
import android.app.Activity
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.di.components.DaggerTestComponent

@RunWith(AndroidJUnit4::class)
abstract class BaseActivityTest<T : Activity> {
  @get:Rule
  abstract var activityRule: ActivityTestRule<T>
  @get:Rule
  var readPermissionRule: GrantPermissionRule =
    GrantPermissionRule.grant(permission.READ_EXTERNAL_STORAGE)
  @get:Rule
  var writePermissionRule: GrantPermissionRule =
    GrantPermissionRule.grant(permission.WRITE_EXTERNAL_STORAGE)

  val context: Context by lazy {
    getInstrumentation().targetContext.applicationContext
  }

  inline fun <reified T : Activity> activityTestRule(noinline beforeActivityAction: (() -> Unit)? = null) =
    object : ActivityTestRule<T>(T::class.java) {
      override fun beforeActivityLaunched() {
        super.beforeActivityLaunched()
        beforeActivityAction?.invoke()
      }
    }

  protected fun testComponent() = DaggerTestComponent.builder()
    .context(context)
    .build()
}
