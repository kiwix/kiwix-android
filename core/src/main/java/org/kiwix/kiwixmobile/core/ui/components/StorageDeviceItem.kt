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

package org.kiwix.kiwixmobile.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import eu.mhutti1.utils.storage.StorageDevice
import org.kiwix.kiwixmobile.core.extensions.getFreeSpace
import org.kiwix.kiwixmobile.core.extensions.getUsedSpace
import org.kiwix.kiwixmobile.core.extensions.storagePathAndTitle
import org.kiwix.kiwixmobile.core.extensions.usedPercentage
import org.kiwix.kiwixmobile.core.settings.StorageCalculator
import org.kiwix.kiwixmobile.core.ui.theme.DenimBlue800
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.EIGHT_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FOUR_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FREE_SPACE_TEXTVIEW_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.PROGRESS_BAR_HEIGHT
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIXTEEN_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.STORAGE_TITLE_TEXTVIEW_SIZE
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore

const val STORAGE_DEVICE_ITEM_TESTING_TAG = "storageDeviceItemTestingTag"

@Composable
fun StorageDeviceItem(
  index: Int,
  storageDevice: StorageDevice,
  shouldShowCheckboxSelected: Boolean,
  onClick: (StorageDevice) -> Unit,
  storageCalculator: StorageCalculator,
  kiwixDataStore: KiwixDataStore,
) {
  var storagePathAndTitle by remember { mutableStateOf("") }
  var usedSpace by remember { mutableStateOf("") }
  var freeSpace by remember { mutableStateOf("") }
  var progress by remember { mutableIntStateOf(0) }
  val context = LocalContext.current
  val currentStorageIndex by kiwixDataStore.selectedStoragePosition.collectAsState(ZERO)
  LaunchedEffect(storageDevice) {
    usedSpace = storageDevice.getUsedSpace(context, storageCalculator)
    freeSpace = storageDevice.getFreeSpace(context, storageCalculator)
    progress = storageDevice.usedPercentage(storageCalculator)
    storagePathAndTitle = storageDevice.storagePathAndTitle(
      context,
      index,
      kiwixDataStore,
      storageCalculator
    )
  }
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = SIXTEEN_DP)
      .selectable(
        selected = shouldShowCheckboxSelected && currentStorageIndex == index,
        onClick = { onClick(storageDevice) }
      )
      .semantics { testTag = STORAGE_DEVICE_ITEM_TESTING_TAG }
      .padding(vertical = EIGHT_DP)
  ) {
    RadioButton(
      selected = shouldShowCheckboxSelected && currentStorageIndex == index,
      onClick = null
    )
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = EIGHT_DP)
    ) {
      StorageTitleAndPath(storagePathAndTitle)
      ContentLoadingProgressBar(
        progressBarStyle = ProgressBarStyle.HORIZONTAL,
        progress = progress,
        modifier = Modifier
          .fillMaxWidth()
          .height(PROGRESS_BAR_HEIGHT)
          .padding(top = EIGHT_DP),
        progressBarColor = DenimBlue800
      )
      StorageSpaceRow(usedSpace, freeSpace)
    }
  }
}

@Composable
private fun StorageSpaceRow(
  usedSpace: String,
  freeSpace: String
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = FOUR_DP)
  ) {
    Text(
      text = usedSpace,
      style = MaterialTheme.typography.bodySmall,
      modifier = Modifier.weight(1f),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Text(
      text = freeSpace,
      style = MaterialTheme.typography.bodySmall,
      modifier = Modifier.alignByBaseline(),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun StorageTitleAndPath(storagePathAndTitle: String) {

  val (title, path) = storagePathAndTitle.split('\n', limit = 2)
    .let { it.first() to it.getOrNull(1) }

  Text(
    text = title,
    style = MaterialTheme.typography.bodyLarge,
    color = MaterialTheme.colorScheme.onSurface,
    fontSize = STORAGE_TITLE_TEXTVIEW_SIZE
  )
  if (!path.isNullOrEmpty()) {
    Spacer(modifier = Modifier.height(FOUR_DP))
    Text(
      text = path,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      fontSize = FREE_SPACE_TEXTVIEW_SIZE
    )
  }
}
