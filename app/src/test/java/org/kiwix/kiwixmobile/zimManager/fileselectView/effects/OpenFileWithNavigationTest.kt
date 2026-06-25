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

package org.kiwix.kiwixmobile.zimManager.fileselectView.effects

import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavOptions
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.main.ZIM_FILE_URI_KEY
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.ui.KiwixDestination
import org.kiwix.sharedFunctions.MainDispatcherRule

class OpenFileWithNavigationTest {
  @RegisterExtension
  @JvmField
  val mainDispatcherRule = MainDispatcherRule()
  private val scope = TestScope(mainDispatcherRule.dispatcher)

  private val zimReaderSource = mockk<ZimReaderSource>()

  @AfterEach
  fun tearDown() {
    unmockkAll()
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `invokeWith should show error toast when file cannot open`() = runTest {
    val activity = mockk<AppCompatActivity>(relaxed = true)

    coEvery { zimReaderSource.canOpenInLibkiwix() } returns false
    every { zimReaderSource.toDatabase() } returns "test.zim"

    mockkStatic("org.kiwix.kiwixmobile.core.extensions.ContextExtensionsKt")

    every {
      activity.toast(any<String>())
    } returns Unit

    val effect = OpenFileWithNavigation(
      zimReaderSource,
      scope,
      mainDispatcherRule.dispatcher
    )

    effect.invokeWith(activity)

    advanceUntilIdle()

    verify {
      activity.toast(any<String>())
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `invokeWith should navigate to reader when file can open`() = runTest {
    val activity = mockk<CoreMainActivity>(relaxed = true)

    coEvery { zimReaderSource.canOpenInLibkiwix() } returns true
    every { zimReaderSource.toDatabase() } returns "test.zim"

    mockkObject(ActivityExtensions)

    every {
      ActivityExtensions.run {
        activity.setNavigationResultOnCurrent(any<String>(), any())
      }
    } returns Unit

    val effect = OpenFileWithNavigation(
      zimReaderSource,
      scope,
      mainDispatcherRule.dispatcher
    )

    effect.invokeWith(activity)

    advanceUntilIdle()

    verify {
      activity.navigate(
        KiwixDestination.Reader.route,
        any<NavOptions>()
      )
    }

    verify {
      ActivityExtensions.run {
        activity.setNavigationResultOnCurrent(
          "test.zim",
          ZIM_FILE_URI_KEY
        )
      }
    }
  }
}
