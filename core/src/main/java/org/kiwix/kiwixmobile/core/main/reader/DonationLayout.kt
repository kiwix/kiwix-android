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

package org.kiwix.kiwixmobile.core.main.reader

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.ui.theme.DenimBlue800
import org.kiwix.kiwixmobile.core.ui.theme.White
import org.kiwix.kiwixmobile.core.ui.theme.dimHighlightedTextLight
import org.kiwix.kiwixmobile.core.utils.ComposeDimens

@Composable
fun DonationLayout(
  appName: String,
  onDonateButtonClick: () -> Unit,
  onLaterButtonClick: () -> Unit
) {
  Column(
    verticalArrangement = Arrangement.Bottom,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    DonationDialogComposable(
      appName,
      onDonateButtonClick,
      onLaterButtonClick
    )
  }
}

@Composable
fun DonationDialogComposable(
  appName: String,
  onDonateButtonClick: () -> Unit,
  onLaterButtonClick: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .wrapContentHeight()
      .padding(all = ComposeDimens.SIXTEEN_DP),
    shape = MaterialTheme.shapes.medium,
    elevation = CardDefaults.cardElevation(defaultElevation = ComposeDimens.SIX_DP),
    colors = CardDefaults.cardColors(
      containerColor = White
    )
  ) {
    Column(
      modifier = Modifier
        .padding(
          start = ComposeDimens.SIXTEEN_DP,
          end = ComposeDimens.SIXTEEN_DP,
          top = ComposeDimens.SIXTEEN_DP
        )
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
      ) {
        Image(
          painter = painterResource(id = R.drawable.ic_donation_icon),
          contentDescription = stringResource(id = R.string.donation_dialog_title),
          modifier = Modifier
            .size(ComposeDimens.FIFTY_DP)
        )
        Spacer(modifier = Modifier.width(ComposeDimens.TWELVE_DP))
        Column {
          DonationDialogHeadingText()
          DonationDialogSubHeadingText(appName = appName)
        }
      }
      Row(
        modifier = Modifier
          .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Spacer(modifier = Modifier.weight(1f))

        DonationDialogButton(
          onButtonClick = onLaterButtonClick,
          buttonText = R.string.rate_dialog_neutral
        )
        DonationDialogButton(
          onButtonClick = onDonateButtonClick,
          buttonText = R.string.make_donation
        )
      }
    }
  }
}

@Composable
fun DonationDialogButton(
  onButtonClick: () -> Unit,
  @StringRes buttonText: Int
) {
  TextButton(
    onClick = onButtonClick
  ) {
    Text(
      text = stringResource(buttonText),
      color = DenimBlue800
    )
  }
}

@Composable
fun DonationDialogHeadingText() {
  Text(
    text = stringResource(id = R.string.donation_dialog_title),
    style = MaterialTheme.typography.titleMedium,
    fontSize = ComposeDimens.SMALL_TITLE_TEXT_SIZE
  )
}

@Composable
fun DonationDialogSubHeadingText(appName: String) {
  Text(
    text = stringResource(
      R.string.donation_dialog_description,
      appName
    ),
    fontSize = ComposeDimens.FOURTEEN_SP,
    color = dimHighlightedTextLight,
    modifier = Modifier.padding(top = ComposeDimens.FOUR_DP)
  )
}

@Preview()
@Composable
fun DonationDialogComposablePreview() {
  DonationLayout(
    appName = "kiwix",
    onDonateButtonClick = {},
    onLaterButtonClick = {}
  )
}
