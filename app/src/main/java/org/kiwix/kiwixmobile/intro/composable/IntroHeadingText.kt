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

package org.kiwix.kiwixmobile.intro.composable

import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import org.kiwix.kiwixmobile.core.ui.theme.dimHighlightedTextDark
import org.kiwix.kiwixmobile.core.ui.theme.dimHighlightedTextLight
import org.kiwix.kiwixmobile.core.utils.ComposeDimens

@Composable
fun HeadingText(
  @StringRes text: Int
) {
  val isSystemThemeDark = isSystemInDarkTheme()
  val color = if (isSystemThemeDark) {
    dimHighlightedTextDark
  } else {
    dimHighlightedTextLight
  }
  Text(
    modifier = Modifier
      .padding(ComposeDimens.SIXTEEN_DP),
    text = stringResource(text),
    fontWeight = FontWeight.Bold,
    fontSize = ComposeDimens.SMALL_HEADLINE_TEXT_SIZE,
    color = color
  )
}

@Composable
fun SubHeadingText(
  @StringRes text: Int
) {
  Text(
    modifier = Modifier
      .fillMaxWidth()
      .height(ComposeDimens.FIFTY_DP),
    textAlign = TextAlign.Center,
    text = stringResource(text),
    fontSize = ComposeDimens.MEDIUM_BODY_TEXT_SIZE,
    color = MaterialTheme.colorScheme.onTertiary
  )
}
