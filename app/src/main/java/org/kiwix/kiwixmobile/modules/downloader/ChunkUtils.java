package org.kiwix.kiwixmobile.modules.downloader;

import org.kiwix.kiwixmobile.common.utils.StorageUtils;

import java.util.ArrayList;
import java.util.List;

public class ChunkUtils {

  public static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz";
  public static final String ZIM_EXTENSION = ".zim";
  public static final String PART = ".part";
  public static final long CHUNK_SIZE = 1024L * 1024L * 1024L * 2L;

  public static List<Chunk> getChunks(String url, long contentLength, int notificationID) {
    int fileCount = getZimChunkFileCount(contentLength);
    String filename = StorageUtils.getFileNameFromUrl(url);
    String[] fileNames = getZimChunkFileNames(filename, fileCount);
    return generateChunks(contentLength, url, fileNames, notificationID);
  }

  private static List<Chunk> generateChunks(long contentLength, String url, String[] fileNames, int notificationID) {
    List<Chunk> chunks = new ArrayList<>();
    long currentRange = 0;
    for (String zim : fileNames) {
      String range;
      if (currentRange + CHUNK_SIZE >= contentLength) {
        range = String.format("%d-", currentRange);
        chunks.add(new Chunk(range, zim, url, contentLength, notificationID, currentRange, contentLength));
        currentRange += CHUNK_SIZE + 1;
      } else {
        range = String.format("%d-%d", currentRange, currentRange + CHUNK_SIZE);
        chunks.add(new Chunk(range, zim, url, contentLength, notificationID, currentRange, currentRange + CHUNK_SIZE));
        currentRange += CHUNK_SIZE + 1;
      }
    }
    return chunks;
  }

  private static int getZimChunkFileCount(long contentLength) {
    int fits = (int) (contentLength / CHUNK_SIZE);
    boolean hasRemainder = contentLength % CHUNK_SIZE > 0;
    if (hasRemainder) return fits + 1;
    return fits;
  }

  private static String[] getZimChunkFileNames(String fileName, int count) {
    if (count == 1) {
      return new String[] { fileName + PART};
    }
    int position = fileName.lastIndexOf(".");
    String baseName = position > 0 ? fileName.substring(0, position) : fileName;
    String[] fileNames = new String[count];

    for (int i = 0; i < count; i++) {
      char first = ALPHABET.charAt(i / 26);
      char second = ALPHABET.charAt(i % 26);
      String chunkExtension = String.valueOf(first) + second;
      fileNames[i] = baseName + ZIM_EXTENSION + chunkExtension + PART;
    }
    return fileNames;
  }
}
