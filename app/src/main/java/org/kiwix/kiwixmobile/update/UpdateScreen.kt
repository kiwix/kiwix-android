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

package org.kiwix.kiwixmobile.update

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Status
import org.kiwix.kiwixmobile.R.drawable
import org.kiwix.kiwixmobile.core.ui.components.ContentLoadingProgressBar
import org.kiwix.kiwixmobile.core.ui.components.KiwixButton
import org.kiwix.kiwixmobile.core.ui.components.KiwixSnackbarHost
import org.kiwix.kiwixmobile.core.ui.components.ProgressBarStyle
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FIVE_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.ONE_DP
import org.kiwix.kiwixmobile.update.viewmodel.DownloadApkState
import org.kiwix.kiwixmobile.update.viewmodel.UpdateEvents
import org.kiwix.kiwixmobile.update.viewmodel.UpdateStates

@Suppress("all")
@Composable
fun UpdateScreen(
  state: UpdateStates,
  events: (UpdateEvents) -> Unit = {},
  onUpdateClick: () -> Unit = {},
  onUpdateCancel: () -> Unit = {},
  onInstallApk: () -> Unit = {}
) {
  LaunchedEffect(state.downloadApkState.currentDownloadState) {
    if (state.downloadApkState.currentDownloadState == Status.COMPLETED) {
      onInstallApk()
    }
  }
  Scaffold(
    snackbarHost = { KiwixSnackbarHost(snackbarHostState = state.snackbarHostState) },
  ) {
    UpdateInfoCard(
      state = state,
      events = events,
      onUpdateClick = onUpdateClick,
      onUpdateCancel = onUpdateCancel,
      onInstallApk = onInstallApk
    )
  }
}

@Suppress("all")
@Composable
fun UpdateInfoCard(
  state: UpdateStates,
  events: (UpdateEvents) -> Unit,
  onUpdateClick: () -> Unit,
  onUpdateCancel: () -> Unit = {},
  onInstallApk: () -> Unit = {}
) {
  Column(
    modifier = Modifier
      .padding(16.dp)
      .fillMaxSize(),
    verticalArrangement = Arrangement.Center
  ) {
    Row(
      verticalAlignment = Alignment.Top
    ) {
      Image(
        modifier = Modifier.size(50.dp),
        painter = painterResource(drawable.kiwix_icon),
        contentDescription = null
      )
      Spacer(modifier = Modifier.width(16.dp))
      Column {
        LabelText(
          label = "Update available for Kiwix",
          style = MaterialTheme.typography.titleMedium,
          color = Color.Black
        )
        Spacer(modifier = Modifier.height(4.dp))
        LabelText(
          label = "Download size: 1.4 MB",
          style = MaterialTheme.typography.bodySmall,
          color = Color.Gray
        )
      }
    }
    Spacer(modifier = Modifier.height(16.dp))
    LabelText(
      label = "Download the latest version",
      style = MaterialTheme.typography.bodyMedium,
      color = Color.DarkGray
    )

    val downloadApkState = state.downloadApkState
    when (downloadApkState.currentDownloadState) {
      Status.QUEUED, Status.DOWNLOADING -> {
        DownloadInfoRow(
          state = state,
          onCancel = onUpdateCancel,
        )
      }

      Status.NONE, Status.CANCELLED -> {
        KiwixButton(
          modifier = Modifier.fillMaxWidth(),
          clickListener = onUpdateClick,
          buttonText = "UPDATE"
        )
      }

      Status.COMPLETED -> {
        KiwixButton(
          modifier = Modifier.fillMaxWidth(),
          clickListener = {
            onInstallApk()
          },
          buttonText = "Install"
        )
      }

      Status.DOWNLOADING -> TODO()
      Status.PAUSED -> TODO()
      Status.FAILED -> TODO()
      Status.REMOVED -> TODO()
      Status.DELETED -> TODO()
      Status.ADDED -> TODO()
    }
  }
}

@Composable
fun DownloadInfoRow(
  state: UpdateStates,
  onCancel: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val contentColor = Color.Gray
  // Automatically invoke onStopClick if the download failed
  LaunchedEffect(state.downloadApkState.currentDownloadState) {
    if (state.downloadApkState.currentDownloadState == Status.FAILED) {
      when (state.downloadApkState.downloadError) {
        Error.UNKNOWN_IO_ERROR,
        Error.CONNECTION_TIMED_OUT,
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
    modifier = modifier
      .fillMaxWidth()
      .padding(8.dp),
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
          text = " ",
          contentColor = contentColor
        )
        DownloadText(
          text = " ",
          contentColor = contentColor
        )
      }

      Spacer(modifier = Modifier.height(4.dp))

      ContentLoadingProgressBar(
        progressBarStyle = ProgressBarStyle.HORIZONTAL,
        modifier = Modifier
          .padding(horizontal = ONE_DP, vertical = FIVE_DP),
        progress = state.downloadApkState.progress,
      )
    }

    Spacer(modifier = Modifier.width(16.dp))

    Icon(
      imageVector = Icons.Default.Close,
      contentDescription = "Cancel upload",
      tint = contentColor,
      modifier = Modifier
        .size(20.dp)
        .clickable(onClick = onCancel)
    )
  }
}

@Composable
fun LabelText(
  label: String,
  style: TextStyle = MaterialTheme.typography.bodyMedium,
  color: Color = Color.DarkGray
) {
  Text(
    text = label,
    style = style,
    color = color
  )
}

@Composable
fun DownloadText(
  text: String,
  contentColor: Color
) {
  Text(
    text = text,
    fontSize = 12.sp,
    color = contentColor,
    fontWeight = FontWeight.Medium
  )
}

@Preview
@Composable
fun UpdateScreenPreview() {
  UpdateScreen(state = UpdateStates(downloadApkState = DownloadApkState()))
}
