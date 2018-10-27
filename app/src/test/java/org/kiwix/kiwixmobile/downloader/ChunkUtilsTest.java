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

package org.kiwix.kiwixmobile.downloader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import org.junit.Before;
import org.kiwix.kiwixmobile.utils.StorageUtils;
import org.powermock.core.classloader.annotations.PrepareForTest;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(StorageUtils.class)
public class ChunkUtilsTest {

  private final String URL = "http://mirror.netcologne.de/kiwix/zim/wikipedia/wikipedia_af_all_nopic_2016-05.zim";

  @Before
  public void executeBefore() {
    mockStatic(StorageUtils.class);
    when(StorageUtils.getFileNameFromUrl(URL)).thenReturn("TestFileName");
    when(StorageUtils.getFileNameFromUrl("TestURL")).thenReturn("TestFileName.xml");
  }

  @Test
  public void TestGetChunks() {
    List<Chunk> listReturned;
    long size;

    // When the file size is exactly equal to CHUNK_SIZE
    size = ChunkUtils.CHUNK_SIZE;
    listReturned = ChunkUtils.getChunks(URL, size, 27);

    assertEquals("verify that the list contains correct number of chunks", 1, listReturned.size());
    assertEquals("verify that the range format is correct", "0-", listReturned.get(0).getRangeHeader());
    assertEquals("verify that the same notificationID is passed to the chunk", 27, listReturned.get(0).getNotificationID());
    assertEquals("verify that the file name is correctly assigned in case of a single file", "TestFileName.part", listReturned.get(0).getFileName());
    assertEquals("verify that the same URL is passed on to the chunk", URL, listReturned.get(0).getUrl());

    // When the file size is more than CHUNK_SIZE
    size = (ChunkUtils.CHUNK_SIZE * (long) 5) + (long) (1024 * 1024);
    listReturned = ChunkUtils.getChunks(URL, size, 56);

    assertEquals("verify that the list contains correct number of chunks", 6, listReturned.size());
    assertEquals("verify that the rangehandler for the last chunk is correct", "10737418245-", listReturned.get(listReturned.size() - 1).getRangeHeader());
    assertEquals("verify that the rangehandler for the first chunk is corect", "0-2147483648", listReturned.get(0).getRangeHeader());

    assertTrue("verify that the same notificationID is passed on to each chunk", listReturned.get(0).getUrl().equals(URL)
            && listReturned.get(1).getUrl().equals(URL)
            && listReturned.get(2).getUrl().equals(URL)
            && listReturned.get(3).getUrl().equals(URL)
            && listReturned.get(4).getUrl().equals(URL)
            && listReturned.get(5).getUrl().equals(URL));

    assertTrue("verify that the same URL is passed on to each chunk", listReturned.get(0).getNotificationID() == 56
            && listReturned.get(1).getNotificationID() == 56
            && listReturned.get(2).getNotificationID() == 56
            && listReturned.get(3).getNotificationID() == 56
            && listReturned.get(4).getNotificationID() == 56
            && listReturned.get(5).getNotificationID() == 56);

    // test assignment of file names
    boolean test = true;
    String ALPHABET = "abcdefghijklmnopqrstuvwxyz";
    for (int i = 0; i < listReturned.size(); i++) {
      if (!listReturned.get(i)
          .getFileName()
          .substring(listReturned.get(i).getFileName().length() - 11)
          .equals(".zim" + ALPHABET.charAt(i / 26) + ALPHABET.charAt(i % 26) + ".part")) {
        test = false;
      }
    }
    assertTrue("verify that the file name endings are correctly assigned", test);

    // When the file size is less than CHUNK_SIZE
    size = ChunkUtils.CHUNK_SIZE - (long) (1024 * 1024);
    listReturned = ChunkUtils.getChunks(URL, size, 37);

    assertEquals("verify that the list contains correct number of chunks", 1, listReturned.size());
    assertEquals("verify that the range format is correct", "0-", listReturned.get(0).getRangeHeader());
    assertEquals("verify that the same notificationID is passed to the chunk", 37, listReturned.get(0).getNotificationID());
    assertEquals("verify that the file name is correctly assigned in case of a single file", "TestFileName.part", listReturned.get(0).getFileName());
    assertEquals("verify that the same URL is passed on to the chunk", URL, listReturned.get(0).getUrl());

    // verify that filename is correctly generated
    size = ChunkUtils.CHUNK_SIZE;
    listReturned = ChunkUtils.getChunks("TestURL", size, 0);
    assertEquals("verify that previous extension in the filename (if any) is removed in case of files having 1 chunk", "TestFileName.xml.part", listReturned.get(0).getFileName());

    size = ChunkUtils.CHUNK_SIZE * (long) 2;
    listReturned = ChunkUtils.getChunks("TestURL", size, 0);
    assertEquals("verify that previous extension in the filename (if any) is removed in case of files having more than 1 chunk", "TestFileName.zimaa.part", listReturned.get(0).getFileName());
    assertEquals("verify that previous extension in the filename (if any) is removed in case of files having more than 1 chunk", "TestFileName.zimab.part", listReturned.get(1).getFileName());
  }
}
