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
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;

public class FileUtilsInstrumentationTest {

  private Context context;
  private boolean mockInitialized = false;

  @Before
  public void executeBefore() {
    if (!mockInitialized) {
      MockitoAnnotations.initMocks(this);
      mockInitialized = true;
    }
    context = InstrumentationRegistry.getTargetContext();
    File testDir = context.getDir("testDir", context.MODE_PRIVATE);
    //assertEquals("file path", "", testDir.getPath());
    File newTestFile = new File(testDir.getPath() + "/newTestFile");
    try {
      newTestFile.createNewFile();
    } catch (IOException e) {
      e.printStackTrace();
    }
    assertEquals("assert file exists", true, newTestFile.exists());

    File newTestFile2 = new File(newTestFile.getPath());
    assertEquals("assert file exists", true, newTestFile2.exists());
  }

  @Test
  public void testGetAllZimParts(){
    // set Constants
    String testId = "8ce5775a-10a9-bbf3-178a-9df69f23263c";
    String fileName = "/data/user/0/org.kiwix.kiwixmobile/files" + File.separator + testId;
    List<File> files;

  }
}
