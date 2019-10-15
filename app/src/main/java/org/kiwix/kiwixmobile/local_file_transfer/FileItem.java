/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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
 *
 */

package org.kiwix.kiwixmobile.local_file_transfer;

import android.net.Uri;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static org.kiwix.kiwixmobile.local_file_transfer.FileItem.FileStatus.ERROR;
import static org.kiwix.kiwixmobile.local_file_transfer.FileItem.FileStatus.SENDING;
import static org.kiwix.kiwixmobile.local_file_transfer.FileItem.FileStatus.SENT;
import static org.kiwix.kiwixmobile.local_file_transfer.FileItem.FileStatus.TO_BE_SENT;

/**
 * Helper class, part of the local file sharing module.
 *
 * Defines a file-item to represent the files being transferred.
 */
public class FileItem {

  @Retention(RetentionPolicy.SOURCE)
  @IntDef({ TO_BE_SENT, SENDING, SENT, ERROR })
  public @interface FileStatus {
    int TO_BE_SENT = -1;  // File yet to be sent
    int SENDING = 0;      // Being sent
    int SENT = +1;        // Successfully sent
    int ERROR = +2;       // Error encountered while transferring the file
  }

  private Uri fileUri;
  private String fileName;
  private int fileStatus;

  public FileItem(@NonNull Uri fileUri) { // For sender devices
    this(fileUri, WifiDirectManager.getFileName(fileUri));
  }

  public FileItem(@NonNull String fileName) { // For receiver devices
    this(null, fileName);
  }

  private FileItem(Uri fileUri, String fileName) {
    this.fileUri = fileUri;
    this.fileName = fileName;
    this.fileStatus = TO_BE_SENT;
  }

  // Helper methods
  public void setFileStatus(@FileStatus int fileStatus) {
    this.fileStatus = fileStatus;
  }

  public @NonNull Uri getFileUri() {
    return fileUri;
  }

  public @NonNull String getFileName() {
    return fileName;
  }

  public @FileStatus int getFileStatus() {
    return fileStatus;
  }
}
