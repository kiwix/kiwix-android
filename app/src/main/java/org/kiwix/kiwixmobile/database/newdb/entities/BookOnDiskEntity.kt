package org.kiwix.kiwixmobile.database.newdb.entities

import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.converter.PropertyConverter
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book
import java.io.File

@Entity
data class BookOnDiskEntity(
  @Id var id: Long = 0,
  @Convert(converter = StringToFileConverter::class, dbType = String::class)
  val file: File = File(""),
  val bookId: String,
  val title: String,
  val description: String,
  val language: String,
  val creator: String,
  val publisher: String,
  val date: String,
  val url: String?,
  val articleCount: String?,
  val mediaCount: String?,
  val size: String,
  val name: String?,
  val favIcon: String
) {
  constructor(bookOnDisk: BookOnDisk) : this(
    0,
    bookOnDisk.file,
    bookOnDisk.book.getId(),
    bookOnDisk.book.getTitle(),
    bookOnDisk.book.getDescription(),
    bookOnDisk.book.getLanguage(),
    bookOnDisk.book.getCreator(),
    bookOnDisk.book.getPublisher(),
    bookOnDisk.book.getDate(),
    bookOnDisk.book.getUrl(),
    bookOnDisk.book.getArticleCount(),
    bookOnDisk.book.getMediaCount(),
    bookOnDisk.book.getSize(),
    bookOnDisk.book.name,
    bookOnDisk.book.getFavicon()
  )

  fun toBook() = Book().apply {
    id = bookId
    title = this@BookOnDiskEntity.title
    description = this@BookOnDiskEntity.description
    language = this@BookOnDiskEntity.language
    creator = this@BookOnDiskEntity.creator
    publisher = this@BookOnDiskEntity.publisher
    date = this@BookOnDiskEntity.date
    url = this@BookOnDiskEntity.url
    articleCount = this@BookOnDiskEntity.articleCount
    mediaCount = this@BookOnDiskEntity.mediaCount
    size = this@BookOnDiskEntity.size
    bookName = name
    favicon = favIcon
  }
}

class StringToFileConverter : PropertyConverter<File, String> {
  override fun convertToDatabaseValue(entityProperty: File?) = entityProperty?.path ?: ""

  override fun convertToEntityProperty(databaseValue: String?) = File(databaseValue ?: "")
}
