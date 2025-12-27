package org.kiwix.kiwixmobile.update

data class UpdateStates(
  val apkVersion: AppVersion = AppVersion(
    name = "",
    version = "",
    apkUrl = ""
  ),
  val loading: Boolean = false,
  val success: Boolean = false,
  val dialogShown: Boolean? = null,
)
