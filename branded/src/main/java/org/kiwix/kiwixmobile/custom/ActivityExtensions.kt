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

package org.kiwix.kiwixmobile.custom

import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.custom.di.BrandedComponent

private val BaseActivity.brandedComponent: BrandedComponent
  get() = brandedApp()?.brandedComponent ?: throw RuntimeException(
    """
        applicationContext is ${applicationContext::class.java.simpleName}
        application is ${application::class.java.simpleName} 
    """.trimIndent()
  )

private fun BaseActivity.brandedApp() =
  applicationContext as? BrandedApp ?: application as? BrandedApp

internal inline val BaseActivity.brandedActivityComponent
  get() = brandedComponent.activityComponentBuilder().activity(this).build()
