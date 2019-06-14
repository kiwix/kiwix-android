package org.kiwix.kiwixmobile.downloader.model

import org.kiwix.kiwixmobile.database.newdb.entities.BookOnDiskEntity
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book
import java.io.File

data class BookOnDisk(
  val databaseId: Long? = null,
  val book: Book = Book().apply { id = "" },
  val file: File = File("")
) {
  constructor(bookOnDiskEntity: BookOnDiskEntity) : this(
      bookOnDiskEntity.id,
      bookOnDiskEntity.toBook(),
      bookOnDiskEntity.file
  )
}