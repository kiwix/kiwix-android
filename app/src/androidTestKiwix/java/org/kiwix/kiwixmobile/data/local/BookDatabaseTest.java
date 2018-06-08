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
import static org.mockito.Mockito.when;


@RunWith(AndroidJUnit4.class)
public class BookDatabaseTest {

  private Context context;
  private KiwixDatabase kiwixDatabase;
  private BookDao bookDao;
  private boolean mockInitialized = false;

  @Before
  public void executeBefore(){
    if (!mockInitialized) {
      MockitoAnnotations.initMocks(this);
      mockInitialized = true;
    }

    context = InstrumentationRegistry.getTargetContext();
    kiwixDatabase = new KiwixDatabase(context);
    //when(kiwixDatabase.deleteWhere(any(),any())).thenReturn(0);
    bookDao = new BookDao(kiwixDatabase);
    //when(bookDao.mDb.deleteWhere(any(),any())).thenReturn(0);

  //set methods for the mocked DAO class



  }

  //TODO : test books are saved in the Database after download
  //@Test
  //public void testBooksSavedInDatabaseAfterDownload() {
  //  DownloadFragment downloadFragment = new DownloadFragment();
  //}

  //TODO : test books are saved after downloading the list of available zim files
  //@Test
  //public void testBooksSavedAfterDownloadingListOfAvailableZimFiles(){
  //
  //}


  //TODO : test book is deleted from database on deleting a specific zim file

  //TODO : test the getBooks() method
  @Test
  public void testGetBooks() throws IOException{

    //save the fake data to test
    String testId = "8ce5775a-10a9-bbf3-178a-9df69f23263c";
    String fileName = context.getFilesDir().getAbsolutePath() + File.separator + testId;
    ArrayList<Book> booksToAdd = getFakeData(fileName);
    for(Book book : booksToAdd){
      saveBookToDatabase(book);
    }

    //retrieve the fake data
    ArrayList<Book> booksRetrieved = bookDao.getBooks();
    if(false) ;
    //test whether the correct books are returned
    Book book_1 = booksToAdd.get(0);
    Book book_2 = booksToAdd.get(1);
    Book book_3 = booksToAdd.get(2);
    Book book_4 = booksToAdd.get(3);
    Book book_5 = booksToAdd.get(4);

    //assertEquals("",book_1.file.getPath());
    Log.d("this is siddharth", booksRetrieved.toString());
    //if(!booksRetrieved.contains(book_1) || !booksRetrieved.contains(book_4)) {
    //  assertEquals("not saving files with .zim extension","0","1");
    //}
  }

  private ArrayList<Book> getFakeData(String baseFileName){
    ArrayList<Book> books = new ArrayList<>();
    for(int i = 0; i < 5; i++){
      Book book = new Book();
      book.bookName = "Test Copy " + Integer.toString(i);
      book.id = "Test ID " + Integer.toString(i);
      String fileName = baseFileName + Integer.toString(i);
      switch (i){
        case 0: fileName = fileName + ".zim"; break;
        case 1: fileName = fileName + ".part"; break;
        case 2: fileName = fileName + ".txt"; break;
        case 3: fileName = fileName + ".zim"; break;
        case 4: fileName = fileName + ".part"; break;
      }
      book.file = new File(fileName);
      books.add(book);
    }
    return books;
  }

  private void insertFakeData(){
    ArrayList<Book> books = new ArrayList<>();
    for(int i = 0; i<5 ; i++){
      Book book = new Book();
      book.id = "TestID_" + Integer.toString(i);
      book.creator = "TestCreator";
      book.bookName = "TestBook_" + Integer.toString(i);
      book.file = new File("/sd/emulated/0/kiwix/test/");
      books.add(book);
    }
    bookDao.saveBooks(books);
  }

  public void saveBookToDatabase(Book book){
    BookDatabaseEntity bookDatabaseEntity = new BookDatabaseEntity();
    bookDatabaseEntity.setBookId(book.getId());
    bookDatabaseEntity.setTitle(book.getTitle());
    bookDatabaseEntity.setDescription(book.getDescription());
    bookDatabaseEntity.setLanguage(book.getLanguage());
    bookDatabaseEntity.setBookCreator(book.getCreator());
    bookDatabaseEntity.setPublisher(book.getPublisher());
    bookDatabaseEntity.setDate(book.getDate());
    bookDatabaseEntity.setUrl(book.file.getPath());
    bookDatabaseEntity.setArticleCount(book.getArticleCount());
    bookDatabaseEntity.setMediaCount(book.getMediaCount());
    bookDatabaseEntity.setSize(book.getSize());
    bookDatabaseEntity.setFavicon(book.getFavicon());
    bookDatabaseEntity.setName(book.getName());
    bookDao.mDb.persist(bookDatabaseEntity);
  }
}
