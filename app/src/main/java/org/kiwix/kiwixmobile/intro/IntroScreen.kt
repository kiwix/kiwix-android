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

package org.kiwix.kiwixmobile.intro

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.ui.components.KiwixButton
import org.kiwix.kiwixmobile.core.utils.ComposeDimens
import org.kiwix.kiwixmobile.intro.composable.HeadingText
import org.kiwix.kiwixmobile.intro.composable.LabelText
import org.kiwix.kiwixmobile.intro.composable.PageIndicator

private const val ANIMATION_DURATION: Long = 3000
private const val ANIMATION_SPEC: Int = 1200
private const val INITIAL_OFFSET: Int = -1200

@Composable
fun IntroScreen(
  onButtonClick: () -> Unit
) {
  Column(
    modifier = Modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val pagerIsDragged = pagerState.interactionSource.collectIsDraggedAsState()
    val pageInteractionSource = remember { MutableInteractionSource() }
    val pageIsPressed = pageInteractionSource.collectIsPressedAsState()

    // Stop auto-advancing when pager is dragged or one of the pages is pressed
    val autoAdvance = !pagerIsDragged.value && !pageIsPressed.value

    if (autoAdvance) {
      LaunchedEffect(pagerState, pageInteractionSource) {
        while (true) {
          delay(ANIMATION_DURATION)
          val nextPage = (pagerState.currentPage + 1) % pagerState.pageCount
          pagerState.animateScrollToPage(nextPage)
        }
      }
    }

    HorizontalPager(
      modifier = Modifier.weight(1f),
      state = pagerState
    ) { page ->
      when (page) {
        0 -> {
          IntroPage(
            shouldAnimate = false,
            headingText = R.string.welcome_to_the_family,
            labelText = R.string.humankind_knowledge,
            image = org.kiwix.kiwixmobile.R.drawable.kiwix_icon
          )
        }

        1 -> {
          IntroPage(
            shouldAnimate = true,
            headingText = R.string.save_books_offline,
            labelText = R.string.download_books_message,
            image = org.kiwix.kiwixmobile.R.drawable.ic_airplane
          )
        }

        2 -> {
          IntroPage(
            shouldAnimate = false,
            headingText = R.string.save_books_in_desired_storage,
            labelText = R.string.storage_location_hint,
            image = org.kiwix.kiwixmobile.R.drawable.ic_storage
          )
        }
      }
    }
    IndicatorColumn(
      pagerState = pagerState,
      onButtonClick = onButtonClick
    )
  }
}

@Composable
fun IntroPage(
  @StringRes headingText: Int,
  @StringRes labelText: Int,
  shouldAnimate: Boolean,
  image: Int,
) {
  var visible by remember {
    mutableStateOf(false)
  }
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(ComposeDimens.SIXTEEN_DP),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Bottom
  ) {
    LaunchedEffect(shouldAnimate) {
      visible = true
    }
    Column {
      if (shouldAnimate) {
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
    }
    Spacer(modifier = Modifier.height(ComposeDimens.ONE_HUNDRED_FIFTY))
    HeadingText(text = headingText)
    LabelText(text = labelText)
  }
}

@Composable
fun IndicatorColumn(
  pagerState: PagerState,
  onButtonClick: () -> Unit
) {
  KiwixButton(
    modifier = Modifier.padding(
      bottom = ComposeDimens.THIRTY_TWO_DP
    ),
    buttonText = stringResource(R.string.get_started).uppercase(),
    clickListener = onButtonClick
  )
  PageIndicator(pagerState)
}

@Composable
@Preview
fun PageIndicatorPreview() {
  IntroScreen(
    onButtonClick = {}
  )
}
