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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.ui.components.ContentLoadingProgressBar
import org.kiwix.kiwixmobile.core.ui.components.KiwixAppBar
import org.kiwix.kiwixmobile.core.ui.components.KiwixButton
import org.kiwix.kiwixmobile.core.ui.components.KiwixSnackbarHost
import org.kiwix.kiwixmobile.core.ui.components.ProgressBarStyle
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem.Drawable
import org.kiwix.kiwixmobile.core.ui.models.toPainter
import org.kiwix.kiwixmobile.core.ui.theme.Black
import org.kiwix.kiwixmobile.core.ui.theme.KiwixDialogTheme
import org.kiwix.kiwixmobile.core.ui.theme.White
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.EIGHT_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FOUR_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.READER_BOTTOM_APP_BAR_BUTTON_ICON_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.READER_BOTTOM_APP_BAR_LAYOUT_HEIGHT
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.TTS_BUTTONS_CONTROL_ALPHA

const val CONTENT_LOADING_PROGRESSBAR_TESTING_TAG = "contentLoadingProgressBarTestingTag"

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("ComposableLambdaParameterNaming")
@Composable
fun ReaderScreen(
  state: ReaderScreenState,
  actionMenuItems: List<ActionMenuItem>,
  navigationIcon: @Composable () -> Unit
) {
  KiwixDialogTheme {
    Scaffold(
      snackbarHost = { KiwixSnackbarHost(snackbarHostState = state.snackBarHostState) },
      topBar = { KiwixAppBar(R.string.note, navigationIcon, actionMenuItems) },
      floatingActionButton = { BackToTopFab(state) }
    ) { paddingValues ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues)
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize(),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          ShowProgressBarIfZIMFilePageIsLoading(state)
          if (state.isNoBookOpenInReader) {
            NoBookOpenView(state.onOpenLibraryButtonClicked)
          }
        }
        TtsControls(state)
        ShowFullScreenView(state)
        ShowDonationLayout(state)
      }
    }
  }
}

@Composable
private fun ShowFullScreenView(state: ReaderScreenState) {
  if (state.fullScreenItem.first) {
    state.fullScreenItem.second
  }
}

@Composable
private fun ShowProgressBarIfZIMFilePageIsLoading(state: ReaderScreenState) {
  if (state.pageLoadingItem.first) {
    ContentLoadingProgressBar(
      modifier = Modifier.testTag(CONTENT_LOADING_PROGRESSBAR_TESTING_TAG),
      progressBarStyle = ProgressBarStyle.HORIZONTAL,
      progress = state.pageLoadingItem.second
    )
  }
}

@Composable
private fun NoBookOpenView(
  onOpenLibraryButtonClicked: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = FOUR_DP)
      .verticalScroll(rememberScrollState()),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Text(
      text = stringResource(R.string.no_open_book),
      style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
      textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(EIGHT_DP))
    KiwixButton(
      buttonText = stringResource(R.string.open_library),
      clickListener = onOpenLibraryButtonClicked
    )
  }
}

@Composable
private fun BoxScope.TtsControls(state: ReaderScreenState) {
  if (state.showTtsControls) {
    Row(modifier = Modifier.align(Alignment.TopCenter)) {
      Button(
        onClick = state.onPauseTtsClick,
        modifier = Modifier
          .weight(1f)
          .alpha(TTS_BUTTONS_CONTROL_ALPHA)
      ) {
        Text(
          text = stringResource(R.string.tts_pause),
          fontWeight = FontWeight.Bold
        )
      }
      Spacer(modifier = Modifier.width(FOUR_DP))
      Button(
        onClick = state.onStopTtsClick,
        modifier = Modifier
          .weight(1f)
          .alpha(TTS_BUTTONS_CONTROL_ALPHA)
      ) {
        Text(
          text = stringResource(R.string.stop),
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}

@Composable
private fun BackToTopFab(state: ReaderScreenState) {
  if (state.showBackToTopButton) {
    FloatingActionButton(
      onClick = state.backToTopButtonClick,
      modifier = Modifier,
      containerColor = MaterialTheme.colorScheme.primary,
      contentColor = MaterialTheme.colorScheme.onPrimary,
      shape = FloatingActionButtonDefaults.smallShape
    ) {
      Icon(
        painter = Drawable(R.drawable.action_find_previous).toPainter(),
        contentDescription = stringResource(R.string.pref_back_to_top),
        tint = White
      )
    }
  }
}

@Composable
private fun BottomAppBarOfReaderScreen(
  onBookmarkClick: () -> Unit,
  onBackClick: () -> Unit,
  onHomeClick: () -> Unit,
  onForwardClick: () -> Unit,
  onTocClick: () -> Unit
) {
  BottomAppBar(
    containerColor = Black,
    contentColor = White
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(READER_BOTTOM_APP_BAR_LAYOUT_HEIGHT),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceEvenly
    ) {
      // Bookmark Icon
      BottomAppBarButtonIcon(
        onBookmarkClick,
        Drawable(R.drawable.ic_bookmark_border_24dp),
        stringResource(R.string.bookmarks)
      )
      // Back Icon(for going to previous page)
      BottomAppBarButtonIcon(
        onBackClick,
        Drawable(R.drawable.ic_keyboard_arrow_left_24dp),
        stringResource(R.string.go_to_previous_page)
      )
      // Home Icon(to open the home page of ZIM file)
      BottomAppBarButtonIcon(
        onHomeClick,
        Drawable(R.drawable.action_home),
        stringResource(R.string.menu_home)
      )
      // Forward Icon(for going to next page)
      BottomAppBarButtonIcon(
        onForwardClick,
        Drawable(R.drawable.ic_keyboard_arrow_right_24dp),
        stringResource(R.string.go_to_next_page)
      )
      // Toggle Icon(to open the table of content in right side bar)
      BottomAppBarButtonIcon(
        onTocClick,
        Drawable(R.drawable.ic_toc_24dp),
        stringResource(R.string.table_of_contents)
      )
    }
  }
}

@Composable
private fun BottomAppBarButtonIcon(
  onClick: () -> Unit,
  buttonIcon: IconItem,
  contentDescription: String
) {
  IconButton(onClick = onClick) {
    Icon(
      buttonIcon.toPainter(),
      contentDescription,
      modifier = Modifier.size(READER_BOTTOM_APP_BAR_BUTTON_ICON_SIZE)
    )
  }
}

@Composable
private fun BoxScope.ShowDonationLayout(state: ReaderScreenState) {
  if (state.shouldShowDonationPopup) {
    Box(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
    ) {
      // TODO create donation popup layout.
    }
  }
}
