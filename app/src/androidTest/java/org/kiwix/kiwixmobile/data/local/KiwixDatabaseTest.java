/*
 * Copyright 2016 Isaac Hutt <mhutti1@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU  General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */

package org.kiwix.kiwixmobile.data.local;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.sql.Query;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;

import static org.junit.Assert.assertArrayEquals;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class KiwixDatabaseTest {

  private final Context context;

  public KiwixDatabaseTest() {
    context = InstrumentationRegistry.getTargetContext();
  }

  @Test
  public void testMigrateDatabase() throws IOException {
    KiwixDatabase kiwixDatabase = new KiwixDatabase(context);
    kiwixDatabase.recreate();
    String testId = "8ce5775a-10a9-bbf3-178a-9df69f23263c";
    String[] testBookmarks = new String[]{"Test1", "Test2", "Test3"};
    String fileName = context.getFilesDir().getAbsolutePath() + File.separator + testId + ".txt";
    File f = new File(fileName);
    if (!f.createNewFile()) {
      throw new IOException("Unable to create file for testing migration");
    }
    Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), "utf-8"));
    for (String bookmark : testBookmarks) {
      writer.write(bookmark + "\n");
    }
    writer.close();
    kiwixDatabase.migrateBookmarksVersion6();

    ArrayList<String> bookmarkTitles = new ArrayList<>();
    try (SquidCursor<Bookmark> bookmarkCursor = kiwixDatabase.query(Bookmark.class,
        Query.selectDistinct(Bookmark.BOOKMARK_TITLE)
            .where(Bookmark.ZIM_ID.eq(testId)
                .or(Bookmark.ZIM_NAME.eq("")))
            .orderBy(Bookmark.BOOKMARK_TITLE.asc()))) {
      while (bookmarkCursor.moveToNext()) {
        bookmarkTitles.add(bookmarkCursor.get(Bookmark.BOOKMARK_TITLE));
      }
    }
    assertArrayEquals(testBookmarks, bookmarkTitles.toArray());

    // TODO Add new migration test for version 16
  }
}
