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

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.FileProvider
import org.kiwix.kiwixmobile.core.ui.components.ContentLoadingProgressBar
import org.kiwix.kiwixmobile.core.ui.components.ProgressBarStyle
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FIVE_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.ONE_DP
import org.kiwix.kiwixmobile.update.viewmodel.DownloadApkState
import org.kiwix.kiwixmobile.update.viewmodel.UpdateEvents
import org.kiwix.kiwixmobile.update.viewmodel.UpdateStates
import java.io.File

@Suppress("all")
@Composable
fun UpdateScreen(
  state: UpdateStates,
  events: (UpdateEvents) -> Unit = {}
) {
  val context = LocalContext.current

  Surface(
    modifier = Modifier
      .fillMaxSize(),
    color = Color.White,
  ) {
    Column(
      modifier = Modifier.background(color = Color.White)
        .fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      if (!state.loading) {
        Text(
          state.downloadApkState.name,
          color = Color.Black
        )
        Text(
          "${state.downloadApkState.readableEta}",
          color = Color.Black
        )
        Row {
          Button(
            onClick = {
              events(
                UpdateEvents.DownloadApk
              )
            }
          ) {
            Text("download")
          }
          Button(
            onClick = {
              events(
                UpdateEvents.CancelDownload
              )
            }
          ) {
            Text("cancel")
          }
          Button(
            onClick = {
              installApk(
                context = context,
                states = state
              )
            }
          ) {
            Text("install")
          }
        }

        ContentLoadingProgressBar(
          progressBarStyle = ProgressBarStyle.HORIZONTAL,
          modifier = Modifier
            .padding(horizontal = ONE_DP, vertical = FIVE_DP),
          progress = state.downloadApkState.progress,
        )
      } else {
        ContentLoadingProgressBar(
          progressBarStyle = ProgressBarStyle.CIRCLE,
        )
      }
    }
  }
}

@Suppress("all")
@SuppressLint("RequestInstallPackagesPolicy")
fun installApk(
  context: Context,
  states: UpdateStates
) {
  val apkFile =
    File(states.downloadApkState.file!!)

  val apkUri = FileProvider.getUriForFile(
    context,
    "${context.packageName}.fileprovider",
    apkFile
  )

  @Suppress("DEPRECATION")
  val installerIntent = Intent(Intent.ACTION_INSTALL_PACKAGE)
  installerIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
  installerIntent.setDataAndType(
    apkUri,
    "application/vnd.android.package-archive",
  )
  installerIntent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
  context.startActivity(installerIntent)
}

@Preview
@Composable
fun UpdateScreenPreview() {
  UpdateScreen(state = UpdateStates(downloadApkState = DownloadApkState()))
}
