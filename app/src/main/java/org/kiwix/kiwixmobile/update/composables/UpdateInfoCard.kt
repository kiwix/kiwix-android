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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tonyodev.fetch2.Status
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.ui.components.KiwixButton
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIXTEEN_DP
import org.kiwix.kiwixmobile.update.viewmodel.UpdateStates

@Composable
fun UpdateInfoCard(
  state: UpdateStates,
  onUpdateClick: () -> Unit,
  onUpdateCancel: () -> Unit = {},
  onInstallApk: () -> Unit = {},
  modifier: Modifier
) {
  Column(
    modifier = modifier
      .padding(SIXTEEN_DP)
      .fillMaxSize(),
    verticalArrangement = Arrangement.Center
  ) {
    AppInfoRow()

    Spacer(modifier = Modifier.height(SIXTEEN_DP))
    val downloadApkState = state.downloadApkItem
    when (downloadApkState.currentDownloadState) {
      Status.QUEUED, Status.DOWNLOADING, Status.ADDED -> {
        DownloadInfoRow(
          state = state,
          onCancel = onUpdateCancel,
        )
      }

      Status.NONE, Status.CANCELLED, Status.FAILED, Status.REMOVED, Status.DELETED -> {
        UpdateButton(onUpdateClick)
      }

      Status.COMPLETED -> {
        InstallButton(onInstallApk)
      }

      Status.PAUSED -> {
        // pause implementation is not present in apk download
      }
    }
  }
}

@Composable
fun InstallButton(onInstallApk: () -> Unit) {
  KiwixButton(
    modifier = Modifier.fillMaxWidth(),
    clickListener = {
      onInstallApk()
    },
    buttonText = stringResource(R.string.install)
  )
}

@Composable
fun UpdateButton(onUpdateClick: () -> Unit) {
  KiwixButton(
    modifier = Modifier.fillMaxWidth(),
    clickListener = onUpdateClick,
    buttonText = stringResource(R.string.update)
  )
}
