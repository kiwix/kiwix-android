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
package org.kiwix.kiwixmobile.utils

import androidx.test.espresso.IdlingResource
import androidx.test.espresso.IdlingResource.ResourceCallback
import org.kiwix.kiwixmobile.core.utils.TestingUtils
import org.kiwix.kiwixmobile.core.utils.TestingUtils.IdleListener

/**
 * Created by mhutti1 on 19/04/17.
 */

class KiwixIdlingResource : IdlingResource, IdleListener {
  private var idle = true
  private var resourceCallback: ResourceCallback? = null

  override fun getName(): String = "Standard Kiwix Idling Resource"

  override fun isIdleNow(): Boolean = idle

  override fun registerIdleTransitionCallback(callback: ResourceCallback) {
    resourceCallback = callback
  }

  override fun startTask() {
    idle = false
  }

  override fun finishTask() {
    idle = true
    if (resourceCallback != null) {
      resourceCallback?.onTransitionToIdle()
    }
  }

  companion object {
    private var kiwixIdlingResource: KiwixIdlingResource? = null

    @JvmStatic
    fun getInstance(): KiwixIdlingResource? {
      if (kiwixIdlingResource == null) {
        kiwixIdlingResource = KiwixIdlingResource()
      }
      kiwixIdlingResource!!.idle = true
      TestingUtils.registerIdleCallback(kiwixIdlingResource)
      return kiwixIdlingResource
    }
  }
}
