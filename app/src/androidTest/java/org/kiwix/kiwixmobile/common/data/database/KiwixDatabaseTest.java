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

package org.kiwix.kiwixmobile.common.data.database;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

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

  private Context mContext;

  public KiwixDatabaseTest (){
    mContext = InstrumentationRegistry.getTargetContext();
  }

  @Test
  public void testMigrateDatabase() throws IOException {
    KiwixDatabase kiwixDatabase = KiwixDatabase.getInstance(mContext);
    kiwixDatabase.recreate();
    String testId = "8ce5775a-10a9-bbf3-178a-9df69f23263c";
    String[] testBookmarks = new String[] {"Test1","Test2","Test3"};
    String fileName = mContext.getFilesDir().getAbsolutePath() + File.separator + testId + ".txt";
    File f = new File(fileName);
    f.createNewFile();
    Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), "utf-8"));
    for (String bookmark : testBookmarks){
      writer.write(bookmark + "\n");
    }
    writer.close();

    kiwixDatabase.migrateBookmarks();
    BookmarksDao bookmarksDao = new BookmarksDao(kiwixDatabase);
    ArrayList<String> b = bookmarksDao.getBookmarkTitles(testId, "");
    assertArrayEquals(testBookmarks,b.toArray());

  }
}
