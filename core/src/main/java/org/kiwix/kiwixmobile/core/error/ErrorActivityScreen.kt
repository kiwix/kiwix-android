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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.tonyodev.fetch2.R.string
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.loadBitmapFromMipmap
import org.kiwix.kiwixmobile.core.ui.components.CrashCheckBox
import org.kiwix.kiwixmobile.core.ui.components.KiwixButton
import org.kiwix.kiwixmobile.core.ui.theme.AlabasterWhite
import org.kiwix.kiwixmobile.core.ui.theme.ErrorActivityBackground
import org.kiwix.kiwixmobile.core.ui.theme.KiwixTheme
import org.kiwix.kiwixmobile.core.ui.theme.White
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.CRASH_IMAGE_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.EIGHT_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SEVENTEEN_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIXTEEN_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIXTY_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.TWELVE_DP

@Composable
fun ErrorActivityScreen(
  @StringRes crashTitleStringId: Int,
  @StringRes messageStringId: Int,
  checkBoxData: List<Pair<Int, MutableState<Boolean>>>,
  noThanksButtonClickListener: () -> Unit,
  sendDetailsButtonClickListener: () -> Unit
) {
  KiwixTheme {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(ErrorActivityBackground)
        .systemBarsPadding()
        .imePadding()
        .padding(SIXTEEN_DP),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      CrashTitle(crashTitleStringId)
      CrashImage()
      CrashMessage(messageStringId)
      CrashCheckBoxList(
        checkBoxData,
        Modifier
          .weight(1f)
          .padding(top = SEVENTEEN_DP, bottom = EIGHT_DP)
      )

      // Buttons on the ErrorActivity.
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .navigationBarsPadding(),
        horizontalArrangement = Arrangement.SpaceEvenly
      ) {
        KiwixButton(
          buttonTextId = R.string.crash_button_confirm,
          clickListener = { sendDetailsButtonClickListener.invoke() },
        )

        KiwixButton(
          clickListener = { noThanksButtonClickListener.invoke() },
          buttonTextId = R.string.no_thanks
        )
      }
    }
  }
}

@Composable
private fun CrashTitle(
  @StringRes titleId: Int
) {
  Text(
    text = stringResource(titleId),
    style = MaterialTheme.typography.headlineSmall,
    color = AlabasterWhite,
    modifier = Modifier.padding(top = SIXTY_DP, start = EIGHT_DP, end = EIGHT_DP)
  )
}

@Composable
private fun CrashImage() {
  Image(
    bitmap = ImageBitmap.loadBitmapFromMipmap(LocalContext.current, R.mipmap.ic_launcher),
    contentDescription = stringResource(id = string.app_name),
    modifier = Modifier
      .height(CRASH_IMAGE_SIZE)
      .padding(top = TWELVE_DP, start = EIGHT_DP, end = EIGHT_DP)
  )
}

@Composable
private fun CrashMessage(
  @StringRes messageId: Int
) {
  Text(
    text = stringResource(messageId),
    style = MaterialTheme.typography.bodyMedium,
    textAlign = TextAlign.Center,
    color = White,
    modifier = Modifier.padding(start = EIGHT_DP, top = EIGHT_DP, end = EIGHT_DP)
  )
}

@Composable
private fun CrashCheckBoxList(
  checkBoxData: List<Pair<Int, MutableState<Boolean>>>,
  modifier: Modifier
) {
  LazyColumn(modifier = modifier) {
    itemsIndexed(checkBoxData) { _, item ->
      CrashCheckBox(item.first to item.second)
    }
  }
}
