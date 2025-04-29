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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import eu.mhutti1.utils.storage.StorageDevice
import org.kiwix.kiwixmobile.core.settings.StorageCalculator
import org.kiwix.kiwixmobile.core.ui.theme.KiwixDialogTheme
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.TEN_DP
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.ui.StorageDeviceItem

const val STORAGE_SELECTION_DIALOG_TITLE_TESTING_TAG = "storageSelectionDialogTestingTag"

@Composable
fun StorageSelectDialogScreen(
  title: String?,
  titleSize: TextUnit?,
  storageDeviceList: List<StorageDevice>,
  storageCalculator: StorageCalculator,
  sharedPreferenceUtil: SharedPreferenceUtil,
  shouldShowCheckboxSelected: Boolean,
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
        shouldShowCheckboxSelected,
        storageDeviceList,
        onSelectAction,
        storageCalculator,
        sharedPreferenceUtil
      )
    }
  }
}

@Composable
fun StorageDeviceList(
  shouldShowCheckboxSelected: Boolean,
  storageDeviceList: List<StorageDevice>,
  onSelectAction: (StorageDevice) -> Unit,
  storageCalculator: StorageCalculator,
  sharedPreferenceUtil: SharedPreferenceUtil
) {
  LazyColumn {
    itemsIndexed(storageDeviceList) { index, item ->
      StorageDeviceItem(
        index,
        item,
        shouldShowCheckboxSelected,
        onSelectAction,
        storageCalculator,
        sharedPreferenceUtil
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
