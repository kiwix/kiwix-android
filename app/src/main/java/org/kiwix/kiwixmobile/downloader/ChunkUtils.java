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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.kiwix.kiwixmobile.utils.StorageUtils;

public class ChunkUtils {

  public static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz";
  public static final String ZIM_EXTENSION = ".zim";
  // Chuck Part
  private static final String CPART = ".cpart";
  // Total Part
  private static final String TPART = ".tpart";
  public static final long CHUNK_SIZE = 1024L * 1024L * 1024L * 2L;

  public static String baseNameFromParts(File file) {
    return file.getName().replace(CPART, "").replace(TPART, "")
        .replaceAll("\\.zim..", ".zim");
  }

  public static File completedChunk(String name) {
    return new File(name + TPART);
  }

  public static boolean isPresent(String name) {
    return new File(name).exists() || new File(name + TPART).exists()
        || new File(name + CPART + TPART).exists();
  }

  public static boolean hasParts(File file) {
    File[] files = file.getParentFile().listFiles((file1, s) ->
        s.startsWith(baseNameFromParts(file)) && s.endsWith(TPART));
    return files != null && files.length > 0;
  }

  public static String getFileName(String fileName) {
    if (isPresent(fileName)) {
      return fileName;
    } else {
      return fileName + "aa";
    }
  }

  public static File initialChunk(String name) {
    return new File(name + CPART + TPART);
  }

  public static void completeChunk(File chunk) {
     chunk.renameTo(new File(chunk.getPath().replace(CPART, "")));
  }

  public static void completeDownload(File file) {
    final String baseName = baseNameFromParts(file);
    File directory =file.getParentFile();
    File[] parts = directory.listFiles((file1, s) -> s.startsWith(baseName) && s.endsWith(TPART));
    for (File part : parts) {
      part.renameTo(new File(part.getPath().replace(TPART, "")));
    }
  }

  //public static long getCurrentSize(LibraryNetworkEntity.Book book) {
  //  long size = 0;
  //  File[] files = getAllZimParts(book.file);
  //  for (File file : files) {
  //    size += file.length();
  //  }
  //  return size;
  //}

  private static File[] getAllZimParts(File file) {
    final String baseName = baseNameFromParts(file);
    File directory = new File(file.getPath()).getParentFile();
    File[] parts = directory.listFiles((file1, s) -> s.matches(baseName + ".*"));
    return parts;
  }

  public static void deleteAllParts(File file) {
    final String baseName = baseNameFromParts(file);
    File directory = file.getParentFile();
    File[] parts = directory.listFiles((file1, s) -> s.matches(baseName + ".*"));
    for (File part : parts) {
      part.delete();
    }
  }

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
      return new String[] { fileName };
    }
    int position = fileName.lastIndexOf(".");
    String baseName = position > 0 ? fileName.substring(0, position) : fileName;
    String[] fileNames = new String[count];

    for (int i = 0; i < count; i++) {
      char first = ALPHABET.charAt(i / 26);
      char second = ALPHABET.charAt(i % 26);
      String chunkExtension = String.valueOf(first) + second;
      fileNames[i] = baseName + ZIM_EXTENSION + chunkExtension;
    }
    return fileNames;
  }
}
