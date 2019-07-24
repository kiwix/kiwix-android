package org.kiwix.kiwixmobile.bookmark

import org.kiwix.kiwixmobile.data.ZimContentProvider
import org.kiwix.kiwixmobile.database.newdb.entities.BookmarkEntity

data class BookmarkItem(
  val databaseId: Long = 0L,
  val zimId: String,
  val zimName: String,
  val zimFilePath: String,
  val bookmarkUrl: String,
  val bookmarkTitle: String,
  val favicon: String
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

  companion object {
    @JvmStatic fun fromZimContentProvider(
      title: String,
      url: String
    ) = BookmarkItem(
      zimId = ZimContentProvider.getId(),
      zimName = ZimContentProvider.getName(),
      zimFilePath = ZimContentProvider.getZimFile(),
      bookmarkUrl = url,
      bookmarkTitle = title,
      favicon = ZimContentProvider.getFavicon()
    )
  }
}
