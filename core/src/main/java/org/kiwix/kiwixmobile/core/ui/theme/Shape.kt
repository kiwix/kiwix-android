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

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.EXTRA_LARGE_ROUND_SHAPE_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.EXTRA_SMALL_ROUND_SHAPE_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.LARGE_ROUND_SHAPE_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.MEDIUM_ROUND_SHAPE_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SMALL_ROUND_SHAPE_SIZE

/**
 * Defines the shape styles used throughout the app, primarily with rounded corners.
 * These shapes are applied consistently via the Material theme to ensure uniformity.
 *
 * @see KiwixTheme
 */
val shapes = Shapes(
  extraSmall = RoundedCornerShape(EXTRA_SMALL_ROUND_SHAPE_SIZE),
  small = RoundedCornerShape(SMALL_ROUND_SHAPE_SIZE),
  medium = RoundedCornerShape(MEDIUM_ROUND_SHAPE_SIZE),
  large = RoundedCornerShape(LARGE_ROUND_SHAPE_SIZE),
  extraLarge = RoundedCornerShape(EXTRA_LARGE_ROUND_SHAPE_SIZE)
)
