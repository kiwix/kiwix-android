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

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavDirections
import androidx.navigation.findNavController
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.custom.di.CustomComponent

private val BaseActivity.customComponent: CustomComponent
  get() = customApp()?.customComponent ?: throw RuntimeException(
    """
        applicationContext is ${applicationContext::class.java.simpleName}
        application is ${application::class.java.simpleName} 
    """.trimIndent()
  )

private fun BaseActivity.customApp() = applicationContext as? CustomApp ?: application as? CustomApp

internal inline val BaseActivity.customActivityComponent
  get() = customComponent.activityComponentBuilder().activity(this).build()

fun AppCompatActivity.navigate(action: NavDirections) =
  findNavController(R.id.custom_nav_controller).navigate(action)

fun AppCompatActivity.navigate(@IdRes fragmentId: Int) =
  findNavController(R.id.custom_nav_controller).navigate(fragmentId)

fun AppCompatActivity.navigate(@IdRes fragmentId: Int, bundle: Bundle) =
  findNavController(R.id.custom_nav_controller).navigate(fragmentId, bundle)

fun <T : View> Activity.lazyView(@IdRes viewId: Int): Lazy<T> = lazy { findViewById<T>(viewId) }
