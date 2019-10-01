package org.kiwix.kiwixmobile.zim_manager.fileselect_view.effects

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import org.kiwix.kiwixmobile.BuildConfig
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.zim_manager.local_file_transfer.LocalFileTransferActivity

class SendFiles(private val selectedBooks: List<BookOnDisk>) : SideEffect<Unit> {
  override fun invokeWith(activity: Activity) {
    val selectedFileShareIntent =
      Intent(activity, LocalFileTransferActivity::class.java)

    val selectedFileContentURIs = selectedBooks.getSelectedFileUris(activity)

    selectedFileShareIntent.putParcelableArrayListExtra(
      LocalFileTransferActivity.FILE_URIS,
      ArrayList(selectedFileContentURIs)
    )

    activity.startActivity(selectedFileShareIntent)
  }

  private fun List<BookOnDisk>.getSelectedFileUris(activity: Activity): List<Uri> {
    return mapNotNull {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        FileProvider.getUriForFile(
          activity,
          BuildConfig.APPLICATION_ID + ".fileprovider",
          it.file
        )
      } else {
        Uri.fromFile(it.file)
      }
    }
  }
}
