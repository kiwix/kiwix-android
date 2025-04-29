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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.ui.components.KiwixButton
import org.kiwix.kiwixmobile.core.utils.ComposeDimens

private const val ANIMATION_SPEC: Int = 1200
private const val INITIAL_OFFSET: Int = -1200

const val GET_STARTED_BUTTON_TESTING_TAG = "getStartedButtonTestingTag"

@Composable
fun IntroPage(
  @StringRes headingText: Int,
  @StringRes labelText: Int,
  image: Int
) {
  var visible by remember {
    mutableStateOf(false)
  }
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(ComposeDimens.SIXTEEN_DP),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Spacer(modifier = Modifier.weight(1f))
    if (image == org.kiwix.kiwixmobile.R.drawable.ic_airplane) {
      LaunchedEffect(Unit) {
        visible = true
      }
      AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
          animationSpec = tween(ANIMATION_SPEC),
          initialOffsetX = {
            INITIAL_OFFSET
          }
        )
      ) {
        Image(
          modifier = Modifier.fillMaxWidth(),
          painter = painterResource(image),
          contentDescription = null
        )
      }
    } else {
      Image(
        modifier = Modifier.size(ComposeDimens.ONE_HUNDRED_FIFTY),
        painter = painterResource(image),
        contentDescription = null
      )
    }
    Spacer(modifier = Modifier.weight(1f))
    HeadingText(text = headingText)
    SubHeadingText(text = labelText)
  }
}

@Composable
fun IndicatorColumn(
  pagerState: PagerState,
  onButtonClick: () -> Unit
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    KiwixButton(
      modifier =
        Modifier
          .padding(bottom = ComposeDimens.THIRTY_TWO_DP)
          .testTag(GET_STARTED_BUTTON_TESTING_TAG),
      buttonText = stringResource(R.string.get_started),
      clickListener = onButtonClick
    )
    PageIndicator(pagerState)
  }
}
