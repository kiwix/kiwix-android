/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
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
 */
package org.kiwix.kiwixmobile.intro

import android.os.Build
import androidx.test.filters.SdkSuppress
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN_MR2)
class IntroActivityTest : BaseActivityTest<IntroActivity>() {

  override var activityRule = activityTestRule<IntroActivity>()

  @Test
  fun viewIsSwipeableAndNavigatesToMain() {
    intro {
      swipeLeft()
      swipeRight()
    } clickGetStarted { }
  }
}
