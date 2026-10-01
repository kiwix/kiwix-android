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

package org.kiwix.kiwixmobile.core.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.kiwix.kiwixmobile.core.ui.theme.DenimBlue200
import org.kiwix.kiwixmobile.core.ui.theme.DenimBlue400
import org.kiwix.kiwixmobile.core.ui.theme.KiwixSnackToastTheme
import org.kiwix.kiwixmobile.core.ui.theme.MineShaftGray850
import org.kiwix.kiwixmobile.core.ui.theme.MineShaftGray900
import org.kiwix.kiwixmobile.core.ui.theme.White
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.EIGHT_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIXTEEN_DP

/**
 * A custom SnackbarHost for displaying snackbars with a pill-shaped appearance and
 * theme-aware colors matching the app's Toast style.
 *
 * @param snackbarHostState The state that controls the Snackbar display.
 */
@Composable
fun KiwixSnackbarHost(snackbarHostState: SnackbarHostState) {
  KiwixSnackToastTheme {
    val isDark = isSystemInDarkTheme()
    val containerColor = if (isDark) MineShaftGray850 else MineShaftGray900
    val contentColor = White
    val actionColor = if (isDark) DenimBlue200 else DenimBlue400

    SnackbarHost(
      hostState = snackbarHostState,
      modifier = Modifier.padding(horizontal = SIXTEEN_DP, vertical = EIGHT_DP)
    ) { snackbarData ->
      Snackbar(
        snackbarData = snackbarData,
        shape = MaterialTheme.shapes.large,
        containerColor = containerColor,
        contentColor = contentColor,
        actionColor = actionColor
      )
    }
  }
}
