package org.kiwix.kiwixmobile.zim_manager.fileselect_view.effects

import android.app.Activity
import org.kiwix.kiwixmobile.R.string
import org.kiwix.kiwixmobile.database.newdb.dao.NewBookDao
import org.kiwix.kiwixmobile.extensions.toast
import org.kiwix.kiwixmobile.utils.DialogShower
import org.kiwix.kiwixmobile.utils.KiwixDialog.DeleteZim
import org.kiwix.kiwixmobile.utils.files.FileUtils
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk
import javax.inject.Inject

class DeleteFiles(private val booksOnDiskListItem: List<BookOnDisk>) :
  SideEffect<Unit> {

  @Inject lateinit var dialogShower: DialogShower
  @Inject lateinit var newBookDao: NewBookDao

  override fun invokeWith(activity: Activity) {
    activityComponent(activity).inject(this)
    booksOnDiskListItem.forEach {
      dialogShower.show(DeleteZim(it), {
        if (deleteSpecificZimFile(it)) {
          activity.toast(string.delete_specific_zim_toast)
        } else {
          activity.toast(string.delete_zim_failed)
        }
      })
    }
  }

  private fun deleteSpecificZimFile(book: BookOnDisk): Boolean {
    val file = book.file
    FileUtils.deleteZimFile(file.path)
    if (file.exists()) {
      return false
    }
    newBookDao.delete(book.databaseId!!)
    return true
  }
}
