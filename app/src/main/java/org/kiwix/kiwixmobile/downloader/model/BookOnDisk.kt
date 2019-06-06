package org.kiwix.kiwixmobile.downloader.model

import org.kiwix.kiwixmobile.database.newdb.entities.BookOnDiskEntity
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book
import java.io.File
import java.util.Locale

data class BookOnDisk(
  val databaseId: Long? = null,
  val book: Book,
  val file: File
) {

  val locale: Locale by lazy {
    Locale(book.language)
  }

  constructor(bookOnDiskEntity: BookOnDiskEntity) : this(
      bookOnDiskEntity.id,
      bookOnDiskEntity.toBook(),
      bookOnDiskEntity.file
  )
}
