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

package org.kiwix.kiwixmobile.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher

/**
 * Matches a node whose content description ends in a digit, for example "3.2 MB1" or
 * "Stop-867512127". TalkBack reads the content description, so a list index or a
 * `hashCode()` must go in a test tag, not in the content description.
 */
fun hasContentDescriptionEndingInDigit(): SemanticsMatcher =
  SemanticsMatcher("content description ends in a digit") { node ->
    node.config.getOrNull(SemanticsProperties.ContentDescription)
      .orEmpty()
      .any { it.lastOrNull()?.isDigit() == true }
  }
