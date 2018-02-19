package org.kiwix.kiwixmobile.common.data.database.entity;

import com.yahoo.squidb.annotations.ColumnSpec;
import com.yahoo.squidb.annotations.TableModelSpec;

/**
 * Squidb spec for recent searches.
 */
@TableModelSpec(className = "RecentSearch", tableName = "recentSearches")
public class RecentSearchSpec {

  @ColumnSpec(constraints = "NOT NULL")
  public String searchString;

  @ColumnSpec(constraints = "NOT NULL")
  public String zimID;


}
