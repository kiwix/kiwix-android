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

package org.kiwix.kiwixmobile.localFileTransfer

import android.net.wifi.p2p.WifiP2pDevice
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import org.kiwix.kiwixmobile.R.drawable
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.main.DELETE_MENU_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.core.ui.components.ContentLoadingProgressBar
import org.kiwix.kiwixmobile.core.ui.components.KiwixAppBar
import org.kiwix.kiwixmobile.core.ui.components.NavigationIcon
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem.Vector
import org.kiwix.kiwixmobile.core.ui.theme.DodgerBlue
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FIFTEEN_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FILE_FOR_TRANSFER_TEXT_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FILE_ITEM_ICON_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FILE_ITEM_TEXT_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FIVE_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.NEARBY_DEVICES_TEXT_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.NEARBY_DEVICE_LIST_HEIGHT
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.NO_DEVICE_FOUND_TEXT_PADDING
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.ONE_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.PEER_DEVICE_ITEM_TEXT_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.TEN_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.YOUR_DEVICE_TEXT_SIZE
import org.kiwix.kiwixmobile.localFileTransfer.FileItem.FileStatus.ERROR
import org.kiwix.kiwixmobile.localFileTransfer.FileItem.FileStatus.SENDING
import org.kiwix.kiwixmobile.localFileTransfer.FileItem.FileStatus.SENT
import org.kiwix.kiwixmobile.localFileTransfer.FileItem.FileStatus.TO_BE_SENT

@Preview(device = "id:Nexus S")
@Composable
fun Preview() {
  LocalFileTransferScreen(
    deviceName = "Google Pixel 7a",
    toolbarTitle = org.kiwix.kiwixmobile.R.string.receive_files_title,
    isPeerSearching = true,
    peerDeviceList = listOf(WifiP2pDevice().apply { deviceName = "Redmi note 9" }),
    transferFileList = listOf(
      FileItem("DemoFile.zim")
    ),
    actionMenuItems = listOf(
      ActionMenuItem(
        Vector(Icons.Default.Search),
        R.string.search_label,
        { },
        isEnabled = true,
        testingTag = DELETE_MENU_BUTTON_TESTING_TAG
      )
    ),
    onDeviceItemClick = {},
    navigationIcon = {
      NavigationIcon(iconItem = IconItem.Drawable(R.drawable.ic_close_white_24dp), onClick = {})
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("ComposableLambdaParameterNaming", "LongParameterList")
@Composable
fun LocalFileTransferScreen(
  deviceName: String,
  @StringRes toolbarTitle: Int,
  isPeerSearching: Boolean,
  peerDeviceList: List<WifiP2pDevice>,
  transferFileList: List<FileItem>,
  actionMenuItems: List<ActionMenuItem>,
  onDeviceItemClick: (WifiP2pDevice) -> Unit,
  navigationIcon: @Composable () -> Unit
) {
  Scaffold(
    topBar = {
      KiwixAppBar(
        titleId = toolbarTitle,
        actionMenuItems = actionMenuItems,
        navigationIcon = navigationIcon
      )
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .background(Color.Transparent)
    ) {
      YourDeviceHeader(deviceName)
      HorizontalDivider(
        color = DodgerBlue,
        thickness = ONE_DP,
        modifier = Modifier.padding(horizontal = FIVE_DP)
      )
      NearbyDevicesSection(peerDeviceList, isPeerSearching, onDeviceItemClick)
      HorizontalDivider(
        color = DodgerBlue,
        thickness = ONE_DP,
        modifier = Modifier
          .padding(horizontal = FIVE_DP)
      )
      TransferFilesSection(transferFileList)
    }
  }
}

@Composable
fun NearbyDevicesSection(
  peerDeviceList: List<WifiP2pDevice>,
  isPeerSearching: Boolean,
  onDeviceItemClick: (WifiP2pDevice) -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .defaultMinSize(minHeight = NEARBY_DEVICE_LIST_HEIGHT)
  ) {
    Text(
      text = stringResource(R.string.nearby_devices),
      fontSize = NEARBY_DEVICES_TEXT_SIZE,
      fontFamily = FontFamily.Monospace,
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = FIVE_DP)
        .align(Alignment.CenterHorizontally),
      textAlign = TextAlign.Center
    )

    when {
      isPeerSearching -> ContentLoadingProgressBar(
        modifier = Modifier
          .padding(NO_DEVICE_FOUND_TEXT_PADDING)
          .align(Alignment.CenterHorizontally)
      )

      peerDeviceList.isEmpty() -> Text(
        text = stringResource(R.string.no_devices_found),
        modifier = Modifier
          .padding(NO_DEVICE_FOUND_TEXT_PADDING)
          .align(Alignment.CenterHorizontally),
        textAlign = TextAlign.Center
      )

      else -> LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .defaultMinSize(minHeight = NEARBY_DEVICE_LIST_HEIGHT)
      ) {
        items(peerDeviceList) { device ->
          PeerDeviceItem(device, onDeviceItemClick)
        }
      }
    }
  }
}

