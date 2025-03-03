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

package org.kiwix.kiwixmobile.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.LARGE_BODY_TEXT_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.LARGE_HEADLINE_TEXT_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.LARGE_LABEL_TEXT_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.LARGE_TITLE_TEXT_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.MEDIUM_BODY_TEXT_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.MEDIUM_HEADLINE_TEXT_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.MEDIUM_LABEL_TEXT_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.MEDIUM_TITLE_TEXT_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SMALL_BODY_TEXT_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SMALL_HEADLINE_TEXT_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SMALL_LABEL_TEXT_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SMALL_TITLE_TEXT_SIZE

/**
 * Defines the typography styles used throughout the app.
 * These text styles follow Material Design guidelines and ensure consistency in font sizes.
 *
 * @see KiwixTheme
 */
val KiwixTypography = Typography(
  headlineLarge = TextStyle(fontSize = LARGE_HEADLINE_TEXT_SIZE),
  headlineMedium = TextStyle(fontSize = MEDIUM_HEADLINE_TEXT_SIZE),
  headlineSmall = TextStyle(fontSize = SMALL_HEADLINE_TEXT_SIZE),
  titleLarge = TextStyle(fontSize = LARGE_TITLE_TEXT_SIZE),
  titleMedium = TextStyle(fontSize = MEDIUM_TITLE_TEXT_SIZE),
  titleSmall = TextStyle(fontSize = SMALL_TITLE_TEXT_SIZE),
  bodyLarge = TextStyle(fontSize = LARGE_BODY_TEXT_SIZE),
  bodyMedium = TextStyle(fontSize = MEDIUM_BODY_TEXT_SIZE),
  bodySmall = TextStyle(fontSize = SMALL_BODY_TEXT_SIZE),
  labelLarge = TextStyle(
    fontSize = LARGE_LABEL_TEXT_SIZE,
    fontWeight = FontWeight.Bold,
  ),
  labelMedium = TextStyle(fontSize = MEDIUM_LABEL_TEXT_SIZE),
  labelSmall = TextStyle(fontSize = SMALL_LABEL_TEXT_SIZE),
)
