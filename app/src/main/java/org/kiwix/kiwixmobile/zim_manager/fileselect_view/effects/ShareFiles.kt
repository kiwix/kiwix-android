package org.kiwix.kiwixmobile.zim_manager.fileselect_view.effects

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import org.kiwix.kiwixmobile.BuildConfig
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk

class ShareFiles(private val selectedBooks: List<BookOnDisk>) : SideEffect<Unit> {
  override fun invokeWith(activity: Activity) {
    val selectedFileShareIntent = Intent()
    selectedFileShareIntent.action = Intent.ACTION_SEND_MULTIPLE
    selectedFileShareIntent.type = "application/octet-stream"
    val selectedFileContentURIs = selectedBooks.mapNotNull {
      if (Build.VERSION.SDK_INT >= 24) {
        FileProvider.getUriForFile(
          activity,
          BuildConfig.APPLICATION_ID + ".fileprovider",
          it.file
        )
      } else {
        Uri.fromFile(it.file)
      }
    }
    selectedFileShareIntent.putParcelableArrayListExtra(
      Intent.EXTRA_STREAM,
      ArrayList(selectedFileContentURIs)
    )
    selectedFileShareIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    val shareChooserIntent = Intent.createChooser(
      selectedFileShareIntent,
      activity.getString(R.string.selected_file_cab_app_chooser_title)
    )
    if (shareChooserIntent.resolveActivity(activity.packageManager) != null) {
      activity.startActivity(shareChooserIntent) // Open the app chooser dialog
    }
  }
}
