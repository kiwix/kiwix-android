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

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.ui.theme.KiwixTheme
import org.kiwix.kiwixmobile.intro.composable.IndicatorColumn
import org.kiwix.kiwixmobile.intro.composable.IntroPage

private const val ANIMATION_DURATION: Long = 2000
private const val PAGE_COUNT: Int = 3

@Composable
fun IntroScreen(
  onButtonClick: () -> Unit
) {
  KiwixTheme {
    Column(
      modifier = Modifier.fillMaxSize()
    ) {
      val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
      val pagerIsDragged = pagerState.interactionSource.collectIsDraggedAsState()
      val pageInteractionSource = remember { MutableInteractionSource() }
      val pageIsPressed = pageInteractionSource.collectIsPressedAsState()

      // Stop  auto-advancing when pager is dragged or one of the pages is pressed
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
      Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        HorizontalPager(
          modifier = Modifier.weight(1f),
          state = pagerState
        ) { page ->
          when (page) {
            0 -> {
              IntroPage(
                headingText = R.string.welcome_to_the_family,
                labelText = R.string.humankind_knowledge,
                image = org.kiwix.kiwixmobile.R.drawable.kiwix_icon
              )
            }

            1 -> {
              IntroPage(
                headingText = R.string.save_books_offline,
                labelText = R.string.download_books_message,
                image = org.kiwix.kiwixmobile.R.drawable.ic_airplane
              )
            }

            2 -> {
              IntroPage(
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
  }
}

@Composable
@Preview
fun PageIndicatorPreview() {
  IntroScreen(
    onButtonClick = {}
  )
}
