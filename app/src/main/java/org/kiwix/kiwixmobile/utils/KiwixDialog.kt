package org.kiwix.kiwixmobile.utils

import android.net.wifi.WifiConfiguration
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk

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
