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
package org.kiwix.kiwixmobile.core.base

import android.content.res.Resources
import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import butterknife.ButterKnife
import butterknife.Unbinder
import dagger.android.AndroidInjection
import org.kiwix.kiwixmobile.core.KiwixApplication
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.di.components.ActivityComponent
import org.kiwix.kiwixmobile.core.utils.LanguageUtils
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import javax.inject.Inject

abstract class BaseActivity : AppCompatActivity() {

  val activityComponent: ActivityComponent by lazy(
    KiwixApplication.getApplicationComponent()
      .activityComponent()
      .activity(this)
    ::build
  )

  @Inject
  lateinit var sharedPreferenceUtil: SharedPreferenceUtil

  private var unbinder: Unbinder? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    injection()
    super.onCreate(savedInstanceState)
    LanguageUtils.handleLocaleChange(this, sharedPreferenceUtil)
  }

  protected open fun injection() {
    AndroidInjection.inject(this)
  }

  override fun getTheme(): Resources.Theme {
    val theme = super.getTheme()
    if (sharedPreferenceUtil.nightMode()) {
      setTheme(R.style.AppTheme_Night)
    } else {
      theme.applyStyle(R.style.StatusBarTheme, true)
    }
    return theme
  }

  override fun setContentView(@LayoutRes layoutResID: Int) {
    super.setContentView(layoutResID)
    unbinder = ButterKnife.bind(this)
  }

  override fun onDestroy() {
    super.onDestroy()
    unbinder?.unbind()
  }
}
