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

package org.kiwix.kiwixmobile.custom.download

import android.content.Context
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.data.remote.isAuthenticationUrl
import org.kiwix.kiwixmobile.core.downloader.model.DownloadItem
import org.kiwix.kiwixmobile.core.downloader.model.DownloadState
import org.kiwix.kiwixmobile.core.ui.components.ContentLoadingProgressBar
import org.kiwix.kiwixmobile.core.ui.components.KiwixButton
import org.kiwix.kiwixmobile.core.ui.components.ProgressBarStyle
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.CUSTOM_DOWNLOAD_LAYOUT_TOP_MARGIN
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.CUSTOM_DOWNLOAD_PROGRESS_BAR_WIDTH
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FIVE_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIX_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.TWENTY_DP
import org.kiwix.kiwixmobile.custom.R
import org.kiwix.kiwixmobile.custom.download.State.DownloadComplete
import org.kiwix.kiwixmobile.custom.download.State.DownloadFailed
import org.kiwix.kiwixmobile.custom.download.State.DownloadInProgress
import org.kiwix.kiwixmobile.custom.download.State.DownloadRequired

@Composable
fun CustomDownloadScreen(
  state: State,
  onDownloadClick: () -> Unit,
  onRetryClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .animateContentSize()
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(top = CUSTOM_DOWNLOAD_LAYOUT_TOP_MARGIN),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Image(
        painter = painterResource(id = R.mipmap.ic_launcher_foreground),
        contentDescription = stringResource(id = R.string.app_name),
        modifier = Modifier
          .wrapContentSize()
          .align(Alignment.CenterHorizontally)
      )
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
        contentAlignment = Alignment.Center
      ) {
        Crossfade(targetState = state, label = "download-state") { currentState ->
          when (currentState) {
            is DownloadRequired -> DownloadRequiredView(onDownloadClick)
            is DownloadInProgress -> DownloadInProgressView(currentState.downloads[0])
            is DownloadFailed -> DownloadErrorView(onRetryClick, currentState)
            is DownloadComplete -> DownloadCompleteView()
          }
        }
      }
    }
  }
}

@Composable
private fun DownloadRequiredView(onDownloadClick: () -> Unit) {
  Column(
    modifier = Modifier.wrapContentSize(Alignment.Center),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text = stringResource(id = R.string.invalid_installation),
      textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(TWENTY_DP))
    KiwixButton(
      clickListener = onDownloadClick,
      buttonText = stringResource(id = string.download)
    )
  }
}

@Composable
private fun DownloadInProgressView(downloadItem: DownloadItem) {
  Column(
    modifier = Modifier.wrapContentSize(Alignment.Center),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(text = downloadItem.readableEta.toString())

    Spacer(modifier = Modifier.height(TWENTY_DP))

    Row(verticalAlignment = Alignment.CenterVertically) {
      ContentLoadingProgressBar(
        progress = downloadItem.progress,
        modifier = Modifier
          .width(CUSTOM_DOWNLOAD_PROGRESS_BAR_WIDTH)
          .height(SIX_DP),
        progressBarStyle = ProgressBarStyle.HORIZONTAL
      )
      Spacer(modifier = Modifier.width(FIVE_DP))
      Text(text = downloadItem.downloadState.toReadableState(LocalContext.current).toString())
    }
  }
}

@Composable
private fun DownloadErrorView(onRetryClick: () -> Unit, downloadFailed: DownloadFailed) {
  val context = LocalContext.current
  Column(
    modifier = Modifier.wrapContentSize(Alignment.Center),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text = getActualErrorMessage(downloadFailed, context),
      textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(TWENTY_DP))
    KiwixButton(
      clickListener = onRetryClick,
      buttonText = stringResource(id = R.string.retry)
    )
  }
}

private fun getActualErrorMessage(downloadFailed: DownloadFailed, context: Context): String {
  if (downloadFailed.downloadState.zimUrl?.isAuthenticationUrl == false) {
    return getErrorMessageFromDownloadState(downloadFailed.downloadState, context)
  }

  val defaultErrorMessage = getErrorMessageFromDownloadState(downloadFailed.downloadState, context)
  // Check if `REQUEST_NOT_SUCCESSFUL` indicates an unsuccessful response from the server.
  // If the server does not respond to the URL, we will display a custom message to the user.
  return if (defaultErrorMessage == context.getString(
      string.failed_state,
      "REQUEST_NOT_SUCCESSFUL"
    )
  ) {
    context.getString(
      string.failed_state,
      context.getString(string.custom_download_error_message_for_authentication_failed)
    )
  } else {
    defaultErrorMessage
  }
}

private fun getErrorMessageFromDownloadState(
  downloadState: DownloadState,
  context: Context
): String = "${downloadState.toReadableState(context)}"

@Composable
private fun DownloadCompleteView() {
  Box(
    modifier = Modifier.wrapContentSize(Alignment.Center),
    contentAlignment = Alignment.TopCenter
  ) {
    Text(
      text = stringResource(id = string.complete),
      textAlign = TextAlign.Center
    )
  }
}
