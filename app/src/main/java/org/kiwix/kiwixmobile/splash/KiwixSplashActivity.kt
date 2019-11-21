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

package org.kiwix.kiwixmobile.splash

import android.content.Intent
import org.kiwix.kiwixmobile.core.splash.CoreSplashActivity
import org.kiwix.kiwixmobile.intro.IntroActivity
import org.kiwix.kiwixmobile.kiwixActivityComponent
import org.kiwix.kiwixmobile.main.KiwixMainActivity

class KiwixSplashActivity : CoreSplashActivity() {

  override fun injection() {
    kiwixActivityComponent.inject(this)
  }

  override val intentForNextScreen: Intent
    get() = if (sharedPreferenceUtil.showIntro())
      Intent(this, IntroActivity::class.java)
    else
      Intent(this, KiwixMainActivity::class.java)
}
