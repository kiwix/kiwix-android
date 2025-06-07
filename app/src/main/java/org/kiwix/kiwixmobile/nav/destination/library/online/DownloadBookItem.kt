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

package org.kiwix.kiwixmobile.nav.destination.library.online

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import com.tonyodev.fetch2.Status
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.ui.components.ContentLoadingProgressBar
import org.kiwix.kiwixmobile.core.ui.components.ProgressBarStyle
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.ui.models.toPainter
import org.kiwix.kiwixmobile.core.ui.theme.KiwixTheme
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FIVE_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.ONE_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIXTEEN_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.TEN_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.TWO_DP
import org.kiwix.kiwixmobile.ui.BookDescription
import org.kiwix.kiwixmobile.ui.BookIcon
import org.kiwix.kiwixmobile.ui.BookTitle
import org.kiwix.kiwixmobile.zimManager.libraryView.adapter.LibraryListItem.LibraryDownloadItem

const val DOWNLOAD_BOOK_ITEM_TESTING_TAG = "downloadBookItemTestingTag"
const val DOWNLOADING_PAUSE_BUTTON_TESTING_TAG = "downloadingPauseButtonTestingTag"
const val DOWNLOADING_STOP_BUTTON_TESTING_TAG = "downloadingStopButtonTestingTag"
const val DOWNLOADING_STATE_TEXT_TESTING_TAG = "downloadingStateTextTestingTag"

@Composable
fun DownloadBookItem(
  item: LibraryDownloadItem,
  onPauseResumeClick: (LibraryDownloadItem) -> Unit,
  onStopClick: (LibraryDownloadItem) -> Unit
) {
  // Automatically invoke onStopClick if the download failed
  LaunchedEffect(item.id, item.currentDownloadState) {
    if (item.currentDownloadState == Status.FAILED) {
      onStopClick.invoke(item)
    }
  }
  KiwixTheme {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(FIVE_DP)
        .testTag(DOWNLOAD_BOOK_ITEM_TESTING_TAG),
      shape = MaterialTheme.shapes.extraSmall,
      elevation = CardDefaults.elevatedCardElevation(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
      DownloadBookContent(item, onPauseResumeClick, onStopClick)
    }
  }
}

@Composable
private fun DownloadBookContent(
  item: LibraryDownloadItem,
  onPauseResumeClick: (LibraryDownloadItem) -> Unit,
  onStopClick: (LibraryDownloadItem) -> Unit
) {
  Row(
    modifier = Modifier
      .padding(top = SIXTEEN_DP, start = SIXTEEN_DP)
      .fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
  ) {
    BookIcon(item.favIconUrl, isOnlineLibrary = true)
    Column(
      modifier = Modifier
        .weight(1f)
        .padding(start = SIXTEEN_DP),
      horizontalAlignment = Alignment.Start
    ) {
      BookTitle(item.title)
      Spacer(modifier = Modifier.height(TWO_DP))
      BookDescription(item.description.orEmpty())
      ContentLoadingProgressBar(
        progressBarStyle = ProgressBarStyle.HORIZONTAL,
        modifier = Modifier.padding(horizontal = ONE_DP, vertical = FIVE_DP),
        progress = item.progress
      )
      DownloadStateRow(item)
    }
    PauseStopButtonsRow(item, onPauseResumeClick, onStopClick)
  }
}

@Composable
fun PauseStopButtonsRow(
  item: LibraryDownloadItem,
  onPauseResumeClick: (LibraryDownloadItem) -> Unit,
  onStopClick: (LibraryDownloadItem) -> Unit
) {
  val context = LocalContext.current
  Row(
    modifier = Modifier
      .fillMaxHeight(),
    verticalAlignment = Alignment.CenterVertically
  ) {
    IconButton(
      onClick = { onPauseResumeClick.invoke(item) },
      modifier = Modifier
        .padding(horizontal = TWO_DP)
        .minimumInteractiveComponentSize()
        .semantics { testTag = DOWNLOADING_PAUSE_BUTTON_TESTING_TAG }
    ) {
      Icon(
        painter = getPauseResumeButtonIcon(item).toPainter(),
        contentDescription = "${context.getString(string.tts_pause)}/${context.getString(string.tts_resume)}"
      )
    }

    IconButton(
      onClick = { onStopClick.invoke(item) },
      modifier = Modifier
        .minimumInteractiveComponentSize()
        .padding(horizontal = TWO_DP)
        .semantics { testTag = DOWNLOADING_STOP_BUTTON_TESTING_TAG }
    ) {
      Icon(
        painter = painterResource(id = R.drawable.ic_stop_24dp),
        contentDescription = context.getString(string.stop)
      )
    }
  }
}

@Composable
private fun getPauseResumeButtonIcon(item: LibraryDownloadItem): IconItem {
  val context = LocalContext.current
  return if (item.downloadState.toReadableState(context) == context.getString(string.paused_state)) {
    IconItem.Drawable(R.drawable.ic_play_24dp)
  } else {
    IconItem.Drawable(R.drawable.ic_pause_24dp)
  }
}

@Composable
private fun DownloadStateRow(item: LibraryDownloadItem) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = TEN_DP),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = item.downloadState.toReadableState(LocalContext.current).toString(),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onTertiary,
      modifier = Modifier
        .weight(1f)
        .testTag(DOWNLOADING_STATE_TEXT_TESTING_TAG)
    )
    Text(
      text = item.readableEta.toString(),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onTertiary
    )
  }
}
