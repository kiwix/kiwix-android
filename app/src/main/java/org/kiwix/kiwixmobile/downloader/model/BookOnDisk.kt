package org.kiwix.kiwixmobile.downloader.model

import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book
import java.io.File

data class BookOnDisk(
  val databaseId: Long? = null,
  val book: Book,
  val file: File
)
