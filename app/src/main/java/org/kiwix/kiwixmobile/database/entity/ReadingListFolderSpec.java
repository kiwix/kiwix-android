package org.kiwix.kiwixmobile.database.entity;

/**
 * Created by EladKeyshawn on 04/04/2017.
 */

import com.yahoo.squidb.annotations.ColumnSpec;
import com.yahoo.squidb.annotations.TableModelSpec;

/**
 * Squidb spec for saved bookmarks.
 */
@TableModelSpec(className = "ReadingListFolders", tableName = "ReadingListFolders")
public class ReadingListFolderSpec {

    @ColumnSpec(constraints = "NOT NULL")
    public String folderTitle;
    public int articleCount;

}