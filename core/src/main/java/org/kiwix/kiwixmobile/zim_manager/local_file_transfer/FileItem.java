package org.kiwix.kiwixmobile.zim_manager.local_file_transfer;

import android.net.Uri;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static org.kiwix.kiwixmobile.zim_manager.local_file_transfer.FileItem.FileStatus.ERROR;
import static org.kiwix.kiwixmobile.zim_manager.local_file_transfer.FileItem.FileStatus.SENDING;
import static org.kiwix.kiwixmobile.zim_manager.local_file_transfer.FileItem.FileStatus.SENT;
import static org.kiwix.kiwixmobile.zim_manager.local_file_transfer.FileItem.FileStatus.TO_BE_SENT;

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
