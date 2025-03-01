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

package org.kiwix.kiwixmobile.core.error

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tonyodev.fetch2.R.string
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.composeviews.ComposeButton
import org.kiwix.kiwixmobile.core.composeviews.CrashCheckBox
import org.kiwix.kiwixmobile.core.extensions.loadBitmapFromMipmap

@Composable
fun ErrorActivityScreen(
  @StringRes crashTitleStringId: Int,
  @StringRes messageStringId: Int,
  checkBoxData: List<Pair<Int, MutableState<Boolean>>>,
  noThanksButtonClickListener: () -> Unit,
  sendDetailsButtonClickListener: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(colorResource(id = R.color.error_activity_background))
      .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text = stringResource(crashTitleStringId),
      fontSize = 24.sp,
      color = colorResource(id = R.color.alabaster_white),
      modifier = Modifier.padding(top = 60.dp, start = 8.dp, end = 8.dp)
    )

    Image(
      bitmap = ImageBitmap.loadBitmapFromMipmap(LocalContext.current, R.mipmap.ic_launcher),
      contentDescription = stringResource(id = string.app_name),
      modifier = Modifier
        .height(70.dp)
        .padding(top = 12.dp, start = 8.dp, end = 8.dp)
    )

    Text(
      text = stringResource(messageStringId),
      fontSize = 14.sp,
      textAlign = TextAlign.Center,
      color = colorResource(id = R.color.white),
      modifier = Modifier.padding(start = 8.dp, top = 8.dp, end = 8.dp)
    )

    Column(modifier = Modifier.weight(1f)) {
      LazyColumn(
        modifier = Modifier.fillMaxWidth()
      ) {
        itemsIndexed(checkBoxData) { _, item ->
          CrashCheckBox(item.first to item.second)
        }
      }
    }

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 32.dp),
      horizontalArrangement = Arrangement.SpaceEvenly
    ) {
      ComposeButton(
        buttonTextId = R.string.crash_button_confirm,
        clickListener = { sendDetailsButtonClickListener.invoke() },
      )

      ComposeButton(
        clickListener = { noThanksButtonClickListener.invoke() },
        buttonTextId = R.string.no_thanks
      )
    }
  }
}
