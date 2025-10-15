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
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.tonyodev.fetch2.R.string
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.ui.components.KiwixButton
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.ui.models.toPainter
import org.kiwix.kiwixmobile.core.ui.theme.AlabasterWhite
import org.kiwix.kiwixmobile.core.ui.theme.Black
import org.kiwix.kiwixmobile.core.ui.theme.DenimBlue400
import org.kiwix.kiwixmobile.core.ui.theme.DenimBlue800
import org.kiwix.kiwixmobile.core.ui.theme.KiwixTheme
import org.kiwix.kiwixmobile.core.ui.theme.MineShaftGray900
import org.kiwix.kiwixmobile.core.ui.theme.White
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.CRASH_IMAGE_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.DETAILS_INCLUDED_TEXT_START_PADDING
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.DETAILS_INCLUDED_TEXT_TOP_PADDING
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.EIGHT_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SEVENTEEN_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIXTEEN_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIXTY_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.TWELVE_DP

@Composable
fun ErrorActivityScreen(
  @StringRes crashTitleStringId: Int,
  @StringRes messageStringId: Int,
  diagnosticDetailsItems: List<Int>,
  noThanksButtonClickListener: () -> Unit,
  sendDetailsButtonClickListener: () -> Unit
) {
  val crashMessageAndCheckboxTextColor = if (isSystemInDarkTheme()) {
    White
  } else {
    Black
  }
  KiwixTheme {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .systemBarsPadding()
        .imePadding()
        .padding(SIXTEEN_DP),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      CrashTitle(crashTitleStringId)
      AppIcon()
      CrashMessage(messageStringId, crashMessageAndCheckboxTextColor)
      DetailsIncludedInErrorReport(
        diagnosticDetailsItems,
        Modifier
          .weight(1f)
          .padding(top = SEVENTEEN_DP, bottom = EIGHT_DP),
        crashMessageAndCheckboxTextColor
      )

      // Buttons on the ErrorActivity.
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .navigationBarsPadding(),
        horizontalArrangement = Arrangement.SpaceEvenly
      ) {
        KiwixButton(
          buttonText = stringResource(R.string.crash_button_confirm),
          clickListener = { sendDetailsButtonClickListener.invoke() },
        )

        KiwixButton(
          clickListener = { noThanksButtonClickListener.invoke() },
          buttonText = stringResource(R.string.no_thanks)
        )
      }
    }
  }
}

@Composable
private fun CrashTitle(
  @StringRes titleId: Int
) {
  val textColor = if (isSystemInDarkTheme()) {
    AlabasterWhite
  } else {
    MineShaftGray900
  }
  Text(
    text = stringResource(titleId),
    style = MaterialTheme.typography.headlineSmall,
    color = textColor,
    modifier = Modifier.padding(top = SIXTY_DP, start = EIGHT_DP, end = EIGHT_DP)
  )
}

@Composable
private fun AppIcon() {
  Image(
    painter = IconItem.MipmapImage(R.mipmap.ic_launcher).toPainter(),
    contentDescription = stringResource(id = string.app_name),
    modifier = Modifier
      .height(CRASH_IMAGE_SIZE)
      .padding(top = TWELVE_DP, start = EIGHT_DP, end = EIGHT_DP)
  )
}

@Composable
private fun CrashMessage(
  @StringRes messageId: Int,
  crashMessageAndCheckboxTextColor: Color
) {
  Text(
    text = stringResource(messageId),
    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
    textAlign = TextAlign.Center,
    color = crashMessageAndCheckboxTextColor,
    modifier = Modifier.padding(start = EIGHT_DP, top = EIGHT_DP, end = EIGHT_DP)
  )
}

@Composable
private fun DetailsIncludedInErrorReport(
  diagnosticDetailsItems: List<Int>,
  modifier: Modifier,
  crashMessageAndCheckboxTextColor: Color
) {
  LazyColumn(modifier = modifier) {
    itemsIndexed(diagnosticDetailsItems) { index, item ->
      DetailsIncludedItem(item, crashMessageAndCheckboxTextColor)
    }
  }
}

@Composable
private fun DetailsIncludedItem(
  diagnosticDetailsItem: Int,
  crashMessageAndCheckboxTextColor: Color
) {
  val iconColor = if (isSystemInDarkTheme()) {
    DenimBlue400
  } else {
    DenimBlue800
  }
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(
        start = DETAILS_INCLUDED_TEXT_START_PADDING,
        top = DETAILS_INCLUDED_TEXT_TOP_PADDING
      ),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = Icons.Default.CheckCircle,
      contentDescription = null,
      tint = iconColor,
      modifier = Modifier.minimumInteractiveComponentSize().semantics { hideFromAccessibility() }
    )
    Text(
      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
      text = stringResource(id = diagnosticDetailsItem),
      color = crashMessageAndCheckboxTextColor,
      modifier = Modifier.padding(start = DETAILS_INCLUDED_TEXT_TOP_PADDING)
    )
  }
}
