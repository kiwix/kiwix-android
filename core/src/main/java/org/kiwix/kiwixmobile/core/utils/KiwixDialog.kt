/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.utils

import android.net.wifi.WifiConfiguration
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk

sealed class KiwixDialog(
  val title: Int?,
  val message: Int,
  val positiveMessage: Int,
  val negativeMessage: Int?
) {

  data class DeleteZim(override val args: List<Any>) : KiwixDialog(
    null, R.string.delete_zim_body, R.string.delete, R.string.no
  ), HasBodyFormatArgs {
    constructor(bookOnDisk: BookOnDisk) : this(listOf(bookOnDisk.book.title))
  }

  object LocationPermissionRationale : KiwixDialog(
    null,
    R.string.permission_rationale_location,
    android.R.string.yes,
    android.R.string.cancel
  )

  object StoragePermissionRationale : KiwixDialog(
    null,
    R.string.request_storage,
    android.R.string.yes,
    android.R.string.cancel
  )

  object EnableWifiP2pServices : KiwixDialog(
    null, R.string.request_enable_wifi, R.string.yes, android.R.string.no
  )

  object EnableLocationServices : KiwixDialog(
    null, R.string.request_enable_location, R.string.yes, android.R.string.no
  )

  object TurnOffHotspotManually : KiwixDialog(
    R.string.hotspot_failed_title,
    R.string.hotspot_failed_message,
    R.string.go_to_wifi_settings_label,
    null
  )

  data class ShowHotspotDetails(override val args: List<Any>) : KiwixDialog(
    R.string.hotspot_turned_on,
    R.string.hotspot_details_message,
    android.R.string.ok,
    null
  ), HasBodyFormatArgs {
    constructor(wifiConfiguration: WifiConfiguration) : this(
      listOf(wifiConfiguration.SSID, wifiConfiguration.preSharedKey)
    )
  }

  data class StartHotspotManually(
    val neutralMessage: Int = R.string.hotspot_dialog_neutral_button
  ) : KiwixDialog(
    R.string.hotspot_dialog_title,
    R.string.hotspot_dialog_message,
    R.string.go_to_settings_label,
    null
  )

  data class FileTransferConfirmation(override val args: List<Any>) : KiwixDialog(
    null, R.string.transfer_to, R.string.yes, android.R.string.cancel
  ), HasBodyFormatArgs {
    constructor(selectedPeerDeviceName: String) : this(listOf(selectedPeerDeviceName))
  }

  open class YesNoDialog(
    title: Int,
    message: Int
  ) : KiwixDialog(title, message, R.string.yes, R.string.no) {
    object StopDownload : YesNoDialog(
      R.string.confirm_stop_download_title, R.string.confirm_stop_download_msg
    )

    object WifiOnly : YesNoDialog(
      R.string.wifi_only_title, R.string.wifi_only_msg
    )
  }
}

interface HasBodyFormatArgs {
  val args: List<Any>
}
