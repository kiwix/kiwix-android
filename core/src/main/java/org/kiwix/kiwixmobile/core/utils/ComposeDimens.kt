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
  val CRASH_CHECKBOX_START_PADDING = 80.dp
  val CRASH_CHECKBOX_TOP_PADDING = 8.dp

  // Our default Button dimens
  val BUTTON_DEFAULT_ELEVATION = 2.dp
  val BUTTON_PRESSED_ELEVATION = 4.dp
  val BUTTON_DEFAULT_PADDING = 4.dp

  // Error screen dimens
  val CRASH_IMAGE_SIZE = 70.dp

  // KiwixAppBar(Toolbar) height
  val KIWIX_APP_BAR_HEIGHT = 56.dp

  // Padding & Margins
  val SIXTY_DP = 60.dp
  val THIRTY_TWO_DP = 30.dp
  val TWENTY_DP = 20.dp
  val SEVENTEEN_DP = 17.dp
  val SIXTEEN_DP = 16.dp
  val TWELVE_DP = 12.dp
  val TEN_DP = 10.dp
  val EIGHT_DP = 8.dp
  val SIX_DP = 6.dp
  val FIVE_DP = 5.dp
  val FOUR_DP = 4.dp
  val TWO_DP = 2.dp
  val SEVENTY_DP = 70.dp

  // Font Sizes
  val TWENTY_FOUR_SP = 24.sp
  val FOURTEEN_SP = 14.sp

  // Default letter spacing in text according to theme
  val DEFAULT_LETTER_SPACING = 0.0333.em

  // Shape configuration sizes. See Shape.kt
  val EXTRA_SMALL_ROUND_SHAPE_SIZE = 4.dp
  val SMALL_ROUND_SHAPE_SIZE = 8.dp
  val MEDIUM_ROUND_SHAPE_SIZE = 16.dp
  val LARGE_ROUND_SHAPE_SIZE = 24.dp
  val EXTRA_LARGE_ROUND_SHAPE_SIZE = 32.dp

  // Typography configuration sizes. See Typography.kt
  val LARGE_HEADLINE_TEXT_SIZE = 32.sp
  val MEDIUM_HEADLINE_TEXT_SIZE = 28.sp
  val SMALL_HEADLINE_TEXT_SIZE = 24.sp
  val LARGE_TITLE_TEXT_SIZE = 22.sp
  val MEDIUM_TITLE_TEXT_SIZE = 20.sp
  val SMALL_TITLE_TEXT_SIZE = 16.sp
  val LARGE_BODY_TEXT_SIZE = 16.sp
  val MEDIUM_BODY_TEXT_SIZE = 14.sp
  val SMALL_BODY_TEXT_SIZE = 12.sp
  val LARGE_LABEL_TEXT_SIZE = 14.sp
  val MEDIUM_LABEL_TEXT_SIZE = 12.sp
  val SMALL_LABEL_TEXT_SIZE = 10.sp
  val MEDIUM_BODY_LETTER_SPACING = 0.00714285714.em

  // AddNoteDialog dimens
  val MINIMUM_HEIGHT_OF_NOTE_TEXT_FILED = 120.dp

  // Material minimum width and height
  val MATERIAL_MINIMUM_HEIGHT_AND_WIDTH = 48.dp

  // ZimHostFragment dimens
  val MINIMUM_HEIGHT_OF_QR_CODE = 76.dp
  val MAXIMUM_HEIGHT_OF_QR_CODE = 128.dp
  val MINIMUM_HEIGHT_OF_BOOKS_LIST = 256.dp

  // BookItem dimens
  val BOOK_ICON_SIZE = 40.dp

  // LocalLibraryFragment dimens
  val FAB_ICON_BOTTOM_MARGIN = 50.dp
}
