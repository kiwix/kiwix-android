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

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.core.view.WindowCompat
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.MEDIUM_BODY_LETTER_SPACING
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.MEDIUM_BODY_TEXT_SIZE

private val DarkColorScheme = darkColorScheme(
  primary = DenimBlue200,
  secondary = DenimBlue200,
  background = MineShaftGray900,
  surface = MineShaftGray900,
  error = MonzaRed,
  onPrimary = Black,
  onSecondary = MineShaftGray350,
  onBackground = White,
  onSurface = White,
  onError = White,
  onTertiary = MineShaftGray500,
  surfaceContainer = MineShaftGray850,
)

private val LightColorScheme = lightColorScheme(
  primary = DenimBlue800,
  secondary = DenimBlue800,
  background = AlabasterWhite,
  surface = AlabasterWhite,
  error = MonzaRed,
  onPrimary = White,
  onSecondary = ScorpionGray,
  onBackground = Black,
  onSurface = Black,
  onError = AlabasterWhite,
  onTertiary = MineShaftGray600,
  surfaceContainer = AlabasterWhite,
)

@Composable
fun KiwixTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit
) {
  val view = LocalView.current
  val colorScheme = when {
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  // Set the systemBarColor according to our theme.
  if (!view.isInEditMode) {
    val windowBackGroundColor = colorScheme.onPrimary.toArgb()
    SideEffect {
      val window = (view.context as Activity).window
      val insetsController = WindowCompat.getInsetsController(window, view)
      window?.decorView?.setBackgroundColor(windowBackGroundColor)
      insetsController.isAppearanceLightStatusBars = !darkTheme
      insetsController.isAppearanceLightNavigationBars = !darkTheme
    }
  }

  MaterialTheme(
    colorScheme = colorScheme,
    content = content,
    shapes = shapes,
    typography = KiwixTypography
  )
}

/**
 * A custom MaterialTheme specifically for dialogs in the Kiwix app.
 *
 * This theme applies a modified dark mode background for dialogs while keeping
 * the rest of the color scheme unchanged. In light mode, it uses the
 * standard app theme(KiwixTheme).
 */
@Composable
fun KiwixDialogTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit
) {
  val colorScheme = when {
    darkTheme -> DarkColorScheme.copy(onSurfaceVariant = MineShaftGray350)
    else -> LightColorScheme.copy(onSurfaceVariant = ScorpionGray)
  }
  MaterialTheme(
    colorScheme = colorScheme,
    content = content,
    shapes = shapes,
    typography = KiwixTypography
  )
}

/**
 * A custom theme specifically designed for displaying short-lived UI messages,
 * such as Snackbars, and Toasts.
 *
 * This theme overrides the default `bodyMedium` typography to remove bold styling,
 * ensuring that Snackbar and Toast messages appear in a normal-weight font.
 *
 * @param darkTheme Determines whether the theme should use dark mode colors.
 * Defaults to the system's dark mode setting.
 * @param content The composable content that will be wrapped with this theme.
 */
@Composable
fun KiwixSnackToastTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit
) {
  val colorScheme = when {
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }
  val snackBarTypography = KiwixTypography.copy(
    bodyMedium = TextStyle(
      fontSize = MEDIUM_BODY_TEXT_SIZE,
      letterSpacing = MEDIUM_BODY_LETTER_SPACING
    )
  )
  MaterialTheme(
    colorScheme = colorScheme,
    content = content,
    shapes = shapes,
    typography = snackBarTypography
  )
}