@Composable
private fun TransferFilesSection(transferFileList: List<FileItem>) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
      text = stringResource(R.string.files_for_transfer),
      fontSize = FILE_FOR_TRANSFER_TEXT_SIZE,
      fontFamily = FontFamily.Monospace,
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = TEN_DP),
      textAlign = TextAlign.Center
    )

    LazyColumn(modifier = Modifier.fillMaxSize()) {
      items(transferFileList) { file ->
        TransferFileItem(file)
      }
    }
  }
}

@Composable
private fun YourDeviceHeader(deviceName: String) {
  Column(modifier = Modifier.padding(horizontal = FIFTEEN_DP, vertical = FIVE_DP)) {
    Text(
      text = stringResource(R.string.your_device),
      fontStyle = FontStyle.Italic,
      fontSize = YOUR_DEVICE_TEXT_SIZE,
      modifier = Modifier
        .padding(top = FIVE_DP, bottom = ONE_DP)
    )
    val contentDescription = stringResource(R.string.device_name)
    Text(
      text = deviceName,
      fontWeight = FontWeight.Bold,
      fontSize = PEER_DEVICE_ITEM_TEXT_SIZE,
      modifier = Modifier
        .minimumInteractiveComponentSize()
        .semantics { this.contentDescription = contentDescription }
    )
  }
}

@Composable
fun TransferFileItem(
  fileItem: FileItem
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(TEN_DP),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = fileItem.fileName,
      fontSize = FILE_ITEM_TEXT_SIZE,
      modifier = Modifier
        .weight(1f)
        .padding(horizontal = FIVE_DP, vertical = ONE_DP)
    )

    val modifier = Modifier
      .size(FILE_ITEM_ICON_SIZE)
      .padding(horizontal = FIVE_DP, vertical = ONE_DP)
    when (fileItem.fileStatus) {
      SENDING -> ContentLoadingProgressBar(modifier)

      TO_BE_SENT,
      SENT,
      ERROR -> {
        val iconRes = when (fileItem.fileStatus) {
          FileItem.FileStatus.TO_BE_SENT -> drawable.ic_baseline_wait_24px
          FileItem.FileStatus.SENT -> drawable.ic_baseline_check_24px
          FileItem.FileStatus.ERROR -> drawable.ic_baseline_error_24px
          else -> error("Unhandled status: ${fileItem.fileStatus}")
        }

        Icon(
          painter = painterResource(iconRes),
          contentDescription = stringResource(R.string.status),
          modifier = modifier
        )
      }
    }
  }
}

@Suppress("MagicNumber")
@Composable
fun PeerDeviceItem(
  wifiP2PDevice: WifiP2pDevice,
  onDeviceItemClick: (WifiP2pDevice) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(TEN_DP)
      .clickable(onClick = { onDeviceItemClick.invoke(wifiP2PDevice) }),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = wifiP2PDevice.deviceName,
      fontSize = PEER_DEVICE_ITEM_TEXT_SIZE,
      fontWeight = FontWeight.Bold,
      textAlign = TextAlign.Center,
      modifier = Modifier
        .weight(3f)
        .padding(horizontal = FIVE_DP, vertical = ONE_DP)
    )
  }
}
