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

package org.kiwix.kiwixmobile.core.utils

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

object ComposeDimens {
  // Crash checkbox dimens
  val CRASH_CHECKBOX_START_PADDING = 100.dp
  val CRASH_CHECKBOX_TOP_PADDING = 8.dp

  // Our default Button dimens
  val BUTTON_ROUND_CORNER_SHAPE = 5.dp
  val BUTTON_DEFAULT_ELEVATION = 2.dp
  val BUTTON_PRESSED_ELEVATION = 4.dp
  val BUTTON_DEFAULT_PADDING = 4.dp

  // Error screen dimens
  val CRASH_TITLE_TEXT_SIZE = 24.sp
  val CRASH_MESSAGE_TEXT_SIZE = 14.sp
  val CRASH_IMAGE_SIZE = 70.dp

  // Padding & Margins
  val SIXTY_DP = 60.dp
  val THIRTY_TWO_DP = 30.dp
  val SIXTEEN_DP = 16.dp
  val TWELVE_DP = 12.dp
  val EIGHT_DP = 8.dp
  val FIVE_DP = 5.dp
  val FOUR_DP = 4.dp
  val TWO_DP = 2.dp
  val SEVENTY_DP = 70.dp

  // Font Sizes
  val TWENTY_FOUR_SP = 24.sp
  val FOURTEEN_SP = 14.sp

  // Default letter spacing in text according to theme
  val DEFAULT_LETTER_SPACING = 0.0333.em
}
