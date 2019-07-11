package org.kiwix.kiwixmobile.zim_manager.local_file_transfer;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static org.kiwix.kiwixmobile.zim_manager.local_file_transfer.FileItem.FileStatus.*;

/**
 * Helper class, part of the local file sharing module.
 *
 * Defines a file-item to represent the files being transferred.
 * */
public class FileItem {
  @Retention(RetentionPolicy.SOURCE)
  @IntDef({TO_BE_SENT, SENDING, SENT, ERROR})
  public @interface FileStatus {
    int TO_BE_SENT = -1;  // File yet to be sent
    int SENDING = 0;      // Being sent
    int SENT = +1;        // Successfully sent
    int ERROR = +2;       // Error encountered while transferring the file
  }

  private String fileName;
  private int fileStatus;

  public FileItem(String fileName, @FileStatus int fileStatus) {
    this.fileName = fileName;
    this.fileStatus = fileStatus;
  }

  public void setFileStatus(@FileStatus int fileStatus) {
    this.fileStatus = fileStatus;
  }

  public String getFileName() {
    return fileName;
  }

  @FileStatus
  public int getFileStatus() {
    return fileStatus;
  }
}
