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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.kiwix.kiwixmobile.core.ui.components.KiwixAppBar
import org.kiwix.kiwixmobile.core.ui.components.KiwixSnackbarHost
import org.kiwix.kiwixmobile.update.composables.UpdateInfoCard
import org.kiwix.kiwixmobile.update.viewmodel.DownloadApkState
import org.kiwix.kiwixmobile.update.viewmodel.UpdateStates

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateScreen(
  state: UpdateStates,
  onUpdateClick: () -> Unit = {},
  onUpdateCancel: () -> Unit = {},
  onInstallApk: () -> Unit = {},
  content: @Composable () -> Unit
) {
  Scaffold(
    snackbarHost = { KiwixSnackbarHost(snackbarHostState = state.snackbarHostState) },
    topBar = {
      KiwixAppBar(
        title = "Update",
        navigationIcon = content,
      )
    }
  ) {
    UpdateInfoCard(
      modifier = Modifier.padding(it),
      state = state,
      onUpdateClick = onUpdateClick,
      onUpdateCancel = onUpdateCancel,
      onInstallApk = onInstallApk
    )
  }
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
