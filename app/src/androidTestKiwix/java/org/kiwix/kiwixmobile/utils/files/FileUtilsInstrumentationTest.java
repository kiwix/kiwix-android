package org.kiwix.kiwixmobile.utils.files;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import org.junit.After;
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
  public void testGetAllZimParts() throws IOException {

    // Filename ends with .zimXX and the files up till "FileName.zimer" exist
    // i.e. 26 * 4 + 18 = 122 files exist
    String testId = "8ce5775a-10a9-bbf3-178a-9df69f23263s";
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
        fileNameWithExtension =
            bool[index] ? fileNameWithExtension : fileNameWithExtension + ".part";
        File file = new File(testDir.getPath() + fileNameWithExtension);
        file.createNewFile();
        if (char1 == 'e' && char2 == 'r') {
          break;
        }
        index++;
      }
      if (char1 == 'e') {
        break;
      }
    }
    assertEquals("siddhartj", 121, index);
    // Test begins here
    Book book = new Book();
    book.file = new File(testDir.getPath() + fileName + "bg");
    //assertEquals("verify that the file has been created properly", true, book.file.exists());
    List<File> files = FileUtils.getAllZimParts(book);

    // Checking all the values returned
    assertEquals("26 * 4 + 18 = 122 files should be returned", 122, files.size());

    for (index = 0; index < 122; index++) {
      if (bool[index]) {
        assertEquals(
            "if the file fileName.zimXX exists, then no need to add the .part extension at the end",
            false, files.get(index).getPath().endsWith(".part"));
      } else {
        //assertEquals("if the file fileName.zimXX.part exists, then the file returned should also have the same ending .zimXX.part",
        //    true, files.get(index).getPath().endsWith(".part"));
      }
    }

    // Delete all the Files just created
    index = 0;
    for (char char1 = 'a'; char1 <= 'z'; char1++) {
      for (char char2 = 'a'; char2 <= 'z'; char2++) {
        fileNameWithExtension = fileName + char1 + char2;
        fileNameWithExtension =
            bool[index] ? fileNameWithExtension : fileNameWithExtension + ".part";
        File file = new File(testDir.getPath() + fileNameWithExtension);
        file.delete();
        if (char1 == 'e' && char2 == 'r') {
          break;
        }
        index++;
      }
      if (char1 == 'e') {
        break;
      }
    }
  }

  @Test
  public void testHasPart() throws IOException {
    String testId = "8ce5775a-10a9-bbf3-178a-9df69f23263d";
    String baseName = "/" + testId + "testFile";

    // FileName ends with .zim
    File file1 = new File(testDir + baseName + "1" + ".zim");
    file1.createNewFile();
    assertEquals("if the fileName ends with .zim and exists in memory, return false",
        false, FileUtils.hasPart(file1));
    file1.delete();

    // FileName ends with .part
    File file2 = new File(testDir + baseName + "2" + ".zim");
    file2.createNewFile();
    assertEquals("if the fileName ends with .part and exists in memory, return true",
        false, FileUtils.hasPart(file2));
    file2.delete();

    // FileName ends with .zim, however, only the FileName.zim.part file exists in memory
    File file3 = new File(testDir + baseName + "3" + ".zim" + ".part");
    file3.createNewFile();
    File file4 = new File(testDir + baseName + "3" + ".zim");
    assertEquals("if the fileName ends with .zim, but instead the .zim.part file exists in memory",
        true, FileUtils.hasPart(file4));
    file3.delete();

    // FileName ends with .zimXX
    File testCall = new File(testDir + baseName + ".zimcj");
    testCall.createNewFile();

    // Case : FileName.zimXX.part does not exist for any value of "XX" from "aa" till "bl", but FileName.zimXX exists for all "XX" from "aa', till "bk", then it does not exist
    for (char char1 = 'a'; char1 <= 'z'; char1++) {
      for (char char2 = 'a'; char2 <= 'z'; char2++) {
        File file = new File(testDir.getPath() + baseName + ".zim" + char1 + char2);
        file.createNewFile();
        if (char1 == 'b' && char2 == 'k') {
          break;
        }
      }
      if (char1 == 'b') {
        break;
      }
    }

    //assertEquals("", false, FileUtils.hasPart(testCall));
    //
    //// Case : FileName.zim is the calling file, but neither FileName.zim, nor FileName.zim.part exist. In this case the answer will be the same as that in the previous (FileName.zimXX) case
    File testCall2 = new File(testDir.getPath() + baseName + ".zim");
    //assertEquals("", false, FileUtils.hasPart(testCall2));

    // Case : FileName.zimXX.part exists for some "XX" between "aa" till "bl", and FileName.zimXX exists for all "XX" from "aa', till "bk", then it does not exist
    File t = new File(testDir + baseName + ".zimaj.part");
    t.createNewFile();
    assertEquals("", true, FileUtils.hasPart(testCall));

    // Case : FileName.zim is the calling file, but neither FileName.zim, nor FileName.zim.part exist. In this case the answer will be the same as that in the previous (FileName.zimXX) case
    assertEquals("", true, FileUtils.hasPart(testCall2));

    // Delete all the Files created for the test
    for (char char1 = 'a'; char1 <= 'z'; char1++) {
      for (char char2 = 'a'; char2 <= 'z'; char2++) {
        File file = new File(testDir.getPath() + baseName + ".zim" + char1 + char2);
        file.delete();
        if (char1 == 'b' && char2 == 'k') {
          break;
        }
      }
      if (char1 == 'b') {
        break;
      }
    }
    t.delete();
    testCall.delete();
    testCall2.delete();
  }

  @After
  public void RemoveTestDirectory() {
    testDir.delete();
  }
}