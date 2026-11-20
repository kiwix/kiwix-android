/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.custom.main

import android.os.Build
import android.os.Bundle
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.search.NAV_ARG_SEARCH_STRING
import org.kiwix.kiwixmobile.core.utils.EXTRA_IS_WIDGET_VOICE
import org.kiwix.kiwixmobile.core.utils.TAG_FROM_TAB_SWITCHER
import org.kiwix.sharedFunctions.TestApplication
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R], application = TestApplication::class)
class BrandedSearchRouteTest {
  @get:Rule
  val composeTestRule = createComposeRule()

  private lateinit var navController: NavHostController
  private var searchArguments: Bundle? = null

  @Before
  fun setUp() {
    composeTestRule.setContent {
      navController = rememberNavController()
      NavHost(navController = navController, startDestination = "start") {
        composable("start") {}
        composable(
          route = CustomDestination.Search.route,
          arguments = listOf(
            navArgument(NAV_ARG_SEARCH_STRING) {
              type = NavType.StringType
              defaultValue = ""
            },
            navArgument(TAG_FROM_TAB_SWITCHER) {
              type = NavType.BoolType
              defaultValue = false
            },
            navArgument(EXTRA_IS_WIDGET_VOICE) {
              type = NavType.BoolType
              defaultValue = false
            }
          )
        ) { searchArguments = it.arguments }
      }
    }
  }

  @Test
  fun searchStringWithSpecialCharactersIsPassedUnchanged() {
    listOf(
      "plain",
      "two words",
      "line\nbreak",
      "what?",
      "this & that",
      "a=b",
      "100%",
      "#hashtag",
      "path/to/article",
      "emoji 😀",
      "?&=#%/ \n😀"
    ).forEach { query ->
      navigateToSearch(query, isOpenedFromTabView = true, isVoice = true)
      assertEquals(query, searchArguments?.getString(NAV_ARG_SEARCH_STRING))
      assertTrue(searchArguments?.getBoolean(TAG_FROM_TAB_SWITCHER) == true)
      assertTrue(searchArguments?.getBoolean(EXTRA_IS_WIDGET_VOICE) == true)
    }
  }

  private fun navigateToSearch(query: String, isOpenedFromTabView: Boolean, isVoice: Boolean) {
    composeTestRule.runOnIdle {
      navController.navigate(
        CustomDestination.Search.createRoute(query, isOpenedFromTabView, isVoice)
      )
    }
    composeTestRule.waitForIdle()
  }
}
