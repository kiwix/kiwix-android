package org.kiwix.kiwixmobile.database.entity;

import com.yahoo.squidb.annotations.ColumnSpec;
import com.yahoo.squidb.annotations.TableModelSpec;

/**
 * Squidb spec for saved bookmarks.
 */
@TableModelSpec(className = "Bookmarks", tableName = "Bookmarks")
public class BookmarksSpec {

  @ColumnSpec(constraints = "NOT NULL")
  public String ZimId;
  public String ZimName;
  public String bookmarkUrl;
  public String bookmarkTitle;
  public String parentReadinglist;

}
