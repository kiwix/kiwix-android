package org.kiwix.kiwixmobile.database.newdb.dao

import io.objectbox.Box
import io.objectbox.kotlin.inValues
import io.objectbox.kotlin.query
import org.kiwix.kiwixmobile.database.newdb.entities.DownloadEntity
import org.kiwix.kiwixmobile.database.newdb.entities.DownloadEntity_
import org.kiwix.kiwixmobile.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book
import javax.inject.Inject

class NewDownloadDao @Inject constructor(private val box: Box<DownloadEntity>) {

  fun downloads() = box.asFlowable()
    .map { it.map(DownloadEntity::toDownloadModel) }

  fun delete(vararg downloadIds: Long) {
    box
      .query {
        inValues(DownloadEntity_.downloadId, downloadIds)
      }
      .remove()
  }

  fun containsAny(vararg downloadIds: Long) =
    box
      .query {
        inValues(DownloadEntity_.downloadId, downloadIds)
      }
      .count() > 0

  fun doesNotAlreadyExist(book: Book) =
    box
      .query {
        equal(DownloadEntity_.bookId, book.id)
      }
      .count() == 0L

  fun insert(downloadModel: DownloadModel) {
    box.put(DownloadEntity(downloadModel))
  }
}
