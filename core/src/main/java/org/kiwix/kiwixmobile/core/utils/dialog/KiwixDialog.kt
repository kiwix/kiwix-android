/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.utils.dialog

import android.app.Activity
import android.view.View
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.main.CoreMainActivity

@Suppress("LongParameterList")
sealed class KiwixDialog(
  val title: Int?,
  val message: Int?,
  val positiveMessage: Int,
  val negativeMessage: Int?,
  val cancelable: Boolean = true,
  val icon: Int? = null,
  val neutralMessage: Int? = null,
  val getView: (() -> View)? = null
) {

  data class DeleteZims(override val args: List<Any>) :
    KiwixDialog(
      null, R.string.delete_zim_body, R.string.delete, R.string.no
    ),
    HasBodyFormatArgs {
    constructor(zimNameList: String) : this(listOf(zimNameList))
  }

  object LocationPermissionRationale : KiwixDialog(
    null,
    R.string.permission_rationale_location,
    android.R.string.ok,
    android.R.string.cancel
  )

  object NearbyWifiPermissionRationale : KiwixDialog(
    null,
    R.string.permission_rationale_nearby,
    android.R.string.ok,
    android.R.string.cancel
  )

  object StoragePermissionRationale : KiwixDialog(
    null,
    R.string.request_storage,
    android.R.string.ok,
    android.R.string.cancel
  )

  object WriteStoragePermissionRationale : KiwixDialog(
    null,
    R.string.request_write_storage,
    android.R.string.ok,
    android.R.string.cancel
  )

  object NotificationPermissionDialog : KiwixDialog(
    null,
    R.string.request_notification_permission_message,
    android.R.string.ok,
    android.R.string.cancel
  )

  object EnableWifiP2pServices : KiwixDialog(
    null, R.string.request_enable_wifi, R.string.yes, android.R.string.cancel
  )

  object EnableLocationServices : KiwixDialog(
    null, R.string.request_enable_location, R.string.yes, android.R.string.cancel
  )

  object TurnOffHotspotManually : KiwixDialog(
    R.string.hotspot_failed_title,
    R.string.hotspot_failed_message,
    R.string.go_to_wifi_settings_label,
    null
  )

  object ReadPermissionRequired : KiwixDialog(
    R.string.storage_permission_denied,
    R.string.grant_read_storage_permission,
    R.string.go_to_settings_label,
    null,
    cancelable = false
  )

  object ManageExternalFilesPermissionDialog : KiwixDialog(
    R.string.all_files_permission_needed,
    R.string.all_files_permission_needed_message,
    R.string.yes,
    R.string.no,
    cancelable = false
  )

  data object CopyMoveFileToPublicDirectoryDialog : KiwixDialog(
    null,
    R.string.copy_move_files_dialog_description,
    R.string.action_copy,
    R.string.move,
    neutralMessage = R.string.cancel,
    cancelable = false
  )

  data object ShowWarningAboutSplittedZimFile : KiwixDialog(
    R.string.verify_zim_chunk_dialog_title,
    R.string.verify_zim_chunks_dialog_message,
    android.R.string.ok,
    null,
    cancelable = false
  )

  object SaveOrOpenUnsupportedFiles : KiwixDialog(
    R.string.save_or_open_unsupported_files_dialog_title,
    R.string.save_or_open_unsupported_files_dialog_message,
    R.string.open,
    R.string.save,
    neutralMessage = R.string.no_thanks
  )

  data class ShowHotspotDetails(override val args: List<Any>) :
    KiwixDialog(
      R.string.hotspot_turned_on,
      R.string.hotspot_details_message,
      android.R.string.ok,
      null
    ),
    HasBodyFormatArgs {
    constructor(ssid: String, preSharedKey: String) : this(
      listOf(ssid, preSharedKey)
    )
  }

  object StartHotspotManually : KiwixDialog(
    R.string.hotspot_dialog_title,
    R.string.hotspot_dialog_message,
    R.string.go_to_settings,
    R.string.go_to_wifi_settings_label
  )

  object WiFiOnWhenHostingBooks : KiwixDialog(
    R.string.wifi_dialog_title,
    R.string.wifi_dialog_body,
    positiveMessage = R.string.go_to_wifi_settings_label,
    negativeMessage = null,
    neutralMessage = R.string.hotspot_dialog_neutral_button
  )

  data class FileTransferConfirmation(override val args: List<Any>) :
    KiwixDialog(
      null, R.string.transfer_to, R.string.yes, android.R.string.cancel
    ),
    HasBodyFormatArgs {
    constructor(selectedPeerDeviceName: String) : this(listOf(selectedPeerDeviceName))
  }

  object DeleteSearch : KiwixDialog(
    null, R.string.delete_recent_search_item, R.string.delete, R.string.no
  )

  object ContentsDrawerHint : KiwixDialog(
    R.string.did_you_know,
    R.string.hint_contents_drawer_message,
    R.string.got_it,
    null
  )

  object ExternalLinkPopup : KiwixDialog(
    R.string.external_link_popup_dialog_title,
    R.string.external_link_popup_dialog_message,
    R.string.yes,
    R.string.no,
    neutralMessage = R.string.do_not_ask_anymore
  )

  data class ShowRate(override val args: List<Any>, val customIcon: Int?) :
    KiwixDialog(
      R.string.rate_dialog_title,
      R.string.single_arg_format_string,
      R.string.rate_dialog_positive,
      R.string.no_thanks,
      icon = customIcon,
      neutralMessage = R.string.rate_dialog_neutral
    ),
    HasBodyFormatArgs {
    constructor(icon: Int?, activity: Activity) : this(
      listOf(
        String.format(
          activity.getString(R.string.rate_dialog_msg),
          (activity as CoreMainActivity).appName
        )
      ),
      icon
    )
  }

  object ClearAllHistory : KiwixDialog(
    R.string.clear_all_history_dialog_title,
    R.string.clear_recent_and_tabs_history_dialog,
    positiveMessage = R.string.delete,
    negativeMessage = R.string.cancel
  )

  object ClearAllNavigationHistory : KiwixDialog(
    R.string.clear_all_history_dialog_title,
    R.string.clear_all_navigation_history_message,
    positiveMessage = R.string.delete,
    negativeMessage = R.string.cancel
  )

  object ClearAllNotes : KiwixDialog(
    R.string.delete_notes_confirmation_msg,
    message = null,
    positiveMessage = R.string.delete,
    negativeMessage = R.string.cancel
  )

  data class OpenCredits(val customGetView: (() -> View)?) : KiwixDialog(
    null,
    null,
    android.R.string.ok,
    null,
    getView = customGetView
  )

  data class StartServer(val customGetView: (() -> View)?) : KiwixDialog(
    R.string.progress_dialog_starting_server,
    null,
    R.string.empty_string,
    null,
    getView = customGetView
  )

  data class PreparingCopyingFilesDialog(val customGetView: (() -> View)?) : KiwixDialog(
    R.string.preparing_file_for_copy,
    null,
    R.string.empty_string,
    null,
    cancelable = false,
    getView = customGetView
  )

  object NotesDiscardConfirmation : KiwixDialog(
    null,
    R.string.confirmation_alert_dialog_message,
    R.string.yes,
    android.R.string.cancel
  )

  object DownloadTTSLanguage : KiwixDialog(
    R.string.download_tts_language_title,
    R.string.download_tts_language_message,
    R.string.download,
    android.R.string.cancel
  )

  open class YesNoDialog(
    title: Int?,
    message: Int
  ) : KiwixDialog(title, message, R.string.yes, R.string.no) {
    object StopDownload : YesNoDialog(
      R.string.confirm_stop_download_title, R.string.confirm_stop_download_msg
    )

    object WifiOnly : YesNoDialog(
      R.string.wifi_only_title, R.string.wifi_only_msg
    )

    object OpenInNewTab : YesNoDialog(
      null, R.string.open_in_new_tab
    )

    object ExportBookmarks : YesNoDialog(
      R.string.export_all_bookmarks_dialog_title,
      message = R.string.export_all_bookmarks_dialog_message,
    )
  }

  object ImportBookmarks : KiwixDialog(
    R.string.import_bookmarks_dialog_title,
    message = null,
    positiveMessage = R.string.yes,
    negativeMessage = R.string.no
  )

  object DeleteSelectedHistory : KiwixDialog(
    R.string.delete_selected_history,
    null,
    positiveMessage = R.string.delete,
    negativeMessage = R.string.cancel
  )

  object DeleteAllHistory : KiwixDialog(
    R.string.delete_history,
    null,
    positiveMessage = R.string.delete,
    negativeMessage = R.string.cancel
  )

  object DeleteAllBookmarks : KiwixDialog(
    R.string.delete_bookmarks,
    null,
    positiveMessage = R.string.delete,
    negativeMessage = R.string.cancel
  )

  object DeleteSelectedBookmarks : KiwixDialog(
    R.string.delete_selected_bookmarks,
    null,
    positiveMessage = R.string.delete,
    negativeMessage = R.string.cancel
  )

  object DeleteAllNotes : KiwixDialog(
    R.string.delete_notes_confirmation_msg,
    R.string.delete_note_dialog_message,
    positiveMessage = R.string.delete,
    negativeMessage = R.string.cancel
  )

  object DeleteSelectedNotes : KiwixDialog(
    R.string.delete_selected_notes,
    R.string.delete_note_dialog_message,
    positiveMessage = R.string.delete,
    negativeMessage = R.string.cancel
  )

  object ShowNoteDialog : KiwixDialog(
    R.string.choose_your_action,
    null,
    positiveMessage = R.string.open_article,
    negativeMessage = R.string.open_note
  )
}

interface HasBodyFormatArgs {
  val args: List<Any>
}
