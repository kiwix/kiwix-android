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

package org.kiwix.kiwixmobile.data.local;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kiwix.kiwixmobile.data.local.dao.BookDao;
import org.kiwix.kiwixmobile.data.local.entity.BookDatabaseEntity;
import org.kiwix.kiwixmobile.downloader.DownloadFragment;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyListOf;

@RunWith(PowerMockRunner.class)
@PrepareForTest(File.class)
public class BookDatabaseUnitTest {

  private Context context;
  private KiwixDatabase kiwixDatabase;
  private BookDao bookDao;
  private boolean mockInitialized = false;

  @Before
  public void executeBefore() {
    //if (!mockInitialized) {
    //  MockitoAnnotations.initMocks(this);
    //  mockInitialized = true;
    //}
    //context = InstrumentationRegistry.getTargetContext();
    //kiwixDatabase = new KiwixDatabase(context);
    //bookDao = new BookDao(kiwixDatabase);
  }

  @Test
  public void testGetBooks() throws IOException {
    //save the fake data to test
    String testId = "8ce5775a-10a9-bbf3-178a-9df69f23263c";
    String fileName = "/data/user/0/org.kiwix.kiwixmobile/files" + File.separator + testId;
    ArrayList<Book> booksRetrieved;
    ArrayList<Book> booksToAdd;

    // Set up PowerMock
    File fileZIM = PowerMockito.mock(File.class);
    File filePART = PowerMockito.mock(File.class);
    File fileTXT = PowerMockito.mock(File.class);
    File fileMP4 = PowerMockito.mock(File.class);
    File fileZIMPART = PowerMockito.mock(File.class);

    try {
      PowerMockito.whenNew(File.class).withParameterTypes(String.class).withArguments(fileName + ".zim").thenReturn(fileZIM);
      PowerMockito.whenNew(File.class).withParameterTypes(String.class).withArguments(fileName + ".part").thenReturn(filePART);
      PowerMockito.whenNew(File.class).withParameterTypes(String.class).withArguments(fileName + ".txt").thenReturn(fileTXT);
      PowerMockito.whenNew(File.class).withParameterTypes(String.class).withArguments(fileName + ".mp4").thenReturn(fileMP4);
      PowerMockito.whenNew(File.class).withParameterTypes(String.class).withArguments(fileName + ".zim.part").thenReturn(fileZIMPART);
    } catch (Exception e) {
      e.printStackTrace();
    }

    PowerMockito.when(fileZIM.getPath()).thenReturn(fileName + ".zim");
    PowerMockito.when(filePART.getPath()).thenReturn(fileName + ".zim");
    PowerMockito.when(fileTXT.getPath()).thenReturn(fileName + ".zim");
    PowerMockito.when(fileMP4.getPath()).thenReturn(fileName + ".zim");
    PowerMockito.when(fileZIMPART.getPath()).thenReturn(fileName + ".zim");

    // When no file exists
    PowerMockito.when(fileZIM.exists()).thenReturn(false);
    PowerMockito.when(filePART.exists()).thenReturn(false);
    PowerMockito.when(fileTXT.exists()).thenReturn(false);
    PowerMockito.when(fileMP4.exists()).thenReturn(false);
    PowerMockito.when(fileZIMPART.exists()).thenReturn(false);

    // Get fake data to test
    booksToAdd = getFakeData(fileName);

    // Get the filtered book list from the database (using the internal selection logic in BookDao)
    booksRetrieved = bookDao.filterBookResults(booksToAdd);

    // No book should be present in the database
    if(booksRetrieved.contains(booksToAdd.get(0))) assertEquals(".zim", 0, 1);
    if(booksRetrieved.contains(booksToAdd.get(1))) assertEquals(".zim",0, 1);
    if(booksRetrieved.contains(booksToAdd.get(2))) assertEquals(".zim",0, 1);
    if(booksRetrieved.contains(booksToAdd.get(3))) assertEquals(".zim",0, 1);
    if(booksRetrieved.contains(booksToAdd.get(4))) assertEquals(".zim",0, 1);

    // When the files exist
    PowerMockito.when(fileZIM.exists()).thenReturn(true);
    PowerMockito.when(filePART.exists()).thenReturn(true);
    PowerMockito.when(fileTXT.exists()).thenReturn(true);
    PowerMockito.when(fileMP4.exists()).thenReturn(true);
    PowerMockito.when(fileZIMPART.exists()).thenReturn(true);

    // Get fake data to test
    booksToAdd = getFakeData(fileName);

    // Get the filtered book list from the database (using the internal selection logic in BookDao)
    booksRetrieved = bookDao.filterBookResults(booksToAdd);

    // No book should be present in the database
    if(booksRetrieved.contains(booksToAdd.get(0))) assertEquals(".zim", 0, 1);
    if(booksRetrieved.contains(booksToAdd.get(1))) assertEquals(".zim",0, 1);
    if(booksRetrieved.contains(booksToAdd.get(2))) assertEquals(".zim",0, 1);
    if(booksRetrieved.contains(booksToAdd.get(3))) assertEquals(".zim",0, 1);
    if(booksRetrieved.contains(booksToAdd.get(4))) assertEquals(".zim",0, 1);
  }

  private ArrayList<Book> getFakeData(String baseFileName) {
    ArrayList<Book> books = new ArrayList<>();
    for(int i = 0; i < 5; i++){
      Book book = new Book();
      book.bookName = "Test Copy " + Integer.toString(i);
      book.id = "Test ID " + Integer.toString(i);
      String fileName = baseFileName + Integer.toString(i);
      switch (i) {
        case 0: book.file = new File(fileName + ".zim"); break;
        case 1: book.file = new File(fileName + ".part"); break;
        case 2: book.file = new File(fileName + ".txt"); break;
        case 3: book.file = new File(fileName + ".mp4"); break;
        case 4: book.file = new File(fileName + ".zim.part"); break;
      }
      books.add(book);
    }
    return books;
  }
}
