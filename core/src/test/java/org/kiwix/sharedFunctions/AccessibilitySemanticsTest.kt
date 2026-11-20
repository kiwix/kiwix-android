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

package org.kiwix.sharedFunctions

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class AccessibilitySemanticsTest {
  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun passes_whenDescriptionsAreLabels() {
    composeTestRule.setContent {
      Column {
        Text("Page 1")
        Text("Page 2")
        Text("3.2 MB", Modifier.semantics { contentDescription = "Size" })
        Text("Settings", Modifier.clickable {})
      }
    }
    composeTestRule.assertAccessibilitySemantics()
  }

  @Test
  fun fails_whenDescriptionEndsInHashCode() {
    assertProblem("ends in a hashCode()") {
      Text("Stop", Modifier.semantics { contentDescription = "Stop-867512127" })
    }
  }

  @Test
  fun fails_whenDescriptionIsTextPlusIndex() {
    assertProblem("is the text of the node plus a number") {
      Text("Alpine Linux Wiki", Modifier.semantics { contentDescription = "Alpine Linux Wiki1" })
    }
  }

  @Test
  fun fails_whenDescriptionsDifferOnlyByIndex() {
    assertProblem("plus a number, for example a list index") {
      Column {
        Text("A", Modifier.semantics { contentDescription = "Favicon0" })
        Text("B", Modifier.semantics { contentDescription = "Favicon1" })
      }
    }
  }

  @Test
  fun fails_whenClickableNodeIsHidden() {
    assertProblem("is hidden from accessibility services") {
      Column(Modifier.semantics { hideFromAccessibility() }) {
        Text("Language", Modifier.clickable {})
      }
    }
  }

  private fun assertProblem(problem: String, content: @Composable () -> Unit) {
    composeTestRule.setContent(content)
    val error = assertThrows(AssertionError::class.java) {
      composeTestRule.assertAccessibilitySemantics()
    }
    assertTrue("Expected \"$problem\" in: ${error.message}", error.message.orEmpty().contains(problem))
  }
}
