package org.kiwix.kiwixmobile.data.local.dao;/*
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

import android.content.Context;
import androidx.test.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kiwix.kiwixmobile.data.local.KiwixDatabase;
import org.kiwix.kiwixmobile.data.local.entity.BookDatabaseEntity;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class BookDaoTest {

  private Context context;
  private @Mock
  KiwixDatabase kiwixDatabase;
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
    testDir = context.getDir("testDir", Context.MODE_PRIVATE);
  }

  //TODO : test books are saved after downloading the list of available zim files

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
    if (!booksRetrieved.contains(booksToAdd.get(0))) assertEquals(0, 1);
    verify(kiwixDatabase, never()).deleteWhere(BookDatabaseEntity.class,
        BookDatabaseEntity.URL.eq(booksToAdd.get(0).file.getPath()));

    // Filename ends with .part and the file exists in memory
    if (booksRetrieved.contains(booksToAdd.get(1))) assertEquals(0, 1);
    verify(kiwixDatabase, never()).deleteWhere(BookDatabaseEntity.class,
        BookDatabaseEntity.URL.eq(booksToAdd.get(1).file.getPath()));

    // Filename ends with .zim, however only the .zim.part file exists in memory
    if (booksRetrieved.contains(booksToAdd.get(2))) assertEquals(0, 1);
    verify(kiwixDatabase, never()).deleteWhere(BookDatabaseEntity.class,
        BookDatabaseEntity.URL.eq(booksToAdd.get(2).file.getPath()));

    // Filename ends with .zim but neither the .zim, nor the .zim.part file exists in memory
    if (booksRetrieved.contains(booksToAdd.get(3))) assertEquals(0, 1);
    verify(kiwixDatabase).deleteWhere(BookDatabaseEntity.class,
        BookDatabaseEntity.URL.eq(booksToAdd.get(3).file.getPath()));

    // Filename ends with .zim and both the .zim, and the .zim.part files exists in memory
    if (!booksRetrieved.contains(booksToAdd.get(4))) assertEquals(0, 1);
    verify(kiwixDatabase, never()).deleteWhere(BookDatabaseEntity.class,
        BookDatabaseEntity.URL.eq(booksToAdd.get(4).file.getPath()));

    // If the filename ends with .zimXX

    // FileName.zimXX.part does not exist for any value of "XX" from "aa" till "dr", but FileName.zimXX exists for all "XX" from "aa', till "ds", then it does not exist
    // Also, the file inside the BooksToAdd does exist in memory
    if (!booksRetrieved.contains(booksToAdd.get(5))) assertEquals(0, 1);
    verify(kiwixDatabase, never()).deleteWhere(BookDatabaseEntity.class,
        BookDatabaseEntity.URL.eq(booksToAdd.get(5).file.getPath()));

    // FileName.zimXX.part does not exist for any value of "XX" from "aa" till "dr", but FileName.zimXX exists for all "XX" from "aa', till "ds", then it does not exist
    // Also, the file inside the BooksToAdd also not exist in memory
    if (booksRetrieved.contains(booksToAdd.get(6))) assertEquals(0, 1);
    verify(kiwixDatabase).deleteWhere(BookDatabaseEntity.class,
        BookDatabaseEntity.URL.eq(booksToAdd.get(6).file.getPath()));

    // FileName.zimXX.part exists for some "XX" between "aa" till "bl"
    // And FileName.zimXX exists for all "XX" from "aa', till "bk", and then it does not exist
    // Also, the file inside the BooksToAdd does exist in memory
    if (!booksRetrieved.contains(booksToAdd.get(7))) assertEquals(0, 1);
    verify(kiwixDatabase, never()).deleteWhere(BookDatabaseEntity.class,
        BookDatabaseEntity.URL.eq(booksToAdd.get(7).file.getPath()));

    // FileName.zimXX.part exists for some "XX" between "aa" till "bl"
    // And FileName.zimXX exists for all "XX" from "aa', till "bk", and then it does not exist
    // Also, the file inside the BooksToAdd does not exist in memory
    if (booksRetrieved.contains(booksToAdd.get(8))) assertEquals(0, 1);
    verify(kiwixDatabase).deleteWhere(BookDatabaseEntity.class,
        BookDatabaseEntity.URL.eq(booksToAdd.get(8).file.getPath()));
  }

  private ArrayList<Book> getFakeData(String baseFileName) throws IOException {
    ArrayList<Book> books = new ArrayList<>();
    for (int i = 0; i < 9; i++) {
      Book book = new Book();
      book.bookName = "Test Copy " + i;
      book.id = "Test ID " + i;
      String fileName = baseFileName + i;
      switch (i) {
        case 0:
          book.file = new File(fileName + ".zim");
          book.file.createNewFile();
          break;
        case 1:
          book.file = new File(fileName + ".part");
          book.file.createNewFile();
          break;
        case 2:
          book.file = new File(fileName + ".zim");
          File t2 = new File(fileName + ".zim.part");
          t2.createNewFile();
          break;
        case 3:
          book.file = new File(fileName + ".zim");
          break;
        case 4:
          book.file = new File(fileName + ".zim");
          book.file.createNewFile();
          File t4 = new File(fileName + ".zim.part");
          t4.createNewFile();
          break;
        case 5:
          book.file = new File(fileName + ".zimdg");
          setupCase1(fileName);
          break;
        case 6:
          book.file = new File(fileName + ".zimyr");
          setupCase2(fileName);
          break;
        case 7:
          book.file = new File(fileName + ".zimdg");
          setupCase1(fileName);
          break;
        case 8:
          book.file = new File(fileName + ".zimyr");
          setupCase2(fileName);
          break;
      }
      books.add(book);
    }
    return books;
  }

  private void setupCase1(String fileName) throws IOException {
    for (char char1 = 'a'; char1 <= 'z'; char1++) {
      for (char char2 = 'a'; char2 <= 'z'; char2++) {
        File file = new File(fileName + ".zim" + char1 + char2);
        file.createNewFile();
        if (char1 == 'd' && char2 == 'r') {
          break;
        }
      }
      if (char1 == 'd') {
        break;
      }
    }
  }

  private void setupCase2(String fileName) throws IOException {
    for (char char1 = 'a'; char1 <= 'z'; char1++) {
      for (char char2 = 'a'; char2 <= 'z'; char2++) {
        File file = new File(fileName + ".zim" + char1 + char2);
        file.createNewFile();
        if (char1 == 'd' && char2 == 'r') {
          break;
        }
      }
      if (char1 == 'd') {
        break;
      }
    }
    File t = new File(fileName + ".zimcp.part");
  }

  @After
  public void RemoveTestDirectory() {
    for (File child : testDir.listFiles()) {
      child.delete();
    }
    testDir.delete();
  }
}
