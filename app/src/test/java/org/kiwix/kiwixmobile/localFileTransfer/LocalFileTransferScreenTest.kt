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

package org.kiwix.kiwixmobile.localFileTransfer

import android.net.wifi.p2p.WifiP2pDevice
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.ui.components.SHOWCASE_VIEW_MESSAGE_TESTING_TAG
import org.kiwix.kiwixmobile.core.ui.components.SHOWCASE_VIEW_NEXT_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.page.SEARCH_ICON_TESTING_TAG
import org.kiwix.kiwixmobile.core.utils.CONTENT_LOADING_PROGRESSBAR_TESTING_TAG
import org.kiwix.kiwixmobile.utils.TestApplication
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Behavior-driven UI tests for [LocalFileTransferScreen].
 *
 * All tests render through the top-level [LocalFileTransferScreen] composable,
 * using [LocalFileTransferUiState] to drive different UI states.
 * This ensures we test real user-visible behavior, not internal
 * composable implementation details.
 *
 * Follows the same testing pattern as ReaderScreenComposablesTest (PR #4797).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R], application = TestApplication::class)
class LocalFileTransferScreenTest {
  @get:Rule
  val composeTestRule = createComposeRule()

  private val context get() = RuntimeEnvironment.getApplication()

  /**
   * Creates a minimal [LocalFileTransferUiState] with sensible defaults for testing.
   * Test-specific values can be overridden via named parameters.
   */
  private fun createTestState(
    deviceName: String = "Test Device",
    isReceiver: Boolean = false,
    isPeerSearching: Boolean = false,
    peers: List<WifiP2pDevice> = emptyList(),
    transferFiles: List<FileItem> = emptyList(),
    shouldShowShowCase: Boolean = false,
    isWritePermissionRequired: Boolean = false
  ): LocalFileTransferUiState = LocalFileTransferUiState(
    deviceName = deviceName,
    isReceiver = isReceiver,
    isPeerSearching = isPeerSearching,
    peers = peers,
    transferFiles = transferFiles,
    shouldShowShowCase = shouldShowShowCase,
    isWritePermissionRequired = isWritePermissionRequired
  )

  /**
   * Renders the full [LocalFileTransferScreen] with the given state,
   * providing minimal test doubles for required dependencies.
   */
  private fun renderScreen(
    state: LocalFileTransferUiState,
    actionMenuItems: List<ActionMenuItem> = emptyList(),
    onDeviceItemClick: (WifiP2pDevice) -> Unit = {},
    onShowCaseDisplayed: () -> Unit = {}
  ) {
    composeTestRule.setContent {
      LocalFileTransferScreen(
        state = state,
        actionMenuItems = actionMenuItems,
        onDeviceItemClick = onDeviceItemClick,
        onShowCaseDisplayed = onShowCaseDisplayed,
        navigationIcon = {}
      )
    }
  }
  // App Bar Title Tests

  @Test
  fun localFileTransferScreen_appBar_showsSendTitleWhenSender() {
    renderScreen(createTestState(isReceiver = false))
    composeTestRule
      .onNodeWithText(context.getString(org.kiwix.kiwixmobile.R.string.send_files_title))
      .assertIsDisplayed()
  }

  @Test
  fun localFileTransferScreen_appBar_showsReceiveTitleWhenReceiver() {
    renderScreen(createTestState(isReceiver = true))
    composeTestRule
      .onNodeWithText(context.getString(org.kiwix.kiwixmobile.R.string.receive_files_title))
      .assertIsDisplayed()
  }

  // Your Device Header Tests

  @Test
  fun localFileTransferScreen_yourDeviceHeader_displaysLabel() {
    renderScreen(createTestState())
    composeTestRule
      .onNodeWithText(context.getString(R.string.your_device))
      .assertIsDisplayed()
  }

  @Test
  fun localFileTransferScreen_yourDeviceHeader_displaysDeviceName() {
    renderScreen(createTestState(deviceName = "My Pixel 7"))
    composeTestRule
      .onNodeWithContentDescription(context.getString(R.string.device_name))
      .assertIsDisplayed()
  }

  @Test
  fun localFileTransferScreen_yourDeviceHeader_showsCorrectDeviceName() {
    renderScreen(createTestState(deviceName = "My Pixel 7"))
    composeTestRule
      .onNodeWithText("My Pixel 7")
      .assertIsDisplayed()
  }

  // Nearby Devices Section Tests

  @Test
  fun localFileTransferScreen_nearbyDevices_displaysSectionTitle() {
    renderScreen(createTestState())
    composeTestRule
      .onNodeWithText(context.getString(R.string.nearby_devices))
      .assertIsDisplayed()
  }

  @Test
  fun localFileTransferScreen_nearbyDevices_showsNoDevicesFoundWhenEmpty() {
    renderScreen(createTestState(peers = emptyList(), isPeerSearching = false))
    composeTestRule
      .onNodeWithText(context.getString(R.string.no_devices_found))
      .assertIsDisplayed()
  }

  @Test
  fun localFileTransferScreen_nearbyDevices_hidesNoDevicesTextWhenSearching() {
    renderScreen(createTestState(isPeerSearching = true))
    composeTestRule
      .onNodeWithText(context.getString(R.string.no_devices_found))
      .assertDoesNotExist()
  }

  @Test
  fun localFileTransferScreen_nearbyDevices_showsProgressBarWhenSearching() {
    renderScreen(createTestState(isPeerSearching = true))
    composeTestRule
      .onNodeWithTag(CONTENT_LOADING_PROGRESSBAR_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun localFileTransferScreen_nearbyDevices_displaysPeerDeviceName() {
    val device = createMockWifiP2pDevice("Nearby Phone")
    renderScreen(createTestState(peers = listOf(device)))
    composeTestRule
      .onNodeWithText("Nearby Phone")
      .assertIsDisplayed()
  }

  @Test
  fun localFileTransferScreen_nearbyDevices_displaysMultiplePeers() {
    val device1 = createMockWifiP2pDevice("Phone A")
    val device2 = createMockWifiP2pDevice("Phone B")
    renderScreen(createTestState(peers = listOf(device1, device2)))
    composeTestRule
      .onNodeWithText("Phone A")
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithText("Phone B")
      .assertIsDisplayed()
  }

  @Test
  fun localFileTransferScreen_nearbyDevices_clickDevice_triggersCallback() {
    var clickedDevice: WifiP2pDevice? = null
    val device = createMockWifiP2pDevice("Clickable Device")
    renderScreen(
      createTestState(peers = listOf(device)),
      onDeviceItemClick = { clickedDevice = it }
    )
    composeTestRule
      .onNodeWithText("Clickable Device")
      .performClick()
    assertTrue(
      "Device click callback should be triggered",
      clickedDevice === device
    )
  }

  // Transfer Files Section Tests

  @Test
  fun localFileTransferScreen_transferFiles_displaysSectionTitle() {
    renderScreen(createTestState())
    composeTestRule
      .onNodeWithText(context.getString(R.string.files_for_transfer))
      .assertIsDisplayed()
  }

  @Test
  fun localFileTransferScreen_transferFiles_displaysFileName() {
    val file = FileItem("test_file.zim")
    renderScreen(createTestState(transferFiles = listOf(file)))
    composeTestRule
      .onNodeWithText("test_file.zim")
      .assertIsDisplayed()
  }

  @Test
  fun localFileTransferScreen_transferFiles_displaysMultipleFiles() {
    val file1 = FileItem("wiki_en.zim")
    val file2 = FileItem("wiki_fr.zim")
    renderScreen(createTestState(transferFiles = listOf(file1, file2)))
    composeTestRule
      .onNodeWithText("wiki_en.zim")
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithText("wiki_fr.zim")
      .assertIsDisplayed()
  }

  @Test
  fun localFileTransferScreen_transferFiles_showsStatusIconForToBeSent() {
    val file = FileItem("pending.zim")
    renderScreen(createTestState(transferFiles = listOf(file)))
    composeTestRule
      .onNodeWithContentDescription(
        context.getString(R.string.status) + "0"
      )
      .assertIsDisplayed()
  }

  @Test
  fun localFileTransferScreen_transferFiles_showsStatusIconForSent() {
    val file = FileItem("sent.zim")
    file.fileStatus = FileItem.FileStatus.SENT
    renderScreen(createTestState(transferFiles = listOf(file)))
    composeTestRule
      .onNodeWithContentDescription(
        context.getString(R.string.status) + "0"
      )
      .assertIsDisplayed()
  }

  @Test
  fun localFileTransferScreen_transferFiles_showsStatusIconForError() {
    val file = FileItem("error.zim")
    file.fileStatus = FileItem.FileStatus.ERROR
    renderScreen(createTestState(transferFiles = listOf(file)))
    composeTestRule
      .onNodeWithContentDescription(
        context.getString(R.string.status) + "0"
      )
      .assertIsDisplayed()
  }

  @Test
  fun localFileTransferScreen_transferFiles_multipleStatusIcons() {
    val file1 = FileItem("file1.zim")
    val file2 = FileItem("file2.zim")
    file2.fileStatus = FileItem.FileStatus.SENT
    renderScreen(createTestState(transferFiles = listOf(file1, file2)))
    composeTestRule
      .onNodeWithContentDescription(
        context.getString(R.string.status) + "0"
      )
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithContentDescription(
        context.getString(R.string.status) + "1"
      )
      .assertIsDisplayed()
  }

  // Combined State Tests

  @Test
  fun localFileTransferScreen_displaysAllSectionsSimultaneously() {
    val device = createMockWifiP2pDevice("Peer Device")
    val file = FileItem("transfer.zim")
    renderScreen(
      createTestState(
        deviceName = "My Device",
        peers = listOf(device),
        transferFiles = listOf(file)
      )
    )
    // Verify all sections are displayed
    composeTestRule
      .onNodeWithText(context.getString(R.string.your_device))
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithText("My Device")
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithText(context.getString(R.string.nearby_devices))
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithText("Peer Device")
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithText(context.getString(R.string.files_for_transfer))
      .assertIsDisplayed()
    composeTestRule
      .onNodeWithText("transfer.zim")
      .assertIsDisplayed()
  }

  @Test
  fun localFileTransferScreen_emptyDeviceName_displaysEmptyHeader() {
    renderScreen(createTestState(deviceName = ""))
    composeTestRule
      .onNodeWithText(context.getString(R.string.your_device))
      .assertIsDisplayed()
  }

  // Showcase Tests

  @Test
  fun localFileTransferScreen_showcase_isDisplayedWhenRequested() {
    renderScreen(createTestState(shouldShowShowCase = true))
    // The showcase is displayed, so the message should be visible.
    composeTestRule
      .onNodeWithTag(SHOWCASE_VIEW_MESSAGE_TESTING_TAG)
      .assertIsDisplayed()
  }

  @Test
  fun localFileTransferScreen_showcase_triggersCallbackOnCompletion() {
    var showcaseDisplayed = false
    val actionMenuItem = ActionMenuItem(
      icon = IconItem.Drawable(org.kiwix.kiwixmobile.core.R.drawable.action_search),
      contentDescription = org.kiwix.kiwixmobile.core.R.string.search_label,
      onClick = {},
      testingTag = SEARCH_ICON_TESTING_TAG
    )

    renderScreen(
      createTestState(shouldShowShowCase = true),
      actionMenuItems = listOf(actionMenuItem),
      onShowCaseDisplayed = { showcaseDisplayed = true }
    )

    // Wait for all targets to be positioned and added to the map.
    composeTestRule.waitForIdle()

    // In LocalFileTransferScreen, there are 4 showcase targets:
    // 0: Search icon (index 0) - now added via actionMenuItems
    // 1: Your device header (index 1)
    // 2: Nearby devices list (index 2) - added because peers is empty
    // 3: Files for transfer (index 3)

    repeat(4) {
      composeTestRule
        .onNodeWithTag(SHOWCASE_VIEW_NEXT_BUTTON_TESTING_TAG)
        .assertIsDisplayed()
        .performClick()
    }

    assertTrue("Showcase displayed callback should be triggered", showcaseDisplayed)
  }

  // Helper Methods

  /**
   * Creates a [WifiP2pDevice] with the given device name.
   * Uses a real instance since deviceName is a public Java field
   * that MockK cannot intercept.
   */
  private fun createMockWifiP2pDevice(name: String): WifiP2pDevice =
    WifiP2pDevice().apply {
      deviceName = name
    }
}
