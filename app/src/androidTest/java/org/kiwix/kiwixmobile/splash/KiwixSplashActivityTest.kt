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

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.intent.Intents
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.DuplicateClickableBoundsCheck
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import kotlinx.coroutines.runBlocking
import leakcanary.LeakAssertions
import org.hamcrest.Matchers.anyOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.utils.TestingUtils.COMPOSE_TEST_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.TestingUtils.RETRY_RULE_ORDER
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.intro.composable.GET_STARTED_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils.closeSystemDialogs
import org.kiwix.kiwixmobile.testutils.TestUtils.isSystemUINotRespondingDialogVisible
import org.kiwix.kiwixmobile.testutils.TestUtils.testFlakyView

@LargeTest
@RunWith(AndroidJUnit4::class)
class KiwixSplashActivityTest {
  @Rule(order = RETRY_RULE_ORDER)
  @JvmField
  val retryRule = RetryRule()

  @get:Rule(order = COMPOSE_TEST_RULE_ORDER)
  val composeTestRule = createComposeRule()

  @Rule
  @JvmField
  val permissionRule: GrantPermissionRule? =
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
      GrantPermissionRule.grant(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
      )
    } else {
      null
    }
  private lateinit var context: Context

  @Before
  fun setUp() {
    Intents.init()
    context = InstrumentationRegistry.getInstrumentation().targetContext
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).apply {
      if (isSystemUINotRespondingDialogVisible(this)) {
        closeSystemDialogs(context, this)
      }
      waitForIdle()
    }
    val accessibilityValidator = AccessibilityValidator().setRunChecksFromRootView(true).apply {
      setSuppressingResultMatcher(
        anyOf(
          matchesCheck(DuplicateClickableBoundsCheck::class.java)
        )
      )
    }
    composeTestRule.enableAccessibilityChecks(accessibilityValidator)
  }

  @Test
  fun testFirstRun() {
    shouldShowIntro(true)

    testFlakyView({
      composeTestRule.waitUntil(timeoutMillis = 5_000) {
        composeTestRule
          .onAllNodesWithTag(GET_STARTED_BUTTON_TESTING_TAG)
          .fetchSemanticsNodes()
          .isNotEmpty()
      }

      composeTestRule
        .onNodeWithTag(GET_STARTED_BUTTON_TESTING_TAG)
        .assertTextEquals(context.getString(R.string.get_started).uppercase())
    }, 10)

    LeakAssertions.assertNoLeaks()
  }

  @Test
  fun testNormalRun() {
    shouldShowIntro(true)
    val scenario = ActivityScenario.launch(KiwixMainActivity::class.java)

    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
        .onAllNodesWithTag(GET_STARTED_BUTTON_TESTING_TAG)
        .fetchSemanticsNodes()
        .isNotEmpty()
    }

    composeTestRule
      .onNodeWithTag(GET_STARTED_BUTTON_TESTING_TAG)
      .performClick()

    scenario.recreate()

    composeTestRule
      .onAllNodesWithTag(GET_STARTED_BUTTON_TESTING_TAG)
      .assertCountEquals(0)

    LeakAssertions.assertNoLeaks()
  }

  @After
  fun endTest() {
    Intents.release()
  }

  private fun shouldShowIntro(value: Boolean) {
    val dataStore = KiwixDataStore(context)
    runBlocking {
      dataStore.setIntroShown(value)
      dataStore.setPrefIsTest(true)
    }
  }
}
