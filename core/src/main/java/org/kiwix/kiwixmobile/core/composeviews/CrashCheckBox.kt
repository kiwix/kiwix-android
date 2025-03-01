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

package org.kiwix.kiwixmobile.core.composeviews

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.kiwix.kiwixmobile.core.R

@Composable
fun CrashCheckBox(checkBoxItem: Pair<Int, MutableState<Boolean>>) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(start = 100.dp, top = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Checkbox(
      checked = checkBoxItem.second.value,
      onCheckedChange = { checkBoxItem.second.value = it },
      colors = CheckboxDefaults.colors(
        checkedColor = colorResource(id = R.color.denim_blue200),
        checkmarkColor = colorResource(id = R.color.error_activity_background),
        uncheckedColor = Color.White
      )
    )
    Text(
      text = stringResource(id = checkBoxItem.first),
      color = colorResource(id = R.color.white),
      modifier = Modifier.padding(start = 8.dp)
    )
  }
}
