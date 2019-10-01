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
package org.kiwix.kiwixmobile.core.downloader;

public class Chunk {

  public boolean isDownloaded = false;
  private String rangeHeader;
  private String fileName;
  private String url;
  private long contentLength;
  private int notificationID;
  private long startByte;
  private long endByte;

  public Chunk(String rangeHeader, String fileName, String url, long contentLength,
    int notificationID, long startByte, long endByte) {
    this.rangeHeader = rangeHeader;
    this.fileName = fileName;
    this.url = url;
    this.contentLength = contentLength;
    this.startByte = startByte;
    this.endByte = endByte;
    this.notificationID = notificationID;
  }

  public String getRangeHeader() {
    return rangeHeader;
  }

  public String getFileName() {
    return fileName;
  }

  public long getContentLength() {
    return contentLength;
  }

  public String getUrl() {
    return url;
  }

  public int getNotificationID() {
    return notificationID;
  }

  public long getStartByte() {
    return startByte;
  }

  public long getEndByte() {
    return endByte;
  }

  public long getSize() {
    return 1 + endByte - startByte;
  }
}
