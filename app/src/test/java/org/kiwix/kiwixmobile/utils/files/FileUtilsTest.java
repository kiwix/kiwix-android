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

import java.io.File;
import java.util.List;
import org.junit.Test;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book;

public class FileUtilsTest {

  //TODO : add more test cases and improve test

  @Test
  public void testGetAllZimParts(){
    String testId = "8ce5775a-10a9-bbf3-178a-9df69f23263c";
    String fileName = "/data/user/0/org.kiwix.kiwixmobile/files" + File.separator + testId;
    List<File> files;

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

    // TODO : find a way to stub the file.exist() method and add more tests
  }

}