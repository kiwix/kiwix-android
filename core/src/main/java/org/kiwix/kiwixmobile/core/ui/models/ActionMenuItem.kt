/*
 * Kiwix Android
 * Copyright (c) 2025 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.ui.models

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

data class ActionMenuItem(
  val icon: IconItem? = null,
  @StringRes val contentDescription: Int,
  val onClick: () -> Unit,
  val isEnabled: Boolean = true,
  val iconButtonText: String = "",
  val testingTag: String,
  val modifier: Modifier = Modifier,
  val isInOverflow: Boolean = false,
  val customView: (@Composable () -> Unit)? = null
)
