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

package org.kiwix.kiwixmobile.core.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.ui.theme.KiwixTheme
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.EIGHT_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.KIWIX_TOOLBAR_SHADOW_ELEVATION

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindInPageAppBar(
  query: String,
  resultText: String,
  onQueryChange: (String) -> Unit,
  onNextClick: () -> Unit,
  onPreviousClick: () -> Unit,
  onCloseClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  KiwixTheme {
    TopAppBar(
      title = {
        FindInPageBar(
          query = query,
          resultText = resultText,
          onQueryChange = onQueryChange,
          onPreviousClick = onPreviousClick,
          onNextClick = onNextClick
        )
      },
      colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.onPrimary,
        scrolledContainerColor = MaterialTheme.colorScheme.onPrimary
      ),
      windowInsets = WindowInsets.statusBars.only(WindowInsetsSides.Horizontal),
      modifier = modifier.shadow(KIWIX_TOOLBAR_SHADOW_ELEVATION),
      scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
      navigationIcon = { NavigationIcon(onClick = onCloseClick) }
    )
  }
}

@Composable
private fun FindInPageBar(
  query: String,
  resultText: String,
  onQueryChange: (String) -> Unit,
  onPreviousClick: () -> Unit,
  onNextClick: () -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
  ) {
    KiwixSearchView(
      modifier = Modifier.weight(1f),
      value = query,
      onValueChange = onQueryChange,
      onClearClick = { onQueryChange("") }
    )

    Text(
      text = resultText,
      modifier = Modifier.padding(horizontal = EIGHT_DP),
      style = MaterialTheme.typography.bodyMedium
    )

    IconButton(onClick = onPreviousClick) {
      Icon(
        painter = painterResource(R.drawable.action_find_previous),
        contentDescription = stringResource(R.string.previous)
      )
    }

    IconButton(onClick = onNextClick) {
      Icon(
        painter = painterResource(R.drawable.action_find_next),
        contentDescription = stringResource(R.string.next)
      )
    }
  }
}
