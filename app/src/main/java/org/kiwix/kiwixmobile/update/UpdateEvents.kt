package org.kiwix.kiwixmobile.update

sealed class UpdateEvents {
  data class DownloadApp(val url: String) : UpdateEvents()
  data object CancelDownload : UpdateEvents()
  data object RetrieveLatestAppVersion : UpdateEvents()
}
