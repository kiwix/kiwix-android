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

  @Test
  public void testGetAllZimParts() throws Exception {

    // set Constants
    String testId = "8ce5775a-10a9-bbf3-178a-9df69f23263c";
    String fileName = "/data/user/0/org.kiwix.kiwixmobile/files" + File.separator + testId;
    List<File> files;

    // Set up PowerMockito
    File myFile = PowerMockito.mock(File.class);
    PowerMockito.whenNew(File.class).withParameterTypes(String.class).withArguments(fileName).thenReturn(myFile);

    Book testBook = new Book();
    testBook.file = new File(fileName);

    // Filename ends with .zim and file does not exist at the location
    PowerMockito.when(myFile.getPath()).thenReturn(fileName + ".zim");
    PowerMockito.when(myFile.exists()).thenReturn(false);
    //PowerMockito.when(myFile.toString()).thenReturn(myFile.getPath());

    files = FileUtils.getAllZimParts(testBook);
    assertEquals("Only a single book is returned in case the file has extension .zim", 1, files.size());
    assertEquals("The filename is appended with .part", testBook.file + ".part", files.get(0).getPath());

    // Filename ends with .zim and file exists at the location
    PowerMockito.when(myFile.getPath()).thenReturn(fileName + ".zim");
    PowerMockito.when(myFile.exists()).thenReturn(true);

    files = FileUtils.getAllZimParts(testBook);
    assertEquals("Only a single book is returned in case the file has extension .zim", 1, files.size());
    assertEquals("The filename retained as such", testBook.file.getPath(), files.get(0).getPath());

    // Filename ends with .zim.part and file does not exist at the location
    PowerMockito.when(myFile.getPath()).thenReturn(fileName + ".zim.part");
    PowerMockito.when(myFile.exists()).thenReturn(false);

    files = FileUtils.getAllZimParts(testBook);
    assertEquals("Only a single book is returned in case the file has extension .zim", 1, files.size());
    assertEquals("The filename is appended with .part", testBook.file + ".part", files.get(0).getPath());

    // Filename ends with .zim.part and file exists at the location
    PowerMockito.when(myFile.getPath()).thenReturn(fileName + ".zim.part");
    PowerMockito.when(myFile.exists()).thenReturn(true);

    files = FileUtils.getAllZimParts(testBook);
    assertEquals("Only a single book is returned in case the file has extension .zim", 1, files.size());
    assertEquals("The filename retained as such", testBook.file.getPath(), files.get(0).getPath());
    }
}
// TODO : test deleteZimFile and getLocalFilePathByUrl