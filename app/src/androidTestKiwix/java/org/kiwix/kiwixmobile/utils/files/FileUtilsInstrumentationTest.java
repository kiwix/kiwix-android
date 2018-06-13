package org.kiwix.kiwixmobile.utils.files;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.kiwix.kiwixmobile.data.local.KiwixDatabase;
import org.kiwix.kiwixmobile.data.local.dao.BookDao;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;

public class FileUtilsInstrumentationTest {

  private Context context;
  private boolean mockInitialized = false;
  private File testDir;

  @Before
  public void executeBefore() {
    if (!mockInitialized) {
      MockitoAnnotations.initMocks(this);
      mockInitialized = true;
    }
    context = InstrumentationRegistry.getTargetContext();
    testDir = context.getDir("testDir", context.MODE_PRIVATE);

    //File newTestFile2 = new File(newTestFile.getPath());
    //assertEquals("assert file exists", true, newTestFile2.exists());
  }

  @Test
  public void testGetAllZimParts() throws IOException{
    // set Constants
    String testId = "8ce5775a-10a9-bbf3-178a-9df69f23263c";

    //assertEquals("file path", "", testDir.getPath());
    //File testFile = new File(testDir.getPath() + "/newTestFile");
    //try {
    //  testFile.createNewFile();
    //} catch (IOException e) {
    //  e.printStackTrace();
    //}
    //assertEquals("verify that the file was created properly", true, testFile.exists());

    // Filename ends with .zimXX and the files up till "FileName.zimer" exist
    // i.e. 26 * 4 + 18 = 122 files exist
    String fileName = "/" + testId + "testfile.zim";

    // Creating the files for the test
    for (char char1 = 'a'; char1 <= 'z'; char1++) {
      for (char char2 = 'a'; char2 <= 'z'; char2++) {
        File file = new File(testDir.getPath() + fileName + char1 + char2);
        file.createNewFile();
        if(char1 == 'e' && char2 == 'r') {
          break;
        }
      }
      if(char1 == 'e') {
        break;
      }
    }

    // Test begins here
    Book book = new Book();
    book.file = new File(testDir.getPath() + fileName + "bg");
    assertEquals("verify that the file has been created properly", true, book.file.exists());
    List<File> files = FileUtils.getAllZimParts(book);

    // Checking all the values returned
    assertEquals("26 * 4 + 18 = 122 files should be returned", 122, files.size());


    // Deleting the Files


  }
}
