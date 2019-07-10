package org.kiwix.kiwixmobile.utils.files;

import android.content.Context;
import androidx.test.InstrumentationRegistry;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book;

import static org.junit.Assert.assertEquals;

public class FileUtilsInstrumentationTest {

  private Context context;
  private File testDir;

  @Before
  public void executeBefore() {
    context = InstrumentationRegistry.getTargetContext();

    // Create a temporary directory where all the test files will be saved
    testDir = context.getDir("testDir", Context.MODE_PRIVATE);
  }

  @Test
  public void testGetAllZimParts() throws IOException {

    // Filename ends with .zimXX and the files up till "FileName.zimer" exist
    // i.e. 26 * 4 + 18 = 122 files exist
    String testId = "2rs5475f-51h7-vbz6-331b-7rr25r58251s";
    String fileName = testDir.getPath() + "/" + testId + "testfile.zim";
    String fileNameWithExtension;
    Random r = new Random();
    boolean[] bool = new boolean[122];

    // Creating the files for the test
    int index = 0;
    for (char char1 = 'a'; char1 <= 'z'; char1++) {
      for (char char2 = 'a'; char2 <= 'z'; char2++) {
        bool[index] = r.nextBoolean();
        fileNameWithExtension = fileName + char1 + char2;
        fileNameWithExtension =
            bool[index] ? fileNameWithExtension : fileNameWithExtension + ".part";
        File file = new File(fileNameWithExtension);
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

    Book book = new Book();
    book.file = new File(fileName + "bg");

    List<File> files = FileUtils.getAllZimParts(book);

    // Testing the data returned
    assertEquals("26 * 4 + 18 = 122 files should be returned", 122, files.size());

    for (index = 0; index < 122; index++) {
      if (bool[index]) {
        assertEquals(
            "if the file fileName.zimXX exists, then no need to add the .part extension at the end",
            false, files.get(index).getPath().endsWith(".part"));
      } else {
        assertEquals(
            "if the file fileName.zimXX.part exists, then the file returned should also have the same ending .zimXX.part",
            true, files.get(index).getPath().endsWith(".part"));
      }
    }
  }

  @Test
  public void testHasPart() throws IOException {
    String testId = "3yd5474g-55d1-aqw0-108z-1xp69x25260d";
    String baseName = testDir.getPath() + "/" + testId + "testFile";

    // FileName ends with .zim
    File file1 = new File(baseName + "1" + ".zim");
    file1.createNewFile();
    assertEquals("if the fileName ends with .zim and exists in memory, return false",
        false, FileUtils.hasPart(file1));

    // FileName ends with .part
    File file2 = new File(baseName + "2" + ".zim");
    file2.createNewFile();
    assertEquals("if the fileName ends with .part and exists in memory, return true",
        false, FileUtils.hasPart(file2));

    // FileName ends with .zim, however, only the FileName.zim.part file exists in memory
    File file3 = new File(baseName + "3" + ".zim" + ".part");
    file3.createNewFile();
    File file4 = new File(baseName + "3" + ".zim");
    assertEquals("if the fileName ends with .zim, but instead the .zim.part file exists in memory",
        true, FileUtils.hasPart(file4));

    // FileName ends with .zimXX
    File testCall = new File(baseName + ".zimcj");
    testCall.createNewFile();

    // Case : FileName.zimXX.part does not exist for any value of "XX" from "aa" till "bl", but FileName.zimXX exists for all "XX" from "aa', till "bk", then it does not exist
    for (char char1 = 'a'; char1 <= 'z'; char1++) {
      for (char char2 = 'a'; char2 <= 'z'; char2++) {
        File file = new File(baseName + ".zim" + char1 + char2);
        file.createNewFile();
        if (char1 == 'b' && char2 == 'k') {
          break;
        }
      }
      if (char1 == 'b') {
        break;
      }
    }

    assertEquals(false, FileUtils.hasPart(testCall));

    // Case : FileName.zim is the calling file, but neither FileName.zim, nor FileName.zim.part exist
    // In this case the answer will be the same as that in the previous (FileName.zimXX) case
    File testCall2 = new File(baseName + ".zim");
    assertEquals(false, FileUtils.hasPart(testCall2));

    // Case : FileName.zimXX.part exists for some "XX" between "aa" till "bl"
    // And FileName.zimXX exists for all "XX" from "aa', till "bk", and then it does not exist
    File t = new File(baseName + ".zimaj.part");
    t.createNewFile();
    assertEquals(true, FileUtils.hasPart(testCall));

    // Case : FileName.zim is the calling file, but neither FileName.zim, nor FileName.zim.part exist
    // In this case the answer will be the same as that in the previous (FileName.zimXX) case
    assertEquals(true, FileUtils.hasPart(testCall2));
  }

  @After
  public void RemoveTestDirectory() {
    for (File child : testDir.listFiles()) {
      child.delete();
    }
    testDir.delete();
  }
}
