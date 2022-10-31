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

import android.util.Log
import androidx.core.view.GravityCompat
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDialogInteractions
import com.adevinta.android.barista.interaction.BaristaDrawerInteractions.openDrawerWithGravity
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.testutils.TestUtils

/**
 * Created by mhutti1 on 27/04/17.
 */
object StandardActions {
  fun enterSettings() {
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
    clickOn(TestUtils.getResourceString(R.string.menu_settings))
  }

  fun openDrawer() {
    openDrawerWithGravity(R.id.navigation_container, GravityCompat.START)
  }

  @JvmStatic
  fun deleteZimIfExists(zimName: String, adapterId: Int) {
    try {
      Espresso.onData(TestUtils.withContent(zimName)).inAdapterView(
        ViewMatchers.withId(
          adapterId
        )
      ).perform(ViewActions.longClick())
      BaristaDialogInteractions.clickDialogPositiveButton()
      Log.i("TEST_DELETE_ZIM", "Successfully deleted ZIM file [$zimName]")
    } catch (e: RuntimeException) {
      Log.i(
        "TEST_DELETE_ZIM",
        "Failed to delete ZIM file [" + zimName + "]... " +
          "Probably because it doesn't exist"
      )
    }
  }
}
