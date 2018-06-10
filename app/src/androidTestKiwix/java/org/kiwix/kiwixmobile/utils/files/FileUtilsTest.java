/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.kiwix.kiwixmobile.utils.files;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import java.io.File;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FileUtilsTest {

  private boolean mockInitialized = false;
  private Context context;
  //private FileUtils fileUtils;

  @Before
  public void executeBefore(){
    if (!mockInitialized) {
      MockitoAnnotations.initMocks(this);
      mockInitialized = true;
    }
    context = InstrumentationRegistry.getTargetContext();
    //fileUtils = new FileUtils();
  }

  @Test
  public void testGetAllZimParts(){
    String testId = "8ce5775a-10a9-bbf3-178a-9df69f23263c";
    String fileName = context.getFilesDir().getAbsolutePath() + File.separator + testId;
    List<File> files;

    //Test-1 (filename ends with .zim)
    Book book1 = new Book();
    book1.file = new File(fileName + ".zim");
    files = FileUtils.getAllZimParts(book1);
    assertEquals(1, files.size());
    assertEquals(book1.file.getPath() + ".part", files.get(0).getPath());

    //Test-2 (filename ends with .zim.part)
    Book book2 = new Book();
    book2.file = new File(fileName + ".zim.part");
    files = FileUtils.getAllZimParts(book2);
    assertEquals(1, files.size());
    assertEquals(book2.file.getPath() + ".part", files.get(0).getPath());

    //Test-3 (filename ends with .zimXX)
    Book book3 = new Book();
    book3.file = new File(fileName + ".zimab");
    files = FileUtils.getAllZimParts(book3);
    assertEquals(0, files.size());

    //TODO : find a way to stub the file.exist() method and add more tests
  }

}