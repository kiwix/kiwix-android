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
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kiwix.kiwixmobile.data.local.dao.BookDao;
import org.kiwix.kiwixmobile.data.local.entity.BookDatabaseEntity;
import org.kiwix.kiwixmobile.downloader.DownloadFragment;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyListOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(AndroidJUnit4.class)
public class BookDatabaseTest {

  private Context context;
  private @Mock KiwixDatabase kiwixDatabase;
  private BookDao bookDao;
  private boolean mockInitialized = false;
  private File testDir;

  @Before
  public void executeBefore() {
    if (!mockInitialized) {
      MockitoAnnotations.initMocks(this);
      mockInitialized = true;
    }
    context = InstrumentationRegistry.getTargetContext();
    //kiwixDatabase = new KiwixDatabase(context);
    bookDao = new BookDao(kiwixDatabase);

    // Create a temporary directory where all the test files will be saved
    testDir = context.getDir("testDir", context.MODE_PRIVATE);
  }

  //TODO : test books are saved in the Database after download
  //TODO : test books are saved after downloading the list of available zim files
  //TODO : test book is deleted from database on deleting a specific zim file

  @Test
  public void testGetBooks() throws IOException {
    // Save the fake data to test
    String testId = "6qq5301d-2cr0-ebg5-474h-6db70j52864p";
    String fileName = testDir.getPath() + "/" + testId + "testFile";
    ArrayList<Book> booksToAdd = getFakeData(fileName);

    // Set up the mocks
    when(kiwixDatabase.deleteWhere(any(), any())).thenReturn(0);

    // Get the filtered book list from the database (using the internal selection logic in BookDao)
    ArrayList<Book> booksRetrieved = bookDao.filterBookResults(booksToAdd);

    // Test whether the correct books are returned

    // Filename ends with .zim and the file exists in memory
    if(!booksRetrieved.contains(booksToAdd.get(0))) assertEquals(0, 1);
    verify(kiwixDatabase, never()).deleteWhere(BookDatabaseEntity.class, BookDatabaseEntity.URL.eq(booksToAdd.get(0).file.getPath()));

    // Filename ends with .part and the file exists in memory
    if(booksRetrieved.contains(booksToAdd.get(1))) assertEquals(0, 1);
    verify(kiwixDatabase, never()).deleteWhere(BookDatabaseEntity.class, BookDatabaseEntity.URL.eq(booksToAdd.get(1).file.getPath()));

    // Filename ends with .zim, however only the .zim.part file exists in memory
    if(booksRetrieved.contains(booksToAdd.get(2))) assertEquals(0, 1);
    verify(kiwixDatabase, never()).deleteWhere(BookDatabaseEntity.class, BookDatabaseEntity.URL.eq(booksToAdd.get(2).file.getPath()));

    // Filename ends with .zim but neither the .zim, nor the .zim.part file exists in memory
    if(booksRetrieved.contains(booksToAdd.get(3))) assertEquals(0, 1);
    verify(kiwixDatabase).deleteWhere(BookDatabaseEntity.class, BookDatabaseEntity.URL.eq(booksToAdd.get(3).file.getPath()));

    // Filename ends with .zim and both the .zim, and the .zim.part files exists in memory
    if(booksRetrieved.contains(booksToAdd.get(3))) assertEquals(0, 1);

    // If the filename ends with .zimXX, then the file is not included unless the file exists and ..
    if(!booksRetrieved.contains(booksToAdd.get(4))) assertEquals(".zimXX",
        0, 1);
    if(!booksRetrieved.contains(booksToAdd.get(5))) assertEquals(".zimXX",
        0, 1);
  }

  private ArrayList<Book> getFakeData(String baseFileName) throws IOException {
    ArrayList<Book> books = new ArrayList<>();
    for(int i = 0; i < 7; i++){
      Book book = new Book();
      book.bookName = "Test Copy " + Integer.toString(i);
      book.id = "Test ID " + Integer.toString(i);
      String fileName = baseFileName + Integer.toString(i);
      switch (i) {
        case 0: book.file = new File(fileName + Integer.toString(i) + ".zim"); book.file.createNewFile(); break;
        case 1: book.file = new File(fileName + Integer.toString(i) + ".part"); book.file.createNewFile(); break;
        case 2: book.file = new File(fileName + Integer.toString(i) + ".zim");
                File t2 = new File(fileName + Integer.toString(i) + ".zim.part"); t2.createNewFile(); break;
        case 3: book.file = new File(fileName + Integer.toString(i) + ".zim"); break;
        case 4: book.file = new File(fileName + Integer.toString(i) + ".zim"); book.file.createNewFile();
                File t4 = new File(fileName + Integer.toString(i) + ".zim.part"); t4.createNewFile(); break;
        case 5: book.file = new File(fileName + Integer.toString(i) + ".zimcj"); book.file.createNewFile(); break;
        case 6: book.file = new File(fileName + Integer.toString(i) + ".zimcj");
                File t6 = new File(fileName + Integer.toString(i) + ".zimcj.part"); t6.createNewFile(); break;
      }
      books.add(book);
    }
    return books;
  }

  @After
  public void RemoveTestDirectory() {
    for(File child : testDir.listFiles()) {
      child.delete();
    }
    testDir.delete();
  }
}
