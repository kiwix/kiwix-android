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

import android.content.Context
import androidx.compose.foundation.Image
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Status
import org.kiwix.kiwixmobile.R.drawable
import org.kiwix.kiwixmobile.core.ui.components.ContentLoadingProgressBar
import org.kiwix.kiwixmobile.core.ui.components.KiwixAppBar
import org.kiwix.kiwixmobile.core.ui.components.KiwixButton
import org.kiwix.kiwixmobile.core.ui.components.KiwixSnackbarHost
import org.kiwix.kiwixmobile.core.ui.components.ProgressBarStyle
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FIVE_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.ONE_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.TWO_DP
import org.kiwix.kiwixmobile.nav.destination.library.online.DOWNLOADING_STOP_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.nav.destination.library.online.getDownloadedSizeText
import org.kiwix.kiwixmobile.update.viewmodel.DownloadApkState
import org.kiwix.kiwixmobile.update.viewmodel.UpdateStates

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("all")
@Composable
fun UpdateScreen(
  state: UpdateStates,
  onUpdateClick: () -> Unit = {},
  onUpdateCancel: () -> Unit = {},
  onInstallApk: () -> Unit = {},
  navigationIcon: @Composable () -> Unit
) {
  Scaffold(
    snackbarHost = { KiwixSnackbarHost(snackbarHostState = state.snackbarHostState) },
    topBar = {
      KiwixAppBar(
        title = "Update",
        navigationIcon = navigationIcon,
      )
    }
  ) {
    UpdateInfoCard(
      state = state,
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
    AppInfoRow()

    Spacer(modifier = Modifier.height(16.dp))
    val downloadApkState = state.downloadApkItem
    when (downloadApkState.currentDownloadState) {
      Status.QUEUED, Status.DOWNLOADING, Status.ADDED -> {
        DownloadInfoRow(
          state = state,
          onCancel = onUpdateCancel,
        )
      }

      Status.NONE, Status.CANCELLED, Status.FAILED, Status.REMOVED, Status.DELETED -> {
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
          buttonText = "INSTALL"
        )
      }

      Status.PAUSED -> {
        // pause implementation is not present in apk download
      }

      else -> {
        KiwixButton(
          modifier = Modifier.fillMaxWidth(),
          clickListener = onUpdateClick,
          buttonText = "UPDATE"
        )
      }
    }
  }
}

@Composable
fun AppInfoRow() {
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
        label = "Update Kiwix"
      )
      Spacer(modifier = Modifier.height(4.dp))
      DownloadText(
        text = "A new version of Kiwix is available. You can update it now"
      )
    }
  }
}

@Suppress("all")
@Composable
fun DownloadInfoRow(
  state: UpdateStates,
  onCancel: () -> Unit,
  modifier: Modifier = Modifier,
) {
  // Automatically invoke onStopClick if the download failed
  LaunchedEffect(state.downloadApkItem.downloadApkState) {
    if (state.downloadApkItem.currentDownloadState == Status.FAILED) {
      when (state.downloadApkItem.downloadError) {
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
        val downloadState = getDownloadApkStateText(state, LocalContext.current)
        DownloadText(
          text = getDownloadedSizeText(
            state.downloadApkItem.bytesDownloaded,
            state.downloadApkItem.totalSizeBytes
          )
        )
        DownloadText(
          text = downloadState
        )
      }

      Spacer(modifier = Modifier.height(4.dp))

      ContentLoadingProgressBar(
        progressBarStyle = ProgressBarStyle.HORIZONTAL,
        modifier = Modifier
          .padding(horizontal = ONE_DP, vertical = FIVE_DP),
        progress = state.downloadApkItem.progress,
      )
    }

    Spacer(modifier = Modifier.width(16.dp))

    IconButton(
      onClick = onCancel,
      modifier = Modifier
        .minimumInteractiveComponentSize()
        .padding(horizontal = TWO_DP)
        .semantics { testTag = DOWNLOADING_STOP_BUTTON_TESTING_TAG }
    ) {
      Icon(
        painter = painterResource(id = org.kiwix.kiwixmobile.R.drawable.ic_stop_24dp),
        contentDescription = null
      )
    }
  }
}

@Composable
fun LabelText(
  label: String
) {
  Text(
    text = label,
    style = MaterialTheme.typography.titleMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant
  )
}

@Composable
fun DownloadText(
  text: String,
) {
  Text(
    text = text,
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onTertiary
  )
}

@Preview
@Composable
fun UpdateScreenPreview() {
  UpdateScreen(
    state = UpdateStates(),
    onUpdateClick = {},
    onUpdateCancel = {},
    onInstallApk = {},
    {}
  )
}

/**
 * Returns the current download state text.
 * If the download is "In Progress", it returns the ETA. Otherwise, it returns the current
 * download state (e.g., Pending, Paused, or Complete).
 */
fun getDownloadApkStateText(
  state: UpdateStates,
  context: Context
): String {
  val currentDownloadState = state.downloadApkItem.downloadApkState
  return if (currentDownloadState == DownloadApkState.Running) {
    state.downloadApkItem.readableEta.toString()
  } else {
    currentDownloadState.toReadableState(context).toString()
  }
}
