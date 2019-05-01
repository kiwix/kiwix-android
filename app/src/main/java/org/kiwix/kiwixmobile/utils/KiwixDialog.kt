package org.kiwix.kiwixmobile.utils

import org.kiwix.kiwixmobile.R

sealed class KiwixDialog(
  val title: Int?,
  val message: Int,
  val positiveMessage: Int,
  val negativeMessage: Int
) {

  object DeleteZim : KiwixDialog(
      null, R.string.delete_specific_zim, R.string.delete, R.string.no
  )

  open class YesNoDialog(
    title: Int,
    message: Int
  ) : KiwixDialog(title, message, R.string.yes, R.string.no){
    object NoWifi : YesNoDialog(
        R.string.wifi_only_title, R.string.wifi_only_msg
    )

    object StopDownload : YesNoDialog(
        R.string.confirm_stop_download_title, R.string.confirm_stop_download_msg
    )


  }



}
