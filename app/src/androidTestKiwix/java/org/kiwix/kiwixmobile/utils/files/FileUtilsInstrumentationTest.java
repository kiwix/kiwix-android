package org.kiwix.kiwixmobile.utils.files;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import org.junit.Before;
import org.junit.Test;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;

public class FileUtilsInstrumentationTest {

  private Context context;
  private File testDir;

  @Before
  public void executeBefore() {
    context = InstrumentationRegistry.getTargetContext();
    testDir = context.getDir("testDir", context.MODE_PRIVATE);
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
    String fileNameWithExtension;
    Random r = new Random();
    boolean bool[] = new boolean[122];

    // Creating the files for the test
    int index = 0;
    for (char char1 = 'a'; char1 <= 'z'; char1++) {
      for (char char2 = 'a'; char2 <= 'z'; char2++) {
        bool[index] = r.nextBoolean();
        fileNameWithExtension = fileName + char1 + char2;
        fileNameWithExtension = bool[index] ? fileNameWithExtension : fileNameWithExtension + ".part";
        File file = new File(testDir.getPath() + fileNameWithExtension);
        file.createNewFile();
        if(char1 == 'e' && char2 == 'r') {
          break;
        }
        index++;
      }
      if(char1 == 'e') {
        break;
      }
    }
    assertEquals("siddhartj", 121, index);
    // Test begins here
    Book book = new Book();
    book.file = new File(testDir.getPath() + fileName + "bg");
    assertEquals("verify that the file has been created properly", true, book.file.exists());
    List<File> files = FileUtils.getAllZimParts(book);

    // Checking all the values returned
    assertEquals("26 * 4 + 18 = 122 files should be returned", 122, files.size());

    for(index = 0; index < 122; index++ ) {
      if(bool[index]) {
        assertEquals("if the file fileName.zimXX exists, then no need to add the .part extension at the end", false, files.get(index).getPath().endsWith(".part"));
      } else {
        //assertEquals("if the file fileName.zimXX.part exists, then the file returned should also have the same ending .zimXX.part", true, files.get(index).getPath().endsWith(".part"));
      }
    }

    // Deleting the Files


  }
}
