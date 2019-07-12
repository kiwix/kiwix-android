package org.kiwix.kiwixmobile.utils

import android.net.wifi.p2p.WifiP2pDevice
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk

sealed class KiwixDialog(
  val title: Int?,
  val message: Int,
  val positiveMessage: Int,
  val negativeMessage: Int
) {

  data class DeleteZim(override val args: Array<out Any>) : KiwixDialog(
      null, R.string.delete_zim_body, R.string.delete, R.string.no
  ), HasBodyFormatArgs {
    constructor(bookOnDisk: BookOnDisk) : this(arrayOf(bookOnDisk.book.title))
  }

  object LocationPermissionRationale : KiwixDialog( // For the local file transfer module
      null, R.string.permission_rationale_location, android.R.string.yes, android.R.string.cancel
  )

  object StoragePermissionRationale : KiwixDialog( // For the local file transfer module
      null, R.string.permission_rationale_storage, android.R.string.yes, android.R.string.cancel
  )

  data class FileTransferConfirmation(override val args: Array<out Any>) : KiwixDialog( // For the local file transfer module
      null, R.string.transfer_to, R.string.yes, android.R.string.cancel
  ), HasBodyFormatArgs {
    constructor(selectedPeerDevice: WifiP2pDevice) : this(arrayOf(selectedPeerDevice.deviceName))
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
  val args: Array<out Any>
}
