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

package org.kiwix.kiwixmobile.nav.destination.library.local

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.ui.components.ContentLoadingProgressBar
import org.kiwix.kiwixmobile.core.ui.components.ProgressBarStyle
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.COPY_MOVE_DIALOG_TITLE_TEXT_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.DIALOG_TITLE_BOTTOM_PADDING
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.EIGHT_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIXTEEN_DP
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.nav.destination.library.COPY_MOVE_DIALOG_TITLE_TESTING_TAG
import javax.inject.Inject

class CopyMoveProgressBarControllerImpl @Inject constructor(
  private val context: Context
) : CopyMoveProgressBarController {
  /**
   * Holds the state for the copy/move progress bar.
   *
   * A [Pair] containing:
   *  - [String]: The message to display below the progress bar.
   *  - [Int]: The current progress value (0 to 100).
   */
  private var progressBarState = mutableStateOf(Pair("", ZERO))
  private lateinit var alertDialogShower: AlertDialogShower
  override fun setAlertDialogShower(alertDialogShower: AlertDialogShower) {
    this.alertDialogShower = alertDialogShower
  }

  override fun showPreparingCopyMoveDialog() {
    alertDialogShower.show(KiwixDialog.PreparingCopyingFilesDialog { ContentLoadingProgressBar() })
  }

  override fun showProgress(title: String) {
    progressBarState.value =
      context.getString(R.string.percentage, ZERO) to ZERO
    alertDialogShower.show(
      KiwixDialog.CopyMoveProgressBarDialog(
        customViewBottomPadding = ZERO.dp,
        customGetView = { CopyMoveProgressDialog(title) }
      )
    )
  }

  @Composable
  private fun CopyMoveProgressDialog(title: String) {
    Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(
          fontSize = COPY_MOVE_DIALOG_TITLE_TEXT_SIZE,
          fontWeight = FontWeight.Medium
        ),
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = DIALOG_TITLE_BOTTOM_PADDING)
          .semantics { testTag = COPY_MOVE_DIALOG_TITLE_TESTING_TAG }
      )
      ContentLoadingProgressBar(
        progress = progressBarState.value.second,
        progressBarStyle = ProgressBarStyle.HORIZONTAL
      )
      Spacer(modifier = Modifier.height(EIGHT_DP))
      Text(
        progressBarState.value.first,
        modifier = Modifier.padding(end = SIXTEEN_DP, bottom = SIXTEEN_DP)
      )
    }
  }

  @Suppress("InjectDispatcher")
  override suspend fun updateProgress(progress: Int) {
    withContext(Dispatchers.Main) {
      synchronized(this) {
        progressBarState.value =
          context.getString(R.string.percentage, progress) to progress
      }
    }
  }

  override fun dismissCopyMoveProgressDialog() {
    alertDialogShower.dismiss()
  }

  override fun hidePreparingCopyMoveDialog() {
    dismissCopyMoveProgressDialog()
  }

  override fun showCopyMoveDialog(
    title: String,
    onCopyClicked: () -> Unit,
    onMovedClicked: () -> Unit
  ) {
    alertDialogShower.show(
      KiwixDialog.CopyMoveFileToPublicDirectoryDialog(title),
      onCopyClicked,
      onMovedClicked
    )
  }
}
