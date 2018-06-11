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

import java.io.File;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(File.class)
public class FileUtilsTest {

  @Before
  public void executeBefore() throws Exception {


    // TODO : fix this code with powermock and add cases in testGetAllZimParts when file.exists()returns true
  }

  @Test
  public void testGetAllZimParts() throws Exception {

    String testId = "8ce5775a-10a9-bbf3-178a-9df69f23263c";
    String fileName = "/data/user/0/org.kiwix.kiwixmobile/files" + File.separator + testId;
    List<File> files;

    // Set up PowerMockito
    File myFile = PowerMockito.mock(File.class);
    PowerMockito.whenNew(File.class).withParameterTypes(String.class).withArguments("sid").thenReturn(myFile);
    PowerMockito.when(myFile.getPath()).thenReturn(fileName);
    PowerMockito.when(myFile.exists()).thenReturn(false);

    // Filename ends with .zim
    Book book1 = new Book();
    book1.file = new File(fileName + ".zim");
    files = FileUtils.getAllZimParts(book1);
    assertEquals("Only a single book is returned in case the file has extension .zim", 1, files.size());
    assertEquals("The filename is appended with .part", book1.file.getPath() + ".part", files.get(0).getPath());

    // Filename ends with .zim.part
    Book book2 = new Book();
    book2.file = new File(fileName + ".zim.part");
    files = FileUtils.getAllZimParts(book2);
    assertEquals("Only a single book is returned in case the file has extension .zim.part", 1, files.size());
    assertEquals("The filename is appended with .part", book2.file.getPath() + ".part", files.get(0).getPath());

    // Filename ends with .zimXX
    Book book3 = new Book();
    book3.file = new File(fileName + ".zimab");
    files = FileUtils.getAllZimParts(book3);
    assertEquals("Return no files in case no file exists at both 'path', and 'path.part'", 0, files.size());
  }
}
// TODO : test deleteZimFile and getLocalFilePathByUrl