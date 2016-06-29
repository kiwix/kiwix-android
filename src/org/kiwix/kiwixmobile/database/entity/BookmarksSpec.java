package org.kiwix.kiwixmobile.database.entity;

import com.yahoo.squidb.annotations.ColumnSpec;
import com.yahoo.squidb.annotations.TableModelSpec;

/**
 * Squidb spec for recent searches.
 */
@TableModelSpec(className = "Bookmarks", tableName = "Bookmarks")
public class BookmarksSpec {

  @ColumnSpec(constraints = "NOT NULL")
  public String bookmarkStr;

}
