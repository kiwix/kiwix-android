package org.kiwix.kiwixmobile.zim_manager.fileselect_view.effects

import android.app.Activity
import android.view.ActionMode
import io.reactivex.processors.PublishProcessor
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.extensions.startActionMode
import org.kiwix.kiwixmobile.zim_manager.ZimManageViewModel.FileSelectActions
import org.kiwix.kiwixmobile.zim_manager.ZimManageViewModel.FileSelectActions.RequestDeleteMultiSelection
import org.kiwix.kiwixmobile.zim_manager.ZimManageViewModel.FileSelectActions.RequestShareMultiSelection
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter.BooksOnDiskListItem

data class StartMultiSelection(
  val bookOnDisk: BooksOnDiskListItem.BookOnDisk,
  val fileSelectActions: PublishProcessor<FileSelectActions>
) : SideEffect<ActionMode?> {
  override fun invokeWith(activity: Activity) =
    activity.startActionMode(
      R.menu.menu_zim_files_contextual,
      mapOf(
        R.id.zim_file_delete_item to { fileSelectActions.offer(RequestDeleteMultiSelection) },
        R.id.zim_file_share_item to { fileSelectActions.offer(RequestShareMultiSelection) }
      )
    ) { fileSelectActions.offer(FileSelectActions.MultiModeFinished) }
}
