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

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import butterknife.ButterKnife
import butterknife.Unbinder
import dagger.android.AndroidInjection
import org.kiwix.kiwixmobile.core.utils.LanguageUtils
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import javax.inject.Inject

abstract class BaseActivity : AppCompatActivity() {

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

  override fun setContentView(@LayoutRes layoutResID: Int) {
    super.setContentView(layoutResID)
    unbinder = ButterKnife.bind(this)
  }

  // TODO https://issuetracker.google.com/issues/141132133 remove this once appcompat has been fixed
  override fun applyOverrideConfiguration(overrideConfiguration: Configuration?) {
    if (Build.VERSION.SDK_INT in Build.VERSION_CODES.LOLLIPOP..Build.VERSION_CODES.N_MR1 &&
      (resources.configuration.uiMode == applicationContext.resources.configuration.uiMode)
    ) {
      return
    }
    super.applyOverrideConfiguration(overrideConfiguration)
  }

  override fun onDestroy() {
    super.onDestroy()
    unbinder?.unbind()
  }
}
