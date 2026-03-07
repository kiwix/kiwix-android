/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.update.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Status
import org.kiwix.kiwixmobile.core.ui.components.ContentLoadingProgressBar
import org.kiwix.kiwixmobile.core.ui.components.ProgressBarStyle
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.EIGHT_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FIVE_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FOUR_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.ONE_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIXTEEN_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.TWO_DP
import org.kiwix.kiwixmobile.nav.destination.library.online.getDownloadedSizeText
import org.kiwix.kiwixmobile.update.getDownloadApkStateText
import org.kiwix.kiwixmobile.update.viewmodel.UpdateStates

@Suppress("all")
@Composable
fun DownloadInfoRow(
  state: UpdateStates,
  onCancel: () -> Unit
) {
  val downloadState = state.downloadApkItem.currentDownloadState
  val errorState = state.downloadApkItem.downloadError
  // Automatically invoke onStopClick if the download failed
  LaunchedEffect(downloadState) {
    if (downloadState == Status.FAILED) {
      when (errorState) {
        com.tonyodev.fetch2.Error.UNKNOWN_IO_ERROR,
        com.tonyodev.fetch2.Error.CONNECTION_TIMED_OUT,
        Error.UNKNOWN -> {
          // Only retrigger the download for CONNECTION_TIMED_OUT or UNKNOWN_IO_ERROR.
          // For other errors (e.g., REQUEST_DOES_NOT_EXIST, EMPTY_RESPONSE_FROM_SERVER, REQUEST_NOT_SUCCESSFUL),
          // we inform the user because the download cannot be restarted in these cases.
          onCancel.invoke()
        }

        else -> {
          // Do nothing for remaining errors, since re-download is not possible due to the absence of the ZIM file.
        }
      }
    }
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(EIGHT_DP),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(
      modifier = Modifier.weight(1f)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        DownloadText(
          label = getDownloadedSizeText(
            state.downloadApkItem.bytesDownloaded,
            state.downloadApkItem.totalSizeBytes
          )
        )
        DownloadText(
          label = getDownloadApkStateText(state, LocalContext.current)
        )
      }

      Spacer(modifier = Modifier.height(FOUR_DP))

      ContentLoadingProgressBar(
        progressBarStyle = ProgressBarStyle.HORIZONTAL,
        modifier = Modifier
          .padding(horizontal = ONE_DP, vertical = FIVE_DP),
        progress = state.downloadApkItem.progress,
      )
    }

    Spacer(modifier = Modifier.width(SIXTEEN_DP))

    IconButton(
      onClick = onCancel,
      modifier = Modifier
        .minimumInteractiveComponentSize()
        .padding(horizontal = TWO_DP)
    ) {
      Icon(
        painter = painterResource(id = org.kiwix.kiwixmobile.R.drawable.ic_stop_24dp),
        contentDescription = null
      )
    }
  }
}
