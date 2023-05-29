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
package org.kiwix.kiwixmobile.core.utils

/**
 * Created by mhutti1 on 19/04/17.
 */
object TestingUtils {
  private var callback: IdleListener? = null
  private val resources: MutableSet<Class<*>> = HashSet()
  @JvmStatic fun bindResource(bindClass: Class<*>) {
    callback?.let {
      resources.add(bindClass)
      if (resources.size == 1) {
        it.startTask()
      }
    }
  }

  @JvmStatic fun unbindResource(bindClass: Class<*>) {
    callback?.let {
      resources.remove(bindClass)
      if (resources.isEmpty()) {
        it.finishTask()
      }
    }
  }

  @JvmStatic fun registerIdleCallback(listListener: IdleListener) {
    resources.clear()
    callback = listListener
  }

  interface IdleListener {
    fun startTask()
    fun finishTask()
  }
}
