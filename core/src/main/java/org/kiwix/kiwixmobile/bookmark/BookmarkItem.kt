package org.kiwix.kiwixmobile.bookmark

import org.kiwix.kiwixmobile.database.newdb.entities.BookmarkEntity
import org.kiwix.kiwixmobile.zim_manager.ZimFileReader

data class BookmarkItem(
  val databaseId: Long = 0L,
  val zimId: String,
  val zimName: String,
  val zimFilePath: String?,
  val bookmarkUrl: String,
  val bookmarkTitle: String,
  val favicon: String?
) {
  constructor(entity: BookmarkEntity) : this(
    entity.id,
    entity.zimId,
    entity.zimName,
    entity.zimFilePath,
    entity.bookmarkUrl,
    entity.bookmarkTitle,
    entity.favicon
  )

  constructor(
    title: String,
    url: String,
    zimFileReader: ZimFileReader
  ) : this(
    zimId = zimFileReader.id,
    zimName = zimFileReader.name,
    zimFilePath = zimFileReader.zimFile.canonicalPath,
    bookmarkUrl = url,
    bookmarkTitle = title,
    favicon = zimFileReader.favicon
  )
}
