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

package org.kiwix.kiwixmobile.storage

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import eu.mhutti1.utils.storage.StorageDevice
import org.kiwix.kiwixmobile.core.settings.StorageCalculator
import org.kiwix.kiwixmobile.core.ui.components.StorageDeviceItem
import org.kiwix.kiwixmobile.core.ui.theme.KiwixDialogTheme
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.TEN_DP
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore

const val STORAGE_SELECTION_DIALOG_TITLE_TESTING_TAG = "storageSelectionDialogTestingTag"
val STORAGE_SELECT_STORAGE_TITLE_TEXTVIEW_SIZE = 16.sp

@Suppress("MagicNumber")
private val STORAGE_DIALOG_MAX_WIDTH = 600.dp
private val STORAGE_DIALOG_CORNER_RADIUS = 16.dp

/**
 * Pure composable replacement for the old StorageSelectDialog DialogFragment.
 * Shows a dialog with storage device selection, managing its own Dialog window.
 */
@Suppress("LongParameterList")
@Composable
fun StorageSelectDialog(
  title: String?,
  titleSize: TextUnit?,
  storageDeviceList: List<StorageDevice>,
  storageCalculator: StorageCalculator,
  kiwixDataStore: KiwixDataStore,
  shouldShowCheckboxSelected: Boolean,
  onDismiss: () -> Unit,
  onSelectAction: (StorageDevice) -> Unit
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier.widthIn(max = STORAGE_DIALOG_MAX_WIDTH),
      shape = RoundedCornerShape(STORAGE_DIALOG_CORNER_RADIUS),
      color = MaterialTheme.colorScheme.surface
    ) {
      StorageSelectDialogScreen(
        title,
        titleSize,
        storageDeviceList,
        storageCalculator,
        kiwixDataStore,
        shouldShowCheckboxSelected
      ) { storageDevice ->
        onSelectAction(storageDevice)
        onDismiss()
      }
    }
  }
}

@Composable
fun StorageSelectDialogScreen(
  title: String?,
  titleSize: TextUnit?,
  storageDeviceList: List<StorageDevice>,
  storageCalculator: StorageCalculator,
  kiwixDataStore: KiwixDataStore,
  shouldShowStorageSelected: Boolean,
  onSelectAction: (StorageDevice) -> Unit
) {
  KiwixDialogTheme {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(TEN_DP)
    ) {
      StorageSelectDialogTitle(title, titleSize)
      StorageDeviceList(
        shouldShowStorageSelected,
        storageDeviceList,
        onSelectAction,
        storageCalculator,
        kiwixDataStore
      )
    }
  }
}

@Composable
fun StorageDeviceList(
  shouldShowStorageSelected: Boolean,
  storageDeviceList: List<StorageDevice>,
  onSelectAction: (StorageDevice) -> Unit,
  storageCalculator: StorageCalculator,
  kiwixDataStore: KiwixDataStore
) {
  LazyColumn {
    itemsIndexed(storageDeviceList) { index, item ->
      StorageDeviceItem(
        index,
        item,
        shouldShowStorageSelected,
        onSelectAction,
        storageCalculator,
        kiwixDataStore
      )
    }
  }
}

@Composable
private fun StorageSelectDialogTitle(title: String?, titleSize: TextUnit?) {
  title?.let {
    Text(
      text = it,
      maxLines = 1,
      fontSize = titleSize ?: STORAGE_SELECT_STORAGE_TITLE_TEXTVIEW_SIZE,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier
        .fillMaxWidth()
        .semantics { testTag = STORAGE_SELECTION_DIALOG_TITLE_TESTING_TAG },
      textAlign = TextAlign.Center
    )
  }
}
