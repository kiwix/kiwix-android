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

import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.di.components.KiwixActivityComponent
import org.kiwix.kiwixmobile.di.components.KiwixComponent
import org.kiwix.kiwixmobile.main.KiwixMainActivity

private val BaseActivity.kiwixComponent: KiwixComponent
  get() = kiwixApp()?.kiwixComponent ?: throw RuntimeException(
    """
        applicationContext is ${applicationContext::class.java.simpleName}
        application is ${application::class.java.simpleName} 
    """.trimIndent()
  )

private fun BaseActivity.kiwixApp() = applicationContext as? KiwixApp ?: application as? KiwixApp

val BaseActivity.cachedComponent: KiwixActivityComponent
  get() = (this as KiwixMainActivity).cachedComponent

internal inline val BaseActivity.kiwixActivityComponent
  get() = kiwixComponent
    .activityComponentBuilder()
    .activity(this)
    .build()
