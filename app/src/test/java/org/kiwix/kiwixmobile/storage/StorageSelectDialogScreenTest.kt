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

package org.kiwix.kiwixmobile.storage

import android.os.Build
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import eu.mhutti1.utils.storage.StorageDevice
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.settings.StorageCalculator
import org.kiwix.kiwixmobile.core.ui.components.STORAGE_DEVICE_ITEM_TESTING_TAG
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.sharedFunctions.TestApplication
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R], application = TestApplication::class)
class StorageSelectDialogScreenTest {
  @get:Rule
  val composeRule = createComposeRule()
  private val storageCalculator = mockk<StorageCalculator>(relaxed = true)
  private val kiwixDataStore = mockk<KiwixDataStore>(relaxed = true)

  private fun setDialogScreen(title: String?, deviceList: List<StorageDevice> = emptyList()) {
    composeRule.setContent {
      StorageSelectDialogScreen(
        title = title,
        titleSize = null,
        storageDeviceList = deviceList,
        storageCalculator = storageCalculator,
        kiwixDataStore = kiwixDataStore,
        shouldShowStorageSelected = false,
        onSelectAction = {}
      )
    }
  }

  @Test
  fun showsTitle_whenTitleProvided() {
    setDialogScreen("Select Storage")
    composeRule
      .onNodeWithTag(STORAGE_SELECTION_DIALOG_TITLE_TESTING_TAG)
      .assertExists()
      .assertTextEquals("Select Storage")
  }

  @Test
  fun hidesTitle_whenTitleIsNull() {
    setDialogScreen(null)
    composeRule
      .onNodeWithTag(STORAGE_SELECTION_DIALOG_TITLE_TESTING_TAG)
      .assertDoesNotExist()
  }

  @Test
  fun showsCustomTitle_whenCustomTitleProvided() {
    setDialogScreen("Move File To")
    composeRule
      .onNodeWithTag(STORAGE_SELECTION_DIALOG_TITLE_TESTING_TAG)
      .assertExists()
      .assertTextEquals("Move File To")
  }

  @Test
  fun showsStorageDeviceItems_whenStorageDevicesProvided() {
    val storageDevice = mockk<StorageDevice>(relaxed = true)
    every { kiwixDataStore.selectedStoragePosition } returns MutableStateFlow(0)
    every { kiwixDataStore.selectedStorage } returns MutableStateFlow("Internal Storage")
    setDialogScreen(null, listOf(storageDevice))

    composeRule
      .onNodeWithTag(STORAGE_DEVICE_ITEM_TESTING_TAG)
      .assertExists()
  }
}
